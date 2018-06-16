package com.fibremint.blockchain.server.blockchain;

import com.fibremint.blockchain.server.util.HashUtil;

import java.security.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Wallet {
    public PrivateKey privateKey;
    public PublicKey publicKey;

    public HashMap<String, TransactionOutput> UTXOs;

    public Wallet() {
        UTXOs = new HashMap<>();
        KeyPair keyPair = HashUtil.generateKeyPair();
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
    }

/*
    public Wallet(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public float getBalance() {
        float total = 0;
        for (Map.Entry<String ,TransactionOutput> item : Blockchain.UTXOs.entrySet()) {
            TransactionOutput UTXO = item.getValue();
            if (UTXO.isMine(publicKey)) {
                UTXOs.put(UTXO.hash, UTXO);
                total += UTXO.value;
            }
        }

        return total;
    }
*/

    /*public Transaction sendFunds(PublicKey recipient, float value) {
        if (getBalance() < value) {
            System.out.println("#Not enough funds to send transaction. Transaction discarded");
            return null;
        }

        ArrayList<TransactionInput> inputs = new ArrayList<>();
        float total = 0;
        for(Map.Entry<String, TransactionOutput> item : UTXOs.entrySet()) {
            TransactionOutput UTXO = item.getValue();
            total += UTXO.value;
            inputs.add(new TransactionInput(UTXO.hash));
            if (total > value) break;
        }

        Transaction newTransaction = new Transaction(publicKey, recipient, value, inputs);
        newTransaction.generateSignature(privateKey);

        for (TransactionInput input : inputs)
            UTXOs.remove(input.transactionOutputHash);

        return newTransaction;
    }*/
}
