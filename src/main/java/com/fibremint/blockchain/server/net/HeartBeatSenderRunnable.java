package com.fibremint.blockchain.server.net;

import com.fibremint.blockchain.server.net.message.MessageHeartbeat;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class HeartBeatSenderRunnable implements Runnable{
    public static final int HEARTBEAT_TIMEOUT = 2000;

    private ServerInfo destServer;
    private MessageHeartbeat message;
    private Gson gson;

    public HeartBeatSenderRunnable(ServerInfo destServer, MessageHeartbeat message) {
        this.destServer = destServer;
        this.message = message;

        gson = new Gson();
    }

    @Override
    public void run() {
        try {
            // create socket with a timeout of 2 seconds
            Socket s = new Socket();
            s.connect(new InetSocketAddress(this.destServer.getHost(), this.destServer.getPort()), HEARTBEAT_TIMEOUT);
            PrintWriter pw =  new PrintWriter(s.getOutputStream(), true);
            
            // send the message forward
        	pw.println(gson.toJson(message));
        	pw.flush();

            // close printWriter and socket
            pw.close();
            // sleep for two seconds
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            s.close();
            
        } catch (IOException e) {
        }
    }
}