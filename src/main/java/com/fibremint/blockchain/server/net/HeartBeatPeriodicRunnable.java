package com.fibremint.blockchain.server.net;

import com.fibremint.blockchain.server.net.message.MessageHeartbeat;

import java.util.Date;
import java.util.HashMap;

public class HeartBeatPeriodicRunnable implements Runnable {
    public static final int THREAD_SLEEP = 2000;

    private HashMap<ServerInfo, Date> serverStatus;
    private int sequenceNumber;
    private int localPort;

    public HeartBeatPeriodicRunnable(HashMap<ServerInfo, Date> serverStatus, int localPort) {
        this.serverStatus = serverStatus;
        this.sequenceNumber = 0;
        this.localPort = localPort;
        
    }

    @Override
    public void run() {
        MessageHeartbeat message;
        while(true) {
            // broadcast HeartBeat message to all peers
            //message = "hb|" + String.valueOf(localPort) + "|" + String.valueOf(sequenceNumber);
            message = new MessageHeartbeat(localPort, sequenceNumber);

            for (ServerInfo info : serverStatus.keySet()) {
                Thread thread = new Thread(new HeartBeatSenderRunnable(info, message));
                thread.start();
            }

            // increment the sequenceNumber
            sequenceNumber += 1;
            
            // sleep for two seconds
            try {
                Thread.sleep(THREAD_SLEEP);
            } catch (InterruptedException e) {
            }
        }
    }
}
