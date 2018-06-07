package com.fibremint.blockchain.message.model;

public class MessageProperties extends MessageBase {
    public int difficulty;
    public float minimumTransaction;

    public MessageProperties() {
        super(MessageType.properties);
    }

    public MessageProperties(int difficulty, float minimumTransaction) {
        super(MessageType.properties);
        this.difficulty = difficulty;
        this.minimumTransaction = minimumTransaction;
    }
}
