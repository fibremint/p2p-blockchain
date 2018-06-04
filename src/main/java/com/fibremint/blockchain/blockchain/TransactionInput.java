package com.fibremint.blockchain.blockchain;

public class TransactionInput {
    public String transactionOutputHash;
    public TransactionOutput UTXO;

    public TransactionInput(String transactionOutputHash) {
        this.transactionOutputHash = transactionOutputHash;
    }
}
