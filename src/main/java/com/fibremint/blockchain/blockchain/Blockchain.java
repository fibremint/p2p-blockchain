package com.fibremint.blockchain.blockchain;

import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;

public class Blockchain {
    private ArrayList<Block> blockchain = new ArrayList<>();
    public static HashMap<String, TransactionOutput> UTXOs = new HashMap<>();
    public static int difficulty = 3;
    public static float minimumTransaction = 0.1f;

    public Blockchain() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    public HashMap<String, TransactionOutput> getUTXOs() {
        return UTXOs;
    }

    public Block getBlock(String blockHash) {
        return blockchain.stream().filter(block -> blockHash.equals(block.getHeader().hash))
                .findFirst().orElse(null);
    }

    public Block getLastBlock() {
        return blockchain.get(blockchain.size() - 1);
    }

    public int getLength() {
        return blockchain.size() -1;
    }

    public void catchUp(ArrayList<Block> blocks) {
        ArrayList<Block> blockchain = new ArrayList<>();

        for(int i = blocks.size() - 1; i > 0; i--)
            blockchain.add(blocks.get(i));

        // TODO: check validation would be required.
        this.blockchain = blockchain;
    }
    /*
            public static boolean isChainValid() {
                Block currentBlock, previousBlock;
                String hashTarget = new String(new char[difficulty]).replace('\0', '0');
                HashMap<String ,TransactionOutput> tempUTXOs = new HashMap<>();
                tempUTXOs.put()
                for(int i=1; i < blockchain.size(); i++) {
                    currentBlock = blockchain.get(i);
                    previousBlock = blockchain.get(i-1);

                    if (!currentBlock.hash.equals(currentBlock.calculateHash())
                            || !previousBlock.hash.equals((currentBlock.getHeader().previousHash)))
                        return false;
                }

                return true;
            }*/
/*    public static void addBlock(Block newBlock) {
        newBlock.mineBlock(difficulty);
        blockchain.add(newBlock);
    }*/
}
