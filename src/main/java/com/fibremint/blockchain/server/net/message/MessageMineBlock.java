package com.fibremint.blockchain.server.net.message;

import com.fibremint.blockchain.server.blockchain.Block;

public class MessageMineBlock extends MessageBase {
    public Block block;
    public String miner;

    public MessageMineBlock(Block block, String miner) {
        super(MessageType.mineBlock);
        this.block = block;
        this.miner = miner;
    }
}
