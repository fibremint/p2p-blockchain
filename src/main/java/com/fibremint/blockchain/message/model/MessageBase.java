package com.fibremint.blockchain.message.model;

public abstract class MessageBase {
    private MessageType type;

    public MessageBase(MessageType type) {
        this.type = type;
    }


}
