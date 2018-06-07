package com.fibremint.blockchain.blockchain;

import com.fibremint.blockchain.net.ServerInfo;
import com.fibremint.blockchain.util.HashUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class BlockMiner implements Runnable {
    private Block newBlock;
    private Wallet wallet;
    private ServerInfo serverInfo;

    public BlockMiner(Wallet wallet, ServerInfo serverInfo) {
        this.wallet = wallet;
        this.serverInfo = serverInfo;
    }

    public void mine(int difficulty) {
        Gson gson = new GsonBuilder().create();

        header.merkleRootHash = HashUtil.getMerkleRoot(transactions);
        String target = HashUtil.getDifficultyString(difficulty);
        while(!header.hash.substring(0, difficulty).equals(target)) {
            header.nonce++;
            header.hash = header.calculateHash();
        }

        System.out.println("Block mined");
    }

    @Override
    public void run() {

    }
}

