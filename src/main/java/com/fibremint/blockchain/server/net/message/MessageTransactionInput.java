package com.fibremint.blockchain.server.net.message;

import com.fibremint.blockchain.server.blockchain.TransactionInput;

public class MessageTransactionInput {
    public String transactionOutputHash;
    public MessageTransactionOutput UTXO;

    public MessageTransactionInput(TransactionInput transactionInput) {
        this.transactionOutputHash = transactionInput.transactionOutputHash;
        this.UTXO = new MessageTransactionOutput(transactionInput.UTXO);
    }
}
