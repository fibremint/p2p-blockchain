package com.fibremint.blockchain.message.model;

public class MessageCatchUp extends MessageBase{
    String blockHash;

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

    public boolean hasBlockchain() {
        return !blockHash.equals("");
    }
}
