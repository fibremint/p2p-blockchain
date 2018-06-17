package com.fibremint.blockchain.server.blockchain;


import com.fibremint.blockchain.server.net.message.MessageBlock;
import com.fibremint.blockchain.server.net.message.MessageTransaction;

import java.io.Serializable;
import java.util.ArrayList;

public class Block {
    public BlockHeader header;
    public ArrayList<Transaction> transactions = new ArrayList<>();
    ;

    public Block(BlockHeader header) {
        this.header = header;
    }

    public Block(Block block) {
        this.header = block.header;
        this.transactions = new ArrayList<>(block.transactions);
    }

    public Block(MessageBlock message) {
        this.header = message.header;
        for(MessageTransaction messageTransaction : message.transactions) {
            transactions.add(new Transaction(messageTransaction));
        }
    }
/*    public boolean addTransaction(Transaction transaction) {
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
    }*/
}