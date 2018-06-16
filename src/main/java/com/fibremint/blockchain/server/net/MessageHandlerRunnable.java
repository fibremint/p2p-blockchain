package com.fibremint.blockchain.server.net;

import com.fibremint.blockchain.server.blockchain.*;
import com.fibremint.blockchain.server.net.message.*;
import com.fibremint.blockchain.server.util.HashUtil;
import com.fibremint.blockchain.server.util.RuntimeTypeAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.security.PublicKey;
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

       /* RuntimeTypeAdapterFactory<MessageBase> messageAdapterFactory = RuntimeTypeAdapterFactory
                .of(MessageBase.class, "Type")
                .registerSubtype(MessageMineBlock.class, "mineBlock")
                .registerSubtype(MessageCatchUp.class, "catchUp")
                .registerSubtype(MessageHeartbeat.class, "heartbeat")
                .registerSubtype(MessageLatestBlock.class, "latestBlock")
                .registerSubtype(MessageProperties.class, "properties")
                .registerSubtype(MessageTransaction.class, "transaction")
                .registerSubtype(MessageServerInQuestion.class, "serverInQuestion")
                .registerSubtype(MessageResult.class, "result");

        gson = new GsonBuilder().registerTypeAdapterFactory(messageAdapterFactory).create();*/
       gson = new GsonBuilder().create();
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
        		if (inputLine == null) break;

                jsonElement = jsonParser.parse(inputLine);
        		messageType = jsonElement.getAsJsonObject().get("type").getAsString();
        		switch (MessageType.valueOf(messageType)) {
                    case mineBlock:
                        mineBlockHandler(gson.fromJson(jsonElement, MessageMineBlock.class), outWriter);
                        break;
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
        				transactionHandler(gson.fromJson(jsonElement, MessageTransaction.class), outWriter);
        				break;
                    case serverInQuestion:
                        serverInQuestionHandler(gson.fromJson(jsonElement, MessageServerInQuestion.class));
                        break;
                    case walletBalance:
                        walletBalanceHandler(gson.fromJson(jsonElement, MessageWalletBalance.class), outWriter);
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


    private void walletBalanceHandler(MessageWalletBalance message, PrintWriter outWriter) {
        float total = 0f;
        PublicKey publicKey = HashUtil.generatePublicKey(HashUtil.getDecodedKey(message.publicKey));
        HashMap<String, TransactionOutput> walletUTXOs = new HashMap<>();
        for(Map.Entry<String, TransactionOutput> item : Blockchain.UTXOs.entrySet()) {
            TransactionOutput UTXO = item.getValue();
            if (UTXO.isMine(publicKey)) {
                total += UTXO.value;
                walletUTXOs.put(UTXO.hash, UTXO);
            }
        }

        outWriter.println(gson.toJson(new MessageWalletBalance(HashUtil.getEncodedKey(publicKey), total, walletUTXOs)));
        outWriter.flush();
    }

    private void mineBlockHandler(MessageMineBlock message, PrintWriter outWriter) {
        try {
            Block mineBlock = message.block;
            byte[] minerPublicKeyBinary = HashUtil.getDecodedKey(message.miner);
            PublicKey minerPublicKey = HashUtil.generatePublicKey(minerPublicKeyBinary);

            Wallet coinProvider = new Wallet();
            Transaction genesisTransaction = new Transaction(
                    coinProvider.publicKey, minerPublicKey,
                    Blockchain.miningReward, null);
            genesisTransaction.generateSignature(coinProvider.privateKey);
            genesisTransaction.hash = "0";
            genesisTransaction.outputs.add(new TransactionOutput(
                    genesisTransaction.recipient,
                    genesisTransaction.value,
                    genesisTransaction.hash
            ));
            mineBlock.addTransaction(genesisTransaction);

            ArrayList<Block> testChain = new ArrayList<>(Blockchain.blockchain);
            testChain.add(mineBlock);
            if (Blockchain.isChainValid(testChain)) {
                Blockchain.blockchain.add(mineBlock);
                Blockchain.UTXOs.put(mineBlock.transactions.get(0).outputs.get(0).hash,
                        mineBlock.transactions.get(0).outputs.get(0));
                outWriter.println(gson.toJson(new MessageResult(MessageResult.Type.accepted, MessageType.mineBlock)));
            } else {
                outWriter.println(gson.toJson(new MessageResult(MessageResult.Type.denied, MessageType.mineBlock,
                        "Process transaction error")));
            }

        } catch (Exception e) {
            e.printStackTrace();
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
                    || localTransactionLength >= message.transactionLength) {
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
       try {
           String blockHash = "0";
           Block block = Blockchain.getLatestBlock();

           if (block != null) blockHash = block.header.hash;

           outWriter.println(gson.toJson(new MessageProperties(blockHash,
                   Blockchain.difficulty, Blockchain.minimumTransaction, Blockchain.miningReward)));
           outWriter.flush();
           //outWriter.close();
       } catch (Exception e) {
           e.printStackTrace();
       }
    }

    // TODO: Implement validate transaction.
    private void transactionHandler(MessageTransaction message, PrintWriter outWriter) {
        if (Blockchain.getLatestBlock() != null) {
            try {
                Transaction transaction = new Transaction(message.hash, message.sender,
                        message.recipient, message.value, message.signature, message.inputs);
                Blockchain.getLatestBlock().addTransaction(transaction);

                outWriter.println(gson.toJson(new MessageResult(MessageResult.Type.accepted, MessageType.transaction)));
/*                ArrayList<Block> testChain = new ArrayList<>(Blockchain.blockchain);

                Block latestBlock = testChain.get(testChain.size() - 1);
                latestBlock.addTransaction(transaction);
                if (Blockchain.isChainValid(testChain)) {
                    for (TransactionInput input : message.inputs)
                        Blockchain.UTXOs.remove(input.transactionOutputHash);
                    Blockchain.getLatestBlock().addTransaction(transaction);

                    outWriter.println(gson.toJson(new MessageResult(MessageResult.Type.accepted, MessageType.transaction)));
                } else {
                    outWriter.println(gson.toJson(new MessageResult(MessageResult.Type.denied, MessageType.transaction,
                            "Transaction error")));
                }*/

                outWriter.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Not available mined blocks");
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

