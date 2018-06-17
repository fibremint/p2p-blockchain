package com.fibremint.blockchain.server.net;

import com.fibremint.blockchain.server.BlockchainServer;
import com.fibremint.blockchain.server.RootClassAccessibleAbstract;
import com.fibremint.blockchain.server.blockchain.*;
import com.fibremint.blockchain.server.net.message.*;
import com.fibremint.blockchain.server.util.HashUtil;
import com.fibremint.blockchain.server.util.SignatureUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.*;

public class MessageHandlerRunnable extends RootClassAccessibleAbstract implements Runnable {
    private static final int REMOTE_SERVER_TIMEOUT = 4000;

    private Socket clientSocket;
    private HashMap<ServerInfo, Date> serverStatus;
    private int localPort;
    private String remoteIP;

    private Blockchain blockchain;

    private Gson gson;
    private JsonParser jsonParser;


    public MessageHandlerRunnable(BlockchainServer blockchainServer) {
        super(blockchainServer);
        this.clientSocket = blockchainServer.getClientSocket();
        this.serverStatus = blockchainServer.getRemoteServerStatus();
        this.localPort = blockchainServer.getLocalPort();

        this.blockchain = blockchainServer.getBlockchain();

       gson = new GsonBuilder().create();
       jsonParser = new JsonParser();
    }

