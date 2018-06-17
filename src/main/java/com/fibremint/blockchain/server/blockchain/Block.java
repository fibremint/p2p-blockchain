package com.fibremint.blockchain.server.blockchain;


import com.fibremint.blockchain.server.net.message.MessageBlock;
import com.fibremint.blockchain.server.net.message.MessageTransaction;

import java.util.ArrayList;

public class Block {
    public BlockHeader header;
    public ArrayList<Transaction> transactions = new ArrayList<>();

    public Block(MessageBlock message) {
        this.header = message.header;
        for(MessageTransaction messageTransaction : message.transactions) {
            transactions.add(new Transaction(messageTransaction));
        }
    }
}