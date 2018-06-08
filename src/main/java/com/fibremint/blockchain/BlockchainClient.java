package com.fibremint.blockchain;

import com.fibremint.blockchain.blockchain.*;
import com.fibremint.blockchain.message.MessageSenderRunnable;
import com.fibremint.blockchain.message.model.*;
import com.fibremint.blockchain.net.ServerInfo;
import com.fibremint.blockchain.util.HashUtil;
import com.fibremint.blockchain.util.RuntimeTypeAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.bouncycastle.asn1.eac.ECDSAPublicKey;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.Security;
import java.util.concurrent.Executors;

public class BlockchainClient {
    private ServerInfo serverInfo;
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedReader inputReader;
    private PrintWriter outWriter;
    private MessageProperties messageProperties;
    private Gson gson;

    public class Mining implements Runnable {
        Wallet wallet;
        Wallet coinProvider;
        public Mining(Wallet wallet) {
            this.coinProvider = new Wallet();
            this.wallet = wallet;
        }


        @Override
        public void run() {
            outWriter.println(gson.toJson(new MessageProperties()));
            outWriter.flush();

            String retrieve = null;
            try {
                while(retrieve == null) {
                    retrieve = inputReader.readLine();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            messageProperties = gson.fromJson(new JsonParser().parse(retrieve), MessageProperties.class);
            //while(messageProperties != null) break;

            Transaction genesisTransaction = new Transaction(coinProvider.publicKey, wallet.publicKey,
                    messageProperties.miningReward, null);
            genesisTransaction.generateSignature(coinProvider.privateKey);
            genesisTransaction.hash = "0";
            genesisTransaction.outputs.add(new TransactionOutput(
                    genesisTransaction.recipient,
                    genesisTransaction.value,
                    genesisTransaction.hash
            ));

            Block miningBlock = new Block(new BlockHeader(messageProperties.blockHash));
            miningBlock.addTransaction(genesisTransaction);

            miningBlock.header.merkleRootHash = HashUtil.getMerkleRoot(miningBlock.transactions);
            String target = HashUtil.getDifficultyString(messageProperties.difficulty);
            while(!miningBlock.header.hash.substring(0, messageProperties.difficulty).equals(target)) {
                miningBlock.header.nonce++;
                miningBlock.header.hash = miningBlock.header.calculateHash();
            }

            System.out.println("Block mined");

            outWriter.println(gson.toJson(new MessageMineBlock()));
            outWriter.flush();
            try (ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream())){
                outStream.writeObject(miningBlock);
                outStream.flush();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public class IncomingReader implements Runnable {

        @Override
        public void run() {
            JsonParser jsonParser = new JsonParser();
            JsonElement jsonElement;
            String messageType;
            try {
                while(true) {
                    String inputLine = inputReader.readLine();

                    if (inputLine == null) {
                        break;
                    }

                    jsonElement = jsonParser.parse(inputLine);
                    messageType = jsonElement.getAsJsonObject().get("type").getAsString();
                    switch (MessageType.valueOf(messageType)) {
                        case properties:
                            messageProperties = gson.fromJson(jsonElement, MessageProperties.class);
                            break;
                        default:
                    }
                }
            }  catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void start(String host, int port) {
        RuntimeTypeAdapterFactory<MessageBase> messageAdapterFactory = RuntimeTypeAdapterFactory
                .of(MessageBase.class, "Type")
                .registerSubtype(MessageProperties.class, "properties");

        gson = new GsonBuilder().registerTypeAdapterFactory(messageAdapterFactory).create();

        serverInfo = new ServerInfo(host, port);
        try {
            socket = new Socket(serverInfo.getHost(), serverInfo.getPort());
            inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outWriter = new PrintWriter(socket.getOutputStream());

        } catch (Exception e) {
            e.printStackTrace();
        }

        Thread incoming =  new Thread(new IncomingReader());
        //incoming.start();

        Wallet wallet = new Wallet();

        Thread mining = new Thread(new Mining(wallet));
        mining.start();

    }
    public static void main(String[] args) {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        if (args.length != 2) {
            return;
        }

        int remotePort = 0;
        String remoteHost = null;

        try {
            remoteHost = args[0];
            remotePort = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            return;
        }

        new BlockchainClient().start(remoteHost, remotePort);

        /*
        try {
            ServerInfo serv = new ServerInfo(remoteHost, remotePort);
            *//*Scanner sc = new Scanner(System.in);
            while (true) {
                String message = sc.nextLine();
                new Thread(new MessageSenderRunnable(serv, message)).start();
            }*//*
            Wallet wallet = new Wallet();
            Thread incomingThread = new Thread(new IncomingReader())
            //Thread miner = new Thread(new BlockMiner(wallet, serv));
            //miner.start();

        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }


/*
        Transaction genesisTransaction = new Transaction(coinbase.publicKey, 100f);
        genesisTransaction.generateSignature(coinbase.privateKey);
        genesisTransaction.hash = "0";
        genesisTransaction.outputs.add(new TransactionOutput(
                genesisTransaction.recipient,
                genesisTransaction.value,
                genesisTransaction.hash));
        Blockchain.UTXOs.put(genesisTransaction.outputs.get(0).hash, genesisTransaction.outputs.get(0));
        Block genesis = new Block(new BlockHeader("0"));
        genesis.mineBlock(Blockchain.difficulty);
        Blockchain.blockchain.add(genesis);
*/

}
