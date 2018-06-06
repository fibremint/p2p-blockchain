package com.fibremint.blockchain.message.model;

public class MessageCatchUp extends MessageBase{
    public String blockHash;

    public MessageCatchUp() {
        super(MessageType.catchUp);
    }

    public MessageCatchUp(String blockHash) {
        super(MessageType.catchUp);
        this.blockHash = blockHash;

    }

    public String getBlockHash() {
        return blockHash;
    }

    public boolean hasBlockHash() {
        return blockHash != null;
    }
}
