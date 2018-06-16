package com.fibremint.blockchain.server.net.message;

import com.fibremint.blockchain.server.blockchain.TransactionOutput;
import com.fibremint.blockchain.server.util.HashUtil;


public class MessageTransactionOutput {
    public String hash;
    public Long timestamp;
    public String recipient;
    public float value;
    public String parentTransactionHash;

    public MessageTransactionOutput(TransactionOutput transactionOutput) {
        this.hash = transactionOutput.hash;
        this.timestamp = transactionOutput.timestamp;
        this.recipient = HashUtil.getEncodedKey(transactionOutput.recipient);
        this.value = transactionOutput.value;
        this.parentTransactionHash = transactionOutput.parentTransactionHash;
    }
}
