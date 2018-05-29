package com.fibremint.blockchain.message.model;

public class MessageTransaction extends MessageBase {
    private String sender;
    private String content;

    public MessageTransaction(String sender, String content) {
        super(MessageType.transaction);
        this.sender = sender;
        this.content = content;

    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }
}
