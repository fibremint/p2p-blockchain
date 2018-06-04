package com.fibremint.blockchain.blockchain;

import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;

public class Blockchain {
    public static ArrayList<Block> blockchain = new ArrayList<>();
    public static HashMap<String, TransactionOutput> UTXOs = new HashMap<>();

    public static int difficulty = 3;
    public static float minimumTransaction = 0.1f;


    public Blockchain() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
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
    public static void addBlock(Block newBlock) {
        newBlock.mineBlock(difficulty);
        blockchain.add(newBlock);
    }
}
