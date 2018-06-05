package com.fibremint.blockchain.message.model;

public abstract class MessageBase {
    public MessageType type;

    public MessageBase(MessageType type) {
        this.type = type;
    }


}
