package com.fibremint.blockchain.blockchain;

import java.util.ArrayList;
import java.util.HashMap;

public class Blockchain {
    public static ArrayList<Block> blockchain = new ArrayList<>();
    public static HashMap<String, TransactionOutput> UTXOs = new HashMap<>();
    public static int difficulty = 3;
    public static float minimumTransaction = 0.1f;

    public static Block getBlock(String blockHash) {
        return blockchain.stream().filter(block -> blockHash.equals(block.header.hash))
                .findFirst().orElse(null);
    }

    public static Block getLatestBlock() {
        return (blockchain.size() == 0) ? null : blockchain.get(blockchain.size() - 1);
    }

    public static int getLength() {
        return blockchain.size();
    }

    public static synchronized void catchUp(ArrayList<Block> blocks) {
        ArrayList<Block> blockchain = new ArrayList<>();

        for(int i = blocks.size() - 1; i > 0; i--)
            blockchain.add(blocks.get(i));

        // TODO: check validation would be required.
        Blockchain.blockchain = blockchain;
    }
}
