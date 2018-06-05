package com.fibremint.blockchain.message;

import com.fibremint.blockchain.blockchain.Block;
import com.fibremint.blockchain.blockchain.Blockchain;
import com.fibremint.blockchain.blockchain.Transaction;
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
    private Blockchain blockchain;
    private HashMap<ServerInfo, Date> serverStatus;
    private String remoteIP;
    private int localPort;
    private Gson gson;

    public MessageHandlerRunnable(Socket clientSocket, Blockchain blockchain, HashMap<ServerInfo, Date> serverStatus, int localPort) {
        this.clientSocket = clientSocket;
        this.blockchain = blockchain;
        this.serverStatus = serverStatus;
        this.localPort = localPort;

        RuntimeTypeAdapterFactory<MessageBase> messageAdapterFactory = RuntimeTypeAdapterFactory
                .of(MessageBase.class, "Type")
                .registerSubtype(MessageCatchUp.class, "catchUp")
                .registerSubtype(MessageHeartbeat.class, "heartbeat")
                .registerSubtype(MessageLastBlock.class, "lastBlock")
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
                    case lastBlock:
                        if (this.latestBlockHandler(gson.fromJson(jsonElement, MessageLastBlock.class))) {
                            break;
                        }
                    case transaction:
        				this.transactionHandler(gson.fromJson(jsonElement, MessageTransaction.class), outWriter);
        				break;
                    /*case printBlock:
        				this.printBlockHandler(outWriter);
        				break;*/
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
				outStream.writeObject(blockchain.getLastBlock());
				outStream.flush();
			} else {
				Block currentBlock = blockchain.getLastBlock();
				while (true) {
					if (currentBlock.getHeader().hash.equals(message.getBlockHash())) {
					    outStream.writeObject(currentBlock);
					    outStream.flush();
					    return;
                    }
					if (currentBlock == null) {
						break;
					}
					//currentBlock = currentBlock.getPreviousBlock();
					currentBlock = blockchain.getBlock(currentBlock.getHeader().previousHash);
				}
				outStream.writeObject(currentBlock);
				outStream.flush();
			
			}
		} catch (Exception e) {
		    e.printStackTrace();
		}
	}

	// TODO: rename related things: last -> latest
    private boolean latestBlockHandler(MessageLastBlock message) {
    	try {
    		String blockHash;
    		// TODO: check getLashBlock
    		if (blockchain.getLastBlock() != null) {
    			/*byte[] latestHash = blockchain.getLastBlock().calculateHash();
    			blockHash = Base64.getEncoder().encodeToString(latestHash);*/
    			blockHash =  blockchain.getLastBlock().getHeader().calculateHash();

    		} else {
    			blockHash = "null";
    		}
    		
    		if (blockHash.equals(message.getLatestHash())
                    && this.blockchain.getLength() > message.blockchainLength
                    || this.blockchain.getLength() == message.blockchainLength
                    && message.latestHash.length() < blockHash.length()) {
    		//no catchup necessary
    			return true;
    			
    		} else {
    		//catchup case
    			//set up new connection
    			String remoteIP = (((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getAddress()).toString().replace("/", "");
    			Socket s;
    			s = new Socket(remoteIP, message.getLocalPort());
    			PrintWriter outWriter;
    			outWriter = new PrintWriter(s.getOutputStream(), true);
    			
    			//naive catchup
    			ArrayList<Block> catchUpBlocks = new ArrayList<Block>();
    			//getting head
    			outWriter.println(gson.toJson(new MessageCatchUp()));
                outWriter.flush();
				ObjectInputStream inputStream;
				inputStream = new ObjectInputStream(s.getInputStream());
				// TODO: check catchUpBlock contains header
    			Block catchUpBlock = (Block) inputStream.readObject();
    			
    			inputStream.close();
    			s.close();
    			catchUpBlocks.add(catchUpBlock);
                String prevHash = catchUpBlock.getHeader().previousHash;

                // TODO: refactor genesis block hash
    			while (!prevHash.startsWith("A")) {
    				s = new Socket(remoteIP, message.getLocalPort());
    				outWriter = new PrintWriter(s.getOutputStream(), true);

    				outWriter.println(gson.toJson(new MessageCatchUp(prevHash)));
                    outWriter.flush();
    				
    				inputStream = new ObjectInputStream(s.getInputStream());

    				catchUpBlock = (Block) inputStream.readObject();
    				inputStream.close();
    				s.close();
    				catchUpBlocks.add(catchUpBlock);
                    prevHash = catchUpBlock.getHeader().previousHash;
    			}

    			this.blockchain.catchUp(catchUpBlocks);

    			return false;
    		}
    	
    	} catch (Exception e) {
    	}
    	return false;
    }

    public void transactionHandler(MessageTransaction message, PrintWriter outWriter) {
        Transaction transaction = new Transaction(message.g)
        try {
    		if (this.blockchain.getBlock().addTransaction(transaction))
                outWriter.print(gson.toJson(new MessageResult(MessageResult.Type.accepted)));
    		else
                outWriter.print(gson.toJson(new MessageResult(MessageResult.Type.denied)));

            outWriter.flush();
		} catch (Exception e) {
    		e.printStackTrace();
		}
	}

/*	public void printBlockHandler(PrintWriter outWriter) {
    	try {
    		outWriter.print(blockchain.toString() + "\n");
    		System.out.println(blockchain.toString() + "\n");
    		outWriter.flush();
		} catch (Exception e) {
    		e.printStackTrace();
		}
	}*/

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

