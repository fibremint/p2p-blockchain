package com.fibremint.blockchain.server.net.message;

import com.fibremint.blockchain.server.blockchain.Transaction;
import com.fibremint.blockchain.server.blockchain.TransactionInput;
import com.fibremint.blockchain.server.util.HashUtil;

import java.util.ArrayList;
import java.util.List;

public class MessageTransaction extends MessageBase {
    public String hash;
    public String sender;
    public String recipient;
    public float value;
    public String signature;
    public List<MessageTransactionInput> inputs = new ArrayList<>();

    public MessageTransaction(Transaction transaction) {
        super(MessageType.transaction);
        this.hash = transaction.hash;
        this.sender = HashUtil.getEncodedKey(transaction.sender);
        this.recipient = HashUtil.getEncodedKey(transaction.recipient);
        this.value = transaction.value;
        this.signature = HashUtil.getEncodedString(transaction.signature);
        //this.inputs = transaction.inputs;
        for(TransactionInput transactionInput : transaction.inputs) {
            inputs.add(new MessageTransactionInput());
        }
    }
}
