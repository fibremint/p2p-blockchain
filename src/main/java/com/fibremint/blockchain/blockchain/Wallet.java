package com.fibremint.blockchain.blockchain;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Wallet {
    private PrivateKey privateKey;
    public PublicKey publicKey;

    public HashMap<String, TransactionOutput> UTXOs;

    public Wallet() {
        UTXOs = new HashMap<>();
        generateKeyPair();
    }

    public Wallet(PrivateKey privateKey, PublicKey publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public void generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec("prime192v1");
            keyPairGenerator.initialize(ecGenParameterSpec, secureRandom);

            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    public Transaction sendFunds(PublicKey recipient, float value) {
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

        for (TransactionInput input : inputs) {
            UTXOs.remove(input.transactionOutputHash);
        }

        return newTransaction;
    }
}
