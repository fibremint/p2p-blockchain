package com.fibremint.blockchain.server.net;

import com.fibremint.blockchain.server.BlockchainServer;
import com.fibremint.blockchain.server.RootClassAccessibleAbstract;
import com.fibremint.blockchain.server.blockchain.Block;
import com.fibremint.blockchain.server.blockchain.Blockchain;
import com.fibremint.blockchain.server.net.message.MessageLatestBlock;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Collections;

public class CatchupPeriodicRunnable extends RootClassAccessibleAbstract implements Runnable {
    public static final int THREAD_SLEEP = 2000;

    private Blockchain blockchain;

	private HashMap<ServerInfo, Date> serverStatus;
	private int localPort;
	
	public CatchupPeriodicRunnable(BlockchainServer blockchainServer) {
	    super(blockchainServer);
		this.serverStatus = blockchainServer.getRemoteServerStatus();
		this.localPort = blockchainServer.getLocalPort();
		this.blockchain = blockchainServer.getBlockchain();
	}
	
	@Override
	public void run() {
	    Gson gson = new Gson();
	    Block latestBlock;
		while(true) {
            MessageLatestBlock message = new MessageLatestBlock(localPort, blockchain.getLength());
            latestBlock = blockchain.getLatestBlock();
            if (latestBlock != null) {
				String latestHash = latestBlock.header.calculateHash();
				if (latestHash != null) {
                    message.setLatestHash(latestHash);
                    message.setTransactionLength(latestBlock.transactions.size());
				} else {
                    // Blockchain hasn't any of latestBlock
                    message.setLatestHash("0");
                }
			} else {
                message.setLatestHash("0");
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
