package com.fibremint.blockchain.server.blockchain;

import java.io.Serializable;

public class TransactionInput implements Serializable {
    public String transactionOutputHash;
    public TransactionOutput UTXO;

    public TransactionInput(String transactionOutputHash) {
        this.transactionOutputHash = transactionOutputHash;
    }
}
