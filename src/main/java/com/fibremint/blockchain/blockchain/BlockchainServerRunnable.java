package com.fibremint.blockchain.blockchain;

import com.fibremint.blockchain.message.MessageSenderRunnable;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;

public class BlockchainServerRunnable implements Runnable{
    private static final int REMOTE_SERVER_TIMEOUT = 4000;

    private Socket clientSocket;
    private Blockchain blockchain;
    private HashMap<ServerInfo, Date> serverStatus;
    private String remoteIP;
    private int localPort;
    private Gson gson;

    public BlockchainServerRunnable(Socket clientSocket, Blockchain blockchain, HashMap<ServerInfo, Date> serverStatus, int localPort) {
        this.clientSocket = clientSocket;
        this.blockchain = blockchain;
        this.serverStatus = serverStatus;
        this.localPort = localPort;

        // TODO: register MessagePrintBlock
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

        		messageType = jsonElement.getAsJsonObject().get("Type").getAsString();
        		switch (MessageType.valueOf(messageType)) {
                    case catchUp:
                        catchUpHandler(gson.fromJson(jsonElement, MessageCatchUp.class));
                        break;
                    case heartbeat:
                        this.heartBeatHandler(gson.fromJson(jsonElement, MessageHeartbeat.class));
                        break;
                    case lastBlock:
                        if (this.lastBlockHandler(gson.fromJson(jsonElement, MessageLastBlock.class))) {
                            break;
                        }
                    case transaction:
        				this.transactionHandler(gson.fromJson(jsonElement, MessageTransaction.class), outWriter);
        				break;
                    case printBlock:
        				this.printBlockHandler(outWriter);
        				break;
                    case serverInQuestion:
                        this.serverInQuestionHandler(gson.fromJson(jsonElement, MessageServerInQuestion.class));
                        break;
        			default:
                       	outWriter.print("Error\n\n");
                       	outWriter.flush();
        		}
        	}
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	private void catchUpHandler(MessageCatchUp message) {
		try (ObjectOutputStream outStream = new ObjectOutputStream(clientSocket.getOutputStream())){
			if (!message.hasBlockchain()) {
				outStream.writeObject(blockchain.getHead());
				outStream.flush();
			} else {
				Block currentBlock = blockchain.getHead();
				while (true) {
					if (Base64.getEncoder().encodeToString(currentBlock.calculateHash())
                            .equals(message.getBlockHash())) {
						outStream.writeObject(currentBlock);
						outStream.flush();
						return;

					}
					if (currentBlock == null) {
						break;
					}
					currentBlock = currentBlock.getPreviousBlock();
					
				}
				outStream.writeObject(currentBlock);
				outStream.flush();
			
			}
		} catch (Exception e) {
		    e.printStackTrace();
		}
	}

    private boolean lastBlockHandler(MessageLastBlock message) {
    	try {
    		String encodedHash;
    		if (blockchain.getHead() != null) {
    			byte[] latestHash = blockchain.getHead().calculateHash();
    			encodedHash = Base64.getEncoder().encodeToString(latestHash);
    			
    		} else {
    			encodedHash = "null";
    		}
    		
    		if (encodedHash.equals(message.getLatestHash())
                    && this.blockchain.getLength() > message.getBlockchainLength()
                    || this.blockchain.getLength() == message.getBlockchainLength()
                    && message.getLatestHash().length() < encodedHash.length()) {
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
    			ArrayList<Block> blocks = new ArrayList<Block>();
    			//outWriter.println("cu"); //getting head
    			outWriter.println(gson.toJson(new MessageCatchUp()));
                outWriter.flush();
				ObjectInputStream inputStream;
				inputStream = new ObjectInputStream(s.getInputStream());
    			Block b = (Block) inputStream.readObject();
    			
    			inputStream.close();
    			s.close();
    			blocks.add(b);
    			String prevHash = Base64.getEncoder().encodeToString(b.getPreviousHash());

    			while (!prevHash.startsWith("A")) {
    				s = new Socket(remoteIP, message.getLocalPort());
    				outWriter = new PrintWriter(s.getOutputStream(), true);

    				//outWriter.println("cu|" + prevHash);
    				outWriter.println(gson.toJson(new MessageCatchUp(prevHash)));
                    outWriter.flush();
    				
    				inputStream = new ObjectInputStream(s.getInputStream());

    				b = (Block) inputStream.readObject();
    				inputStream.close();
    				s.close();
    				blocks.add(b);
    				prevHash = Base64.getEncoder().encodeToString(b.getPreviousHash());
    			}
    			this.blockchain.setHead(blocks.get(0));
    			this.blockchain.setLength(blocks.size());
    			
    			Block cur = this.blockchain.getHead();
    			
    			for (int i = 0; i < blocks.size(); i++) {
    				if (i <= blocks.size() - 1) {
    					cur.setPreviousBlock(blocks.get(i + 1));
    				} else {
    					cur.setPreviousBlock(null);
    				}
    				cur = cur.getPreviousBlock();
    			}			
    			
    			return false;
    		}
    	
    	} catch (Exception e) {
    	}
    	return false;
    }

    public void transactionHandler(MessageTransaction message, PrintWriter outWriter) {
        MessageTransaction transaction;
        try {
    		if (this.blockchain.addTransaction(message))
                outWriter.print(gson.toJson(new MessageResult(MessageResult.Type.accepted)));
    		else
                outWriter.print(gson.toJson(new MessageResult(MessageResult.Type.denied)));

            outWriter.flush();
		} catch (Exception e) {
    		e.printStackTrace();
		}
	}

	public void printBlockHandler(PrintWriter outWriter) {
    	try {
    		outWriter.print(blockchain.toString() + "\n");
    		System.out.println(blockchain.toString() + "\n");
    		outWriter.flush();
		} catch (Exception e) {
    		e.printStackTrace();
		}
	}

    public void heartBeatHandler(MessageHeartbeat message) {
        ServerInfo serverInQuestion;

        try {	
            String remoteIP = (((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getAddress()).toString().replace("/", "");
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

