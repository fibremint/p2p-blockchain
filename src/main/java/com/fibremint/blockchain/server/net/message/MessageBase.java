package com.fibremint.blockchain.server.net.message;

public abstract class MessageBase {
    public MessageType type;

    public MessageBase(MessageType type) {
        this.type = type;
    }


}
