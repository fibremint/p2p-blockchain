package com.fibremint.blockchain.message.model;

public class MessageProperties extends MessageBase {
    public String blockHash;
    public int difficulty;
    public float minimumTransaction;
    public float miningReward;

    public MessageProperties() {
        super(MessageType.properties);
    }

    public MessageProperties(String blockHash, int difficulty, float minimumTransaction, float miningReward) {
        super(MessageType.properties);
        this.blockHash = blockHash;
        this.difficulty = difficulty;
        this.minimumTransaction = minimumTransaction;
        this.miningReward = miningReward;
    }
}
