package com.fibremint.blockchain.server.blockchain;

import com.fibremint.blockchain.server.net.message.MessageTransactionInput;

import java.io.Serializable;

public class TransactionInput implements Serializable {
    public String transactionOutputHash;
    public TransactionOutput UTXO;

    public TransactionInput(MessageTransactionInput messageTransactionInput) {
        this.transactionOutputHash = messageTransactionInput.transactionOutputHash;
        this.UTXO = new TransactionOutput(messageTransactionInput.UTXO);
    }
}
