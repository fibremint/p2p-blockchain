package com.fibremint.blockchain.blockchain;

import com.fibremint.blockchain.util.HashUtil;

import java.util.ArrayList;

public class Block {
    private BlockHeader header;
    public ArrayList<Transaction> transactions;

    public Block(BlockHeader header) {
        this.header = header;
        this.transactions = new ArrayList<>();
    }

    public BlockHeader getHeader() {
        return header;
    }

    public ArrayList<Transaction> getTransactions() {
        return transactions;
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
        if (!"0".equals(header.previousHash)) {
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