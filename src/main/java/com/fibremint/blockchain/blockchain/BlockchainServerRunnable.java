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

    private Socket clientSocket;
    private Blockchain blockchain;
    private HashMap<ServerInfo, Date> serverStatus;
    String remoteIP;
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
                .registerSubtype(MessageLastBlock.class, "lastBlock")
                .registerSubtype(MessageTransaction.class, "transaction")
                .registerSubtype(MessageServerInQuestion.class, "serverInQuestion")
                .registerSubtype(MessageResult.class, "result");

        gson = new GsonBuilder().registerTypeAdapterFactory(messageAdapterFactory).create();
    }

/*    private enum HandlerType {
        transaction(value -> ),
        printBlock,
        heartbeat,
        lastBlock,
        catchUp;

        private Function<String, Void> expression;

        HandlerType(Function<String, Void> expression) {
            this.expression = expression;
        }

        public
    }*/

    public void run() {
        try {
            BufferedReader inputReader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
        	PrintWriter outWriter = new PrintWriter(clientSocket.getOutputStream(), true);

            JsonParser jsonParser = new JsonParser();
            JsonElement jsonElement;
            String messageType;
        	//String innerJson = null;

        	while (true) {
        		String inputLine = inputReader.readLine();

        		if (inputLine == null) {
                    break;

                }

        		//messages = gson.fromJson(inputLine, Messages.class);
                jsonElement = jsonParser.parse(inputLine);


        		/*for (Messages.Container container : messages) {
        		    innerJson = gson.toJson(container.content);

        		    switch (container.Type) {
                        case transaction:
                            this.transactionHandler(inputLine, outWriter);
                            break;
                        case printBlock:
                            this.printBlockHandler(outWriter);
                            break;
                        case heartbeat:
                        case serverInQuestion:
                            this.heartBeatHandler(inputReader, inputLine, innerJson);
                        case lastBlock:
                            if (this.lastBlockHandler(inputLine, innerJson)) break;
                        case catchUp:
                            this.catchUpHandler(innerJson);
                            break;

                            default:

                    }
                }*/
        		messageType = jsonElement.getAsJsonObject().get("Type").getAsString();
        		//String[] tokens = inputLine.split("\\|");
        		switch (MessageType.valueOf(messageType)) {
                    case catchUp:
                        /*if (tokens[0].equals("cu")) {
                            this.catchUpHandler(tokens);
                            break;
                        }*/
                        catchUpHandler(gson.fromJson(jsonElement, MessageCatchUp.class));
                    case heartbeat:
                    case lastBlock:
                        if (this.lastBlockHandler(gson.fromJson(jsonElement, MessageLastBlock.class))) {
                            break;
                        }
                    case transaction:
        				this.transactionHandler(gson.fromJson(jsonElement, MessageTransaction.class), outWriter);
                    case printBlock:
        				this.printBlockHandler(outWriter);
        			//case "cc":
        				//this.serverHandler(inputLine, outWriter, tokens);
                    case serverInQuestion:
        				this.heartBeatHandler(inputReader, inputLine, tokens);
        			default:
                       	outWriter.print("Error\n\n");
                       	outWriter.flush();
        		}
        	}
            clientSocket.close();
        } catch (IOException e) {
        }
    }

//Request handlers//-----------------------   

	public void catchUpHandler(MessageCatchUp message) {
		try (ObjectOutputStream outStream = new ObjectOutputStream(clientSocket.getOutputStream())){
			if (!message.hasBlockchain()) {
			//cu-only case
				outStream.writeObject(blockchain.getHead());
				outStream.flush();
			} else {
			//cu|<block's hash> case
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
		}
	}

	// last block
    public boolean lastBlockHandler(MessageLastBlock message) {
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

    /*
    public void serverHandler(String inputLine, PrintWriter outWriter, String[] tokens) {
        try {
            switch (tokens[0]) {
                case "tx":
                    if (this.blockchain.addTransaction(inputLine))
                        outWriter.print("Accepted\n\n");
                    else
                        outWriter.print("Rejected\n\n");
                        outWriter.flush();
                        break;
                case "pb":
                    outWriter.print(blockchain.toString() + "\n");
                    System.out.println(blockchain.toString() + "\n");
                    outWriter.flush();
                    break;
                case "cc":
                    return;
                    
0            }
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }
    */

    // transaction
    public void transactionHandler(MessageTransaction message, PrintWriter outWriter) {
        MessageTransaction transaction;
        try {
    		if (this.blockchain.addTransaction(message)) {
                //outWriter.print("Accepted\n\n");
                outWriter.print(gson.toJson(new MessageResult(MessageResult.Type.accepted)));
                //transaction = new MessageTransaction(MessageEnum.MessageType.ACCEPTED)
    		} else {
    			//outWriter.print("Rejected\n\n");
                outWriter.print(gson.toJson(new MessageResult(MessageResult.Type.denied)));
			}

            outWriter.flush();
		} catch (Exception e) {
    		e.printStackTrace();
		}
	}

	// print block
	public void printBlockHandler(PrintWriter outWriter) {
    	try {
    		outWriter.print(blockchain.toString() + "\n");
    		System.out.println(blockchain.toString() + "\n");
    		outWriter.flush();
		} catch (Exception e) {
    		e.printStackTrace();
		}
	}

    public void heartBeatHandler(BufferedReader bufferedReader, String line, String[] tokens) {
        try {	
            String remoteIP = (((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getAddress()).toString().replace("/", "");
            	
            ServerInfo serverInQuestion;
            switch (tokens[0]) {
                case "hb":
                	serverInQuestion = new ServerInfo(remoteIP, Integer.valueOf(tokens[1]));
                		
                	if (!serverStatus.containsKey(serverInQuestion)) {
                		String forwardMessage = "si|" + String.valueOf(localPort) + "|" + remoteIP + "|" + tokens[1];
                    	this.broadcastHeartbeat(forwardMessage, new ArrayList<ServerInfo>());
                	}
                		
                	serverStatus.put(serverInQuestion, new Date());
                	this.removeUnresponsive();
            			
                case "si":
                	serverInQuestion = new ServerInfo(tokens[2], Integer.valueOf(tokens[3]));
                	ServerInfo originator = new ServerInfo(remoteIP, Integer.valueOf(tokens[1]));
                		
                	if (!serverStatus.containsKey(serverInQuestion)) {
                    	ArrayList<ServerInfo> exempt = new ArrayList<ServerInfo>();
                    	exempt.add(originator);
                    	exempt.add(serverInQuestion);
                    	String relayMessage = "si|" + String.valueOf(localPort) + "|" + tokens[2] + "|" + tokens[3];
                    	this.broadcastHeartbeat(relayMessage, exempt);
                    		
                	}
                		
                	serverStatus.put(serverInQuestion, new Date());
                	serverStatus.put(originator, new Date());
                	this.removeUnresponsive();
                    	
                default:     
            }
        } catch (Exception e) {
    	}
    }

//Helper Functions//--------------------------
    
    public void removeUnresponsive() {
    	//check for servers that havent responded in 4 secs
        for (ServerInfo server: serverStatus.keySet()) {
            if (new Date().getTime() - serverStatus.get(server).getTime() > 4000) {
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

