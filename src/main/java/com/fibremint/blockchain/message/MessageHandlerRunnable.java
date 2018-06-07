package com.fibremint.blockchain.message;

import com.fibremint.blockchain.blockchain.*;
import com.fibremint.blockchain.message.model.*;
import com.fibremint.blockchain.net.ServerInfo;
import com.fibremint.blockchain.util.RuntimeTypeAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.*;

public class MessageHandlerRunnable implements Runnable{
    private static final int REMOTE_SERVER_TIMEOUT = 4000;

    private Socket clientSocket;
    private HashMap<ServerInfo, Date> serverStatus;
    private String remoteIP;
    private int localPort;
    private Gson gson;

    public MessageHandlerRunnable(Socket clientSocket, HashMap<ServerInfo, Date> serverStatus, int localPort) {
        this.clientSocket = clientSocket;
        this.serverStatus = serverStatus;
        this.localPort = localPort;

        RuntimeTypeAdapterFactory<MessageBase> messageAdapterFactory = RuntimeTypeAdapterFactory
                .of(MessageBase.class, "Type")
                .registerSubtype(MessageCatchUp.class, "catchUp")
                .registerSubtype(MessageHeartbeat.class, "heartbeat")
                .registerSubtype(MessageLatestBlock.class, "latestBlock")
                .registerSubtype(MessageProperties.class, "properties")
                .registerSubtype(MessageTransaction.class, "transaction")
                .registerSubtype(MessageServerInQuestion.class, "serverInQuestion")
                .registerSubtype(MessageResult.class, "result");

        gson = new GsonBuilder().registerTypeAdapterFactory(messageAdapterFactory).create();
    }

