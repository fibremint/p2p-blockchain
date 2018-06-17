package com.fibremint.blockchain.server.net.message;

import com.fibremint.blockchain.server.blockchain.Block;
import com.fibremint.blockchain.server.blockchain.BlockHeader;
import com.fibremint.blockchain.server.blockchain.Transaction;

import java.util.ArrayList;
import java.util.List;

public class MessageBlock {
    public BlockHeader header;
    public List<MessageTransaction> transactions;

    public MessageBlock(Block block) {
        this.header = block.header;
        this.transactions = new ArrayList<>();
        for(Transaction transaction : block.transactions) {
            this.transactions.add(new MessageTransaction(transaction));
        }
    }
}
