package com.fibremint.blockchain.blockchain;

import com.fibremint.blockchain.util.StringUtil;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

public class Transaction {
    public String transactionHash;
    public PublicKey sender;
    public PublicKey reciepient;
    public float value;
    public byte[] signature;
    public ArrayList<TransactionInput> inputs;
    public ArrayList<TransactionOutput> outputs;

    private static int sequence = 0;

    public Transaction(PublicKey sender, PublicKey reciepient, float value, ArrayList<TransactionInput> inputs) {
        this.outputs = new ArrayList<>();

        this.sender = sender;
        this.reciepient = reciepient;
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
        outputs.add(new TransactionOutput(this.reciepient, value, transactionHash));
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
        return StringUtil.applySHA256(
                StringUtil.getStringFromKey(sender) +
                        StringUtil.getStringFromKey(reciepient) +
                        Float.toString(value) + sequence
        );
    }

    public void generateSignature(PrivateKey privateKey) {
        String data = StringUtil.getStringFromKey(sender) +
                StringUtil.getStringFromKey(reciepient) +
                Float.toString(value);
        this.signature = StringUtil.applyECDSASignature(privateKey, data);
    }

    public boolean verifySignature() {
        String data = StringUtil.getStringFromKey(sender) +
                StringUtil.getStringFromKey(reciepient) +
                Float.toString(value);
        return StringUtil.verifyECDSASignature(sender, data, signature);
    }

}
