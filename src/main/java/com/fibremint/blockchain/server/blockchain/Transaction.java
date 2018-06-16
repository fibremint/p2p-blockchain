package com.fibremint.blockchain.server.blockchain;

import com.fibremint.blockchain.server.util.HashUtil;
import com.fibremint.blockchain.server.util.SignatureUtil;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class Transaction implements Serializable {
    public String hash;
    public PublicKey sender;
    public PublicKey recipient;
    public float value;
    public byte[] signature;
    public ArrayList<TransactionInput> inputs = new ArrayList<>();
    public ArrayList<TransactionOutput> outputs = new ArrayList<>();

    private static int sequence = 0;

    public Transaction(PublicKey sender, PublicKey recipient, float value, ArrayList<TransactionInput> inputs) {
        this.outputs = new ArrayList<>();

        this.sender = sender;
        this.recipient = recipient;
        this.value = value;
        this.inputs = inputs;
    }

    public Transaction(String hash, String sender, String recipient, float value, String signature,
                       List<TransactionInput> inputs) {
        this.hash = hash;
        this.sender = SignatureUtil.generatePublicKey(HashUtil.getDecoded(sender));
        this.recipient = SignatureUtil.generatePublicKey(HashUtil.getDecoded(recipient));
        this.value = value;
        this.signature = HashUtil.getDecoded(signature);
        this.inputs = new ArrayList<>(inputs);
    }
    public boolean processTransaction() {
        if (!verifySignature()) {
            System.out.println("#Transaction signature failed to verify");
            return false;
        }

        for (TransactionInput i : inputs)
            i.UTXO = Blockchain.UTXOs.get(i.transactionOutputHash);

        if (getInputsValue() < Blockchain.minimumTransaction) {
            System.out.println("Transaction inputs too small: " + getInputsValue());
            System.out.println("Please enter the amount greater than " + Blockchain.minimumTransaction);
            return false;
        }

        float leftOver = getInputsValue() - value;
        hash = calculateHash();
        outputs.add(new TransactionOutput(this.recipient, value, hash));
        outputs.add(new TransactionOutput(this.sender, leftOver, hash));

        for (TransactionOutput o : outputs)
            Blockchain.UTXOs.put(o.hash, o);

        for (TransactionInput i : inputs) {
            if (i.UTXO == null) continue;;
            Blockchain.UTXOs.remove(i.UTXO.hash);
        }

        return true;
    }

    public float getInputsValue() {
        float total = 0;
        for(TransactionInput i : inputs) {
            if (i.UTXO == null) continue;;
            total += i.UTXO.value;
        }

        return total;
    }

    public float getOutputsValue() {
        float total = 0;
        for(TransactionOutput o : outputs)
            total += o.value;

        return total;
    }

    private String calculateHash() {
        sequence++;
        return HashUtil.applySHA256(
                HashUtil.getEncodedString(sender) +
                        HashUtil.getEncodedString(recipient) +
                        Float.toString(value) + sequence
        );
    }

    public void generateSignature(PrivateKey privateKey) {
        String data = HashUtil.getEncodedString(sender) +
                HashUtil.getEncodedString(recipient) +
                Float.toString(value);
        this.signature = SignatureUtil.applyECDSASignature(privateKey, data);
    }

    public boolean verifySignature() {
        String data = HashUtil.getEncodedString(sender) +
                HashUtil.getEncodedString(recipient) +
                Float.toString(value);
        return SignatureUtil.verifyECDSASignature(sender, data, signature);
    }

}