    public void run() {
        try {
            BufferedReader inputReader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
        	PrintWriter outWriter = new PrintWriter(clientSocket.getOutputStream(), true);

            JsonParser jsonParser = new JsonParser();
            JsonElement jsonElement;
            String messageType;

        	while (true) {
        		String inputLine = inputReader.readLine();

        		if (inputLine == null) {
                    break;
                }

                jsonElement = jsonParser.parse(inputLine);
        		messageType = jsonElement.getAsJsonObject().get("type").getAsString();
        		switch (MessageType.valueOf(messageType)) {
                    case catchUp:
                        catchUpHandler(gson.fromJson(jsonElement, MessageCatchUp.class));
                        break;
                    case heartbeat:
                        heartbeatHandler(gson.fromJson(jsonElement, MessageHeartbeat.class));
                        break;
                    case latestBlock:
                        latestBlockHandler(gson.fromJson(jsonElement, MessageLatestBlock.class));
                        break;
                    case properties:
                        propertiesHandler(outWriter);
                        break;
                    case transaction:
        				transactionHandler(outWriter);
        				break;
                    case serverInQuestion:
                        serverInQuestionHandler(gson.fromJson(jsonElement, MessageServerInQuestion.class));
                        break;
        			default:
                       	outWriter.print(gson.toJson(new MessageResult(MessageResult.Type.error)));
                       	outWriter.flush();
        		}
        	}
            clientSocket.close();
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

	private synchronized void catchUpHandler(MessageCatchUp message) {
		try (ObjectOutputStream outStream = new ObjectOutputStream(clientSocket.getOutputStream())){
            List<Block> catchUpBlocks = new ArrayList<>();

            for(int i = message.blockIndex; i < Blockchain.getLength(); i++)
                catchUpBlocks.add(Blockchain.blockchain.get(i));

            outStream.writeObject(catchUpBlocks);
            outStream.flush();

            outStream.writeObject(Blockchain.UTXOs);
            outStream.flush();

		} catch (Exception e) {
		    e.printStackTrace();
		}
	}

    private synchronized void latestBlockHandler(MessageLatestBlock message) {
        String localLatestBlockHash;
        Block localLatestBlock = null;
        try {
    		if (Blockchain.getLatestBlock() != null) {
    		    localLatestBlock = Blockchain.getLatestBlock();
                localLatestBlockHash = localLatestBlock.header.calculateHash();
            } else
    			localLatestBlockHash = "0";

    		int localTransactionLength = 0;
    		if (localLatestBlock != null) localTransactionLength = localLatestBlock.transactions.size();
            if (localLatestBlockHash.equals(message.getLatestHash())
                    || Blockchain.getLength() >= message.blockchainLength
                    && localTransactionLength >= message.transactionLength) {
                              //no catchup necessary
                return;

            } else {
    		//catchup case
    			//set up new connection
    			String remoteIP = (((InetSocketAddress) clientSocket.getRemoteSocketAddress())
                        .getAddress()).toString().replace("/", "");
    			Socket socket;
    			socket = new Socket(remoteIP, message.getLocalPort());
    			PrintWriter outWriter;
    			outWriter = new PrintWriter(socket.getOutputStream(), true);
    			
    			//naive catchup
    			int catchUpBlockIndex = 0;
                if (!localLatestBlockHash.equals("0"))
                    catchUpBlockIndex = Blockchain.blockchain.indexOf(Blockchain.getBlock(localLatestBlockHash));
    			outWriter.println(gson.toJson(new MessageCatchUp(catchUpBlockIndex)));
                outWriter.flush();

                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());

                List<Block> catchUpBlocks = (List<Block>) inputStream.readObject();
                HashMap<String, TransactionOutput> catchUpUXTOs = (HashMap) inputStream.readObject();

                inputStream.close();
    			socket.close();

    			Blockchain.catchUp(catchUpBlocks, catchUpUXTOs);

    		}
    	
    	} catch (Exception e) {
    	}
    }

    private void propertiesHandler(PrintWriter outWriter) {
        String blockHash = "0";
        Block block = Blockchain.getLatestBlock();

        if (block != null) blockHash = block.header.hash;
        outWriter.print(gson.toJson(new MessageProperties(blockHash,
                Blockchain.difficulty, Blockchain.minimumTransaction, Blockchain.miningReward)));
    }

    private void transactionHandler(PrintWriter outWriter) {
        try {
            ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
            Transaction transaction = (Transaction) inputStream.readObject();

            if (Blockchain.getLatestBlock().addTransaction(transaction))
                outWriter.print(gson.toJson(new MessageResult(MessageResult.Type.accepted)));
    		else
                outWriter.print(gson.toJson(new MessageResult(MessageResult.Type.denied)));

            outWriter.flush();
		} catch (Exception e) {
    		e.printStackTrace();
		}
	}

    private void heartbeatHandler(MessageHeartbeat message) {
        ServerInfo serverInQuestion;

        try {	
            String remoteIP = (((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getAddress()).toString()
                    .replace("/", "");
            serverInQuestion = new ServerInfo(remoteIP, message.getLocalPort());

            if (!serverStatus.containsKey(serverInQuestion)) {
                MessageServerInQuestion forwardMessage = new MessageServerInQuestion(
                        localPort, remoteIP, message.getLocalPort());
                this.broadcastHeartbeat(gson.toJson(forwardMessage), new ArrayList<>());
            }

            serverStatus.put(serverInQuestion, new Date());
            this.removeUnresponsive();

        } catch (Exception e) {
            e.printStackTrace();
    	}
    }

    private void serverInQuestionHandler(MessageServerInQuestion message) {
        try {
            ServerInfo serverInQuestion;
            serverInQuestion = new ServerInfo(message.getRemoteHost(), message.getRemotePort());
            ServerInfo originator = new ServerInfo(remoteIP, message.getLocalPort());

            if (!serverStatus.containsKey(serverInQuestion)) {
                ArrayList<ServerInfo> exempt = new ArrayList<>();
                exempt.add(originator);
                exempt.add(serverInQuestion);
                MessageServerInQuestion replyMessage = new MessageServerInQuestion(
                        localPort, message.getRemoteHost(), message.getRemotePort());
                this.broadcastHeartbeat(gson.toJson(replyMessage), exempt);

            }

            serverStatus.put(serverInQuestion, new Date());
            serverStatus.put(originator, new Date());
            this.removeUnresponsive();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//Helper Functions//--------------------------
    
    private void removeUnresponsive() {
    	//check for servers that havent responded in 4 secs
        for (ServerInfo server: serverStatus.keySet()) {
            if (new Date().getTime() - serverStatus.get(server).getTime() > REMOTE_SERVER_TIMEOUT) {
            	serverStatus.remove(server);
            	System.out.println("removed " + server.getHost());
            }
        }
    }
    
    private void broadcastHeartbeat(String message, ArrayList<ServerInfo> exempt) {
    	ArrayList<Thread> threadArrayList = new ArrayList<Thread>();
    	for (ServerInfo info: this.serverStatus.keySet()) {
            if (!exempt.contains(info)) {
                Thread thread = new Thread(new MessageSenderRunnable(info, message));
                thread.start();
                threadArrayList.add(thread);
            }
        }
        
        for (int i = 0; i < threadArrayList.size(); i++) {
            try {
            	threadArrayList.get(i).join();
            } catch (InterruptedException e) {
            }
        }
    }
}

