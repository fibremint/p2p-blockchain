package com.fibremint.blockchain.server.blockchain;

import com.fibremint.blockchain.server.net.message.MessageTransaction;
import com.fibremint.blockchain.server.util.HashUtil;
import com.fibremint.blockchain.server.util.SignatureUtil;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

public class Transaction {
    public String hash;
    public PublicKey sender;
    public PublicKey recipient;
    public float value;
    public byte[] signature;
    public ArrayList<TransactionInput> inputs = new ArrayList<>();
    public ArrayList<TransactionOutput> outputs = new ArrayList<>();

    private static int sequence = 0;

    // TODO: remove boolean parameter
    public Transaction(String minerEncodedPublicKey, boolean isBlockchainEmpty) {
        Wallet coinProvider = new Wallet();

        this.sender = coinProvider.publicKey;
        this.recipient = SignatureUtil.generatePublicKey(HashUtil.getDecoded(minerEncodedPublicKey));
        this.value = Blockchain.miningReward;
        this.inputs = new ArrayList<>();
        this.outputs = new ArrayList<>();

        generateSignature(coinProvider.privateKey);
        this.hash = "0";
        this.outputs.add(new TransactionOutput(
                this.recipient,
                this.value,
                this.hash
        ));
    }

    public Transaction(MessageTransaction message) {
        this.hash = message.hash;
        this.sender = SignatureUtil.generatePublicKey(HashUtil.getDecoded(message.sender));
        this.recipient = SignatureUtil.generatePublicKey(HashUtil.getDecoded(message.recipient));
        this.value = message.value;
        this.signature = HashUtil.getDecoded(message.signature);
        this.inputs = new ArrayList<>(message.inputs);
    }

    public boolean processTransaction(Blockchain blockchain) {
        if (!verifySignature()) {
            System.out.println("#Transaction signature failed to verify");
            return false;
        }

        for (TransactionInput i : inputs)
            i.UTXO = blockchain.UTXOs.get(i.transactionOutputHash);

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
            blockchain.UTXOs.put(o.hash, o);

        for (TransactionInput i : inputs) {
            if (i.UTXO == null) continue;;
            blockchain.UTXOs.remove(i.UTXO.hash);
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
                HashUtil.getEncodedKey(sender) +
                        HashUtil.getEncodedKey(recipient) +
                        Float.toString(value) + sequence
        );
    }

    public void generateSignature(PrivateKey privateKey) {
        String data = HashUtil.getEncodedKey(sender) +
                HashUtil.getEncodedKey(recipient) +
                Float.toString(value);
        this.signature = SignatureUtil.applyECDSASignature(privateKey, data);
    }

    public boolean verifySignature() {
        String data = HashUtil.getEncodedKey(sender) +
                HashUtil.getEncodedKey(recipient) +
                Float.toString(value);
        return SignatureUtil.verifyECDSASignature(sender, data, signature);
    }

}
