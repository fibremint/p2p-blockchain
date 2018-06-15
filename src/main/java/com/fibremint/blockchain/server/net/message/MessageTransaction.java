package com.fibremint.blockchain.server.net.message;

public class MessageTransaction extends MessageBase {
    public String transactionHash;
    public String sender;
    public String recipient;
    public float value;

    public MessageTransaction(
            String transactionHash, String sender, String recipient, float value) {
        super(MessageType.transaction);
        this.transactionHash = transactionHash;
        this.sender = sender;
        this.recipient = recipient;
        this.value = value;
    }
}
