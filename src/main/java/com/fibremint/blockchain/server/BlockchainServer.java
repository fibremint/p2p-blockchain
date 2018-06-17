package com.fibremint.blockchain.server;

import com.fibremint.blockchain.server.blockchain.Blockchain;
import com.fibremint.blockchain.server.net.MessageHandlerRunnable;
import com.fibremint.blockchain.server.net.HeartBeatPeriodicRunnable;
import com.fibremint.blockchain.server.net.CatchupPeriodicRunnable;
import com.fibremint.blockchain.server.net.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Security;
import java.util.Date;
import java.util.HashMap;

public class BlockchainServer {
    private int localPort;
    private String remoteHost;
    private int remotePort;

    private HashMap<ServerInfo, Date> remoteServerStatus = new HashMap<>();
    private ServerSocket serverSocket;
    private Socket clientSocket;

    private Blockchain blockchain;

    public BlockchainServer(int localPort, String remoteHost, int remotePort) {
        this.localPort = localPort;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.blockchain = new Blockchain();
    }

    public Blockchain getBlockchain() {
        return blockchain;
    }

    public HashMap<ServerInfo, Date> getRemoteServerStatus() {
        return remoteServerStatus;
    }

    public int getLocalPort() {
        return localPort;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    private void start() {
        Logger logger = LoggerFactory.getLogger(BlockchainServer.class);
        logger.info("Block chain server started");

        remoteServerStatus.put(new ServerInfo(remoteHost, remotePort), new Date());
        //periodically send heartbeats
        new Thread(new HeartBeatPeriodicRunnable(this)).start();
        //periodically catchup
        new Thread(new CatchupPeriodicRunnable(this)).start();

        try {
            serverSocket = new ServerSocket(localPort);
            while (true) {
                clientSocket = serverSocket.accept();
                new Thread(new MessageHandlerRunnable(this)).start();

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
                e.printStackTrace();
            } /*catch (InterruptedException e) {
            }*/
        }
    }

    public static void main(String[] args) {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        if (args.length != 3) {
            System.out.println("Check your arguments.");
            System.out.println("local-port remote-host remote-port");
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

        new BlockchainServer(localPort, remoteHost, remotePort).start();

    }
}
