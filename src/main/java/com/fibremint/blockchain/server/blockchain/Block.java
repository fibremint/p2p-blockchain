package com.fibremint.blockchain.server.blockchain;

import com.fibremint.blockchain.server.util.HashUtil;

import java.io.Serializable;
import java.util.ArrayList;

public class Block implements Serializable {
    public BlockHeader header;
    public ArrayList<Transaction> transactions;

    public Block(BlockHeader header) {
        this.header = header;
        this.transactions = new ArrayList<>();
    }


    public void mineBlock(int difficulty) {
        header.merkleRootHash = HashUtil.getMerkleRoot(transactions);
        String target = HashUtil.getDifficultyString(difficulty);
        while(!header.hash.substring(0, difficulty).equals(target)) {
            header.nonce++;
            header.hash = header.calculateHash();
        }

        System.out.println("Block mined");
    }

    public boolean addTransaction(Transaction transaction) {
        if (transaction == null) return false;
        if (!"0".equals(transaction.hash)) {
            if (!transaction.processTransaction()) {
                System.out.println("Transaction failed to process. Discarded.");
                return false;
            }
        }

        transactions.add(transaction);
        System.out.println("Transaction successfully added to block");
        return true;
    }


}