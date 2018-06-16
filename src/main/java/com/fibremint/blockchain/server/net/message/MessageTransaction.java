package com.fibremint.blockchain.server.net.message;

import com.fibremint.blockchain.server.blockchain.TransactionInput;

import java.util.List;

public class MessageTransaction extends MessageBase {
    public String hash;
    public String sender;
    public String recipient;
    public float value;
    public String signature;
    public List<TransactionInput> inputs;

    public MessageTransaction(
            String hash, String sender, String recipient, float value, String signature, List<TransactionInput> inputs) {
        super(MessageType.transaction);
        this.hash = hash;
        this.sender = sender;
        this.recipient = recipient;
        this.value = value;
        this.signature = signature;
        this.inputs = inputs;
    }
}
