package com.fibremint.blockchain;

import com.fibremint.blockchain.blockchain.*;
import com.fibremint.blockchain.message.MessageHandlerRunnable;
import com.fibremint.blockchain.net.HeartBeatPeriodicRunnable;
import com.fibremint.blockchain.net.CatchupPeriodicRunnable;
import com.fibremint.blockchain.net.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Security;
import java.util.Date;
import java.util.HashMap;

public class BlockchainServer {

    public static void main(String[] args) {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        Logger logger = LoggerFactory.getLogger(BlockchainServer.class);
        if (args.length != 3) {
            return;
        }

        int localPort = 0;
        int remotePort = 0;
        String remoteHost = null;

        try {
            localPort = Integer.parseInt(args[0]);
            remoteHost = args[1];
            remotePort = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        logger.info("Block chain server started");

        Wallet walletA = new Wallet();
        Wallet coinbase = new Wallet();
        Transaction genesisTransaction = new Transaction(coinbase.publicKey, walletA.publicKey, 100f, null);
        genesisTransaction.generateSignature(coinbase.privateKey);
        genesisTransaction.hash = "genesis";
        genesisTransaction.outputs.add(new TransactionOutput(
                genesisTransaction.recipient,
                genesisTransaction.value,
                genesisTransaction.hash));

        Block genesis = new Block(new BlockHeader("genesis"));
        genesis.mineBlock(Blockchain.difficulty);
        Blockchain.blockchain.add(genesis);



        HashMap<ServerInfo, Date> remoteServerStatus = new HashMap<>();
        remoteServerStatus.put(new ServerInfo(remoteHost, remotePort), new Date());

        /*CommitPeriodicRunnable pcr = new CommitPeriodicRunnable(blockchain);
        Thread pct = new Thread(pcr);
        pct.start();*/
        
        //periodically send heartbeats
        new Thread(new HeartBeatPeriodicRunnable(remoteServerStatus, localPort)).start();
        
        //periodically catchup
        new Thread(new CatchupPeriodicRunnable(remoteServerStatus, localPort)).start();
        
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(localPort);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new MessageHandlerRunnable(clientSocket, remoteServerStatus, localPort)).start();

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
/*                pcr.setRunning(false);
                pct.join();*/
                if (serverSocket != null)
                    serverSocket.close();
            } catch (IOException e) {
            } /*catch (InterruptedException e) {
            }*/
        }
    }
}