    public void run() {
        try {
            BufferedReader inputReader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
        	PrintWriter outWriter = new PrintWriter(clientSocket.getOutputStream(), true);

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
        PublicKey publicKey = SignatureUtil.generatePublicKey(HashUtil.getDecoded(message.publicKey));
        HashMap<String, TransactionOutput> walletUTXOs = new HashMap<>();
        for(Map.Entry<String, TransactionOutput> item : blockchain.UTXOs.entrySet()) {
            TransactionOutput UTXO = item.getValue();
            if (UTXO.isMine(publicKey)) {
                total += UTXO.value;
                if (!message.isBalanceOnly)
                    walletUTXOs.put(UTXO.hash, UTXO);

            }
        }

        if (!message.isBalanceOnly) {
            outWriter.println(gson.toJson(new MessageWalletBalance(HashUtil.getEncodedKey(publicKey),
                    message.isBalanceOnly, total, walletUTXOs)));
        } else {
            outWriter.println(gson.toJson(new MessageWalletBalance(HashUtil.getEncodedKey(publicKey),
                    message.isBalanceOnly, total)));
        }

        outWriter.flush();
    }

    private void mineBlockHandler(MessageMineBlock message, PrintWriter outWriter) {
        try {
            Block mineBlock = message.block;
            Transaction miningTransaction = new Transaction(message.miner, blockchain.getLatestHash());

            if (blockchain.addBlock(mineBlock) && blockchain.addTransaction(miningTransaction)) {
                blockchain.UTXOs.put(mineBlock.transactions.get(0).outputs.get(0).hash,
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
/*		try (ObjectOutputStream outStream = new ObjectOutputStream(clientSocket.getOutputStream())){
            List<Block> catchUpBlocks = new ArrayList<>();

            for(int i = message.blockIndex; i < blockchain.getLength(); i++)
                catchUpBlocks.add(blockchain.blockchain.get(i));

*//*            outStream.writeObject(catchUpBlocks);
            outStream.flush();

            outStream.writeObject(blockchain.UTXOs);
            outStream.flush();*//*

		} catch (Exception e) {
		    e.printStackTrace();
		}*/
        try (PrintWriter outWriter = new PrintWriter(new PrintWriter(clientSocket.getOutputStream()))) {
            List<Block> catchUpBlocks = new ArrayList<>();
            for (int i = message.blockIndex; i < blockchain.getLength(); i++)
                catchUpBlocks.add(blockchain.blockchain.get(i));

            HashMap<String, MessageTransactionOutput> catchUpUTXOs = new HashMap<>();
            for (Map.Entry<String, TransactionOutput> item : blockchain.UTXOs.entrySet()) {
                TransactionOutput UTXO = item.getValue();
                catchUpUTXOs.put(UTXO.hash, new MessageTransactionOutput(UTXO));
            }

            String jsonMessage = gson.toJson(new MessageCatchUp(message.blockIndex, catchUpBlocks, catchUpUTXOs));
            outWriter.println(jsonMessage);
            outWriter.flush();
            outWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

	}

    private synchronized void latestBlockHandler(MessageLatestBlock messageLatestBlock) {
        String localLatestBlockHash;
        Block localLatestBlock = null;
        try {
    		if (blockchain.getLatestBlock() != null) {
    		    localLatestBlock = blockchain.getLatestBlock();
                localLatestBlockHash = localLatestBlock.header.calculateHash();
            } else
    			localLatestBlockHash = "0";

    		int localTransactionLength = blockchain.getLength();
    		if (localLatestBlock != null) localTransactionLength = localLatestBlock.transactions.size();

    		boolean logic1 = localLatestBlockHash.equals(messageLatestBlock.getLatestHash());
    		boolean logic2 = blockchain.getLength() >= messageLatestBlock.blockchainLength;
    		boolean logic3 = localTransactionLength >= messageLatestBlock.transactionLength;
            /*if (localLatestBlockHash.equals(messageLatestBlock.getLatestHash())
                    && (blockchain.getLength() >= messageLatestBlock.blockchainLength
                    && localTransactionLength >= messageLatestBlock.transactionLength)) {
                              //no catchup necessary
                return;*/

            if ((logic1) && (logic2 && logic3)) {
                //no catchup necessary
                return;

            } else {
    		//catchup case
    			//set up new connection
    			String remoteIP = (((InetSocketAddress) clientSocket.getRemoteSocketAddress())
                        .getAddress()).toString().replace("/", "");
    			Socket socket;
    			socket = new Socket(remoteIP, messageLatestBlock.getLocalPort());
    			PrintWriter outWriter;
    			outWriter = new PrintWriter(socket.getOutputStream(), true);
    			
    			//naive catchup
    			int catchUpBlockIndex = 0;
                if (!localLatestBlockHash.equals("0"))
                    catchUpBlockIndex = blockchain.blockchain.indexOf(blockchain.getBlock(localLatestBlockHash));
    			outWriter.println(gson.toJson(new MessageCatchUp(catchUpBlockIndex)));
                outWriter.flush();

  /*              ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());

*//*                List<Block> catchUpBlocks = (List<Block>) inputStream.readObject();
                HashMap<String, TransactionOutput> catchUpUTXOs = (HashMap) inputStream.readObject();

                inputStream.close();*/
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                JsonElement messageJson = jsonParser.parse(inputReader.readLine());
                MessageCatchUp messageCatchUp = null;
                try {
                    messageCatchUp = gson.fromJson(messageJson, MessageCatchUp.class);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
                ArrayList<Block> catchUpBlocks = new ArrayList<>();
                for(MessageBlock messageBlock : messageCatchUp.catchUpBlocks) {
                    catchUpBlocks.add(new Block(messageBlock));
                }

                HashMap<String, TransactionOutput> catchUpUTXOs = new HashMap<>();
                for(Map.Entry<String, MessageTransactionOutput> item : messageCatchUp.UTXOs.entrySet()) {
                    MessageTransactionOutput UTXO = item.getValue();
                    catchUpUTXOs.put(UTXO.hash, new TransactionOutput(UTXO));
                }

                inputReader.close();
    			socket.close();

    			blockchain.catchUp(catchUpBlocks, catchUpUTXOs);
    		}
    	
    	} catch (Exception e) {
    	}
    }

    private void propertiesHandler(PrintWriter outWriter) {
       try {
           String blockHash = "0";
           Block block = blockchain.getLatestBlock();

           if (block != null) blockHash = block.header.hash;

           outWriter.println(gson.toJson(new MessageProperties(blockHash,
                   Blockchain.difficulty, Blockchain.minimumTransaction, Blockchain.miningReward)));
           outWriter.flush();
       } catch (Exception e) {
           e.printStackTrace();
       }
    }

    // TODO: Implement validate transaction.
    private void transactionHandler(MessageTransaction message, PrintWriter outWriter) {
        if (blockchain.getLatestBlock() != null) {
            Transaction transaction = new Transaction(message);

           if (blockchain.addTransaction(transaction)) {
               for (MessageTransactionInput messageTransactionInput : message.inputs)
                   blockchain.UTXOs.remove(messageTransactionInput.transactionOutputHash);
               outWriter.println(gson.toJson(new MessageResult(MessageResult.Type.accepted, MessageType.transaction)));
           } else {
                outWriter.println(gson.toJson(new MessageResult(MessageResult.Type.error, MessageType.transaction,
                        "Transaction failed.")));
            }
        } else {
            outWriter.println(gson.toJson(new MessageResult(MessageResult.Type.denied, MessageType.transaction,
                    "Not available block.")));
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

    // TODO: check remoteIP is needed.
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

