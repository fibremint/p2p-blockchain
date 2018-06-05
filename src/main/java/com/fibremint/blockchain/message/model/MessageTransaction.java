package com.fibremint.blockchain.message.model;

public class MessageTransaction extends MessageBase {
    public String transactionHash;
    public String senderPrivateKey;
    public String senderPublicKey;
    public String recipient;
    public float value;

    public MessageTransaction(
            String transactionHash, String senderPrivateKey, String senderPublicKey, String recipient, float value) {
        super(MessageType.transaction);
        this.transactionHash = transactionHash;
        this.senderPrivateKey = senderPrivateKey;
        this.senderPublicKey = senderPublicKey;
        this.recipient = recipient;
        this.value = value;
    }
}
