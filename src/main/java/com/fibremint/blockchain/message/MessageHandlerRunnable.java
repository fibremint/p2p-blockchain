package com.fibremint.blockchain.message;

import com.fibremint.blockchain.blockchain.Block;
import com.fibremint.blockchain.blockchain.Blockchain;
import com.fibremint.blockchain.blockchain.Transaction;
import com.fibremint.blockchain.blockchain.Wallet;
import com.fibremint.blockchain.message.model.*;
import com.fibremint.blockchain.net.ServerInfo;
import com.fibremint.blockchain.util.HashUtil;
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
        		// TODO: move catchUp, heartBeat, latestBlock, serverInQuestion to network related message handler
        		messageType = jsonElement.getAsJsonObject().get("type").getAsString();
        		switch (MessageType.valueOf(messageType)) {
                    case catchUp:

                        catchUpHandler(gson.fromJson(jsonElement, MessageCatchUp.class));
                        break;
                    case heartbeat:
                        this.heartbeatHandler(gson.fromJson(jsonElement, MessageHeartbeat.class));
                        break;
                    case latestBlock:/*
                        if (this.latestBlockHandler(gson.fromJson(jsonElement, MessageLatestBlock.class))) {
                            break;
                        }*/
                        this.latestBlockHandler(gson.fromJson(jsonElement, MessageLatestBlock.class));
                        break;
                    case transaction:
        				this.transactionHandler(gson.fromJson(jsonElement, MessageTransaction.class), outWriter);
        				break;
                    case serverInQuestion:
                        this.serverInQuestionHandler(gson.fromJson(jsonElement, MessageServerInQuestion.class));
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

	private void catchUpHandler(MessageCatchUp message) {
		try (ObjectOutputStream outStream = new ObjectOutputStream(clientSocket.getOutputStream())){
			if (!message.hasBlockHash()) {
				outStream.writeObject(Blockchain.getLatestBlock());
				outStream.flush();
			} else {
				Block currentBlock = Blockchain.getLatestBlock();
				while (true) {
                    if (currentBlock == null) {
                        break;
                    }
					if (currentBlock.header.hash.equals(message.getBlockHash())) {
					    outStream.writeObject(currentBlock);
					    outStream.flush();
					    return;
                    }

					//currentBlock = currentBlock.getPreviousBlock();
					currentBlock = Blockchain.getBlock(currentBlock.header.previousHash);
				}
				outStream.writeObject(currentBlock);
				outStream.flush();
			
			}
		} catch (Exception e) {
		    e.printStackTrace();
		}
	}

	// TODO: rename related things: last -> latest
    private void latestBlockHandler(MessageLatestBlock message) {
    	try {
    		String blockHash;
    		// TODO: check getLatestBlock
    		if (Blockchain.getLatestBlock() != null) {
    			blockHash = Blockchain.getLatestBlock().header.calculateHash();

    		} else {
    			blockHash = "";
    		}
    		
    		/*if (blockHash.equals(message.getLatestHash())
                    && Blockchain.getLength() > message.blockchainLength
                    || Blockchain.getLength() == message.blockchainLength
                    && message.latestHash.length() < blockHash.length()) {
    		//no catchup necessary
    			return true;
    			
    		} */
            if (blockHash.equals(message.getLatestHash())
                    && Blockchain.getLength() >= message.blockchainLength) {
                //no catchup necessary
                return;

            } else {
    		//catchup case
    			//set up new connection
    			String remoteIP = (((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getAddress()).toString().replace("/", "");
    			Socket socket;
    			socket = new Socket(remoteIP, message.getLocalPort());
    			PrintWriter outWriter;
    			outWriter = new PrintWriter(socket.getOutputStream(), true);
    			
    			//naive catchup
    			ArrayList<Block> catchUpBlocks = new ArrayList<Block>();
    			//getting head
    			outWriter.println(gson.toJson(new MessageCatchUp()));
                outWriter.flush();

				ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
    			Block remoteLatestBlock = (Block) inputStream.readObject();
    			
    			inputStream.close();
    			socket.close();

    			catchUpBlocks.add(remoteLatestBlock);
                String prevHash = remoteLatestBlock.header.previousHash;

                // TODO: refactor genesis block hash
    			/*while (!prevHash.startsWith("A")) {*/
                /*while (!prevHash.equals("genesis")) {*/
                while (!blockHash.equals(prevHash) && !prevHash.equals("genesis")) {
    				socket = new Socket(remoteIP, message.getLocalPort());
    				outWriter = new PrintWriter(socket.getOutputStream(), true);

    				outWriter.println(gson.toJson(new MessageCatchUp(prevHash)));
                    outWriter.flush();
    				
    				inputStream = new ObjectInputStream(socket.getInputStream());

    				remoteLatestBlock = (Block) inputStream.readObject();
    				inputStream.close();
    				socket.close();
    				catchUpBlocks.add(remoteLatestBlock);
                    prevHash = remoteLatestBlock.header.previousHash;
    			}

    			Blockchain.catchUp(catchUpBlocks);

    		}
    	
    	} catch (Exception e) {
    	}
    }

    public void transactionHandler(MessageTransaction message, PrintWriter outWriter) {
        Wallet wallet = new Wallet(
                HashUtil.generatePrivateKey(message.senderPrivateKey),
                HashUtil.generatePublicKey(message.senderPublicKey));
        Transaction transaction = wallet.sendFunds(HashUtil.generatePublicKey(message.recipient), message.value);

        try {
    		if (Blockchain.getLatestBlock().addTransaction(transaction))
                outWriter.print(gson.toJson(new MessageResult(MessageResult.Type.accepted)));
    		else
                outWriter.print(gson.toJson(new MessageResult(MessageResult.Type.denied)));

            outWriter.flush();
		} catch (Exception e) {
    		e.printStackTrace();
		}
	}

    public void heartbeatHandler(MessageHeartbeat message) {
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

    public void serverInQuestionHandler(MessageServerInQuestion message) {
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
    
    public void removeUnresponsive() {
    	//check for servers that havent responded in 4 secs
        for (ServerInfo server: serverStatus.keySet()) {
            if (new Date().getTime() - serverStatus.get(server).getTime() > REMOTE_SERVER_TIMEOUT) {
            	serverStatus.remove(server);
            	System.out.println("removed " + server.getHost());
            }
        }
    }
    
    public void broadcastHeartbeat(String message, ArrayList<ServerInfo> exempt) {
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

