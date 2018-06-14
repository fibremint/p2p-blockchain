package com.fibremint.blockchain.server.net.message;

public class MessageCatchUp extends MessageBase{
    public int blockIndex;

    public MessageCatchUp(int blockIndex) {
        super(MessageType.catchUp);
        this.blockIndex = blockIndex;

    }
}
