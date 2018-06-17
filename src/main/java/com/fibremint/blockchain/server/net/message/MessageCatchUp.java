package com.fibremint.blockchain.server.net.message;

import com.fibremint.blockchain.server.blockchain.Block;
import com.fibremint.blockchain.server.blockchain.TransactionInput;
import com.fibremint.blockchain.server.blockchain.TransactionOutput;

import java.util.HashMap;
import java.util.List;

public class MessageCatchUp extends MessageBase{
    public int blockIndex;
    public List<Block> catchUpBlocks;
    public HashMap<String, MessageTransactionOutput> UTXOs;

    public MessageCatchUp(int blockIndex) {
        super(MessageType.catchUp);
        this.blockIndex = blockIndex;

    }

    public MessageCatchUp(int blockIndex, List<Block> catchUpBlocks, HashMap<String ,MessageTransactionOutput> UTXOs) {
        super(MessageType.catchUp);
        this.blockIndex = blockIndex;
        this.catchUpBlocks = catchUpBlocks;
        this.UTXOs = UTXOs;
    }
}
