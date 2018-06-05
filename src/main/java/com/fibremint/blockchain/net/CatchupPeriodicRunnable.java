package com.fibremint.blockchain.net;

import com.fibremint.blockchain.blockchain.Blockchain;
import com.fibremint.blockchain.message.MessageSenderRunnable;
import com.fibremint.blockchain.message.model.MessageLastBlock;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Collections;

public class CatchupPeriodicRunnable implements Runnable {
    public static final int THREAD_SLEEP = 2000;

	private Blockchain blockchain;
	private HashMap<ServerInfo, Date> serverStatus;
	private int localPort;
	
	public CatchupPeriodicRunnable(Blockchain blockchain, HashMap<ServerInfo, Date> serverStatus, int localPort) {
		this.blockchain = blockchain;
		this.serverStatus = serverStatus;
		this.localPort = localPort;
	}
	
	@Override
	public void run() {
	    Gson gson = new Gson();
		while(true) {
			//String LBmessage = "lb|" + String.valueOf(localPort) + "|" + String.valueOf(blockchain.getLength()) + "|";
            MessageLastBlock message = new MessageLastBlock(localPort, blockchain.getLength());
            if (blockchain.getLastBlock() != null) {
				byte[] latestHash = blockchain.getLastBlock().calculateHash();
				if (latestHash != null) {
                    //LBmessage += Base64.getEncoder().encodeToString(latestHash);
                    message.setLatestHash(latestHash);
				} else {
                    //LBmessage += "null";
                }
			} else {
				//LBmessage += "null";
			}
			 
			
			if (serverStatus.size() <= 5) {
				this.broadcast(gson.toJson(message));
				
			} else {
				//select 5 random peers
				ArrayList<ServerInfo> targetPeers = new ArrayList<ServerInfo>();
				ArrayList<ServerInfo> allPeers = new ArrayList(serverStatus.keySet());
				
				for (int i = 0; i < 5; i++) {
					Collections.shuffle(allPeers);
					targetPeers.add(allPeers.remove(0));
				}
				this.multicast(targetPeers, gson.toJson(message));
				
			}

			//sleep for 2 secs
			try {
				Thread.sleep(THREAD_SLEEP);
			} catch (Exception e) {
			}
		}
	}
	
    public void broadcast(String message) {
    	ArrayList<Thread> threadArrayList = new ArrayList<Thread>();
    	for (ServerInfo info: this.serverStatus.keySet()) {
            Thread thread = new Thread(new MessageSenderRunnable(info, message));
            thread.start();
            threadArrayList.add(thread);
        }
    }
    
    public void multicast(ArrayList<ServerInfo> toPeers, String message) {
    	ArrayList<Thread> threadArrayList = new ArrayList<Thread>();
    	for (int i = 0; i < toPeers.size(); i++) {
    		Thread thread = new Thread(new MessageSenderRunnable(toPeers.get(i), message));
    		thread.start();
    		threadArrayList.add(thread);
    	}
    }
}