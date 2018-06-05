package com.fibremint.blockchain.blockchain;

import com.fibremint.blockchain.util.HashUtil;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

public class Transaction {
    public String transactionHash;
    public PublicKey sender;
    public PublicKey recipient;
    public float value;
    public byte[] signature;
    public ArrayList<TransactionInput> inputs;
    public ArrayList<TransactionOutput> outputs;

    private static int sequence = 0;

    public Transaction(PublicKey sender, PublicKey recipient, float value, ArrayList<TransactionInput> inputs) {
        this.outputs = new ArrayList<>();

        this.sender = sender;
        this.recipient = recipient;
        this.value = value;
        this.inputs = inputs;
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
        transactionHash = calculateHash();
        outputs.add(new TransactionOutput(this.recipient, value, transactionHash));
        outputs.add(new TransactionOutput(this.sender, leftOver, transactionHash));

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
                HashUtil.getStringFromKey(sender) +
                        HashUtil.getStringFromKey(recipient) +
                        Float.toString(value) + sequence
        );
    }

    public void generateSignature(PrivateKey privateKey) {
        String data = HashUtil.getStringFromKey(sender) +
                HashUtil.getStringFromKey(recipient) +
                Float.toString(value);
        this.signature = HashUtil.applyECDSASignature(privateKey, data);
    }

    public boolean verifySignature() {
        String data = HashUtil.getStringFromKey(sender) +
                HashUtil.getStringFromKey(recipient) +
                Float.toString(value);
        return HashUtil.verifyECDSASignature(sender, data, signature);
    }

}
