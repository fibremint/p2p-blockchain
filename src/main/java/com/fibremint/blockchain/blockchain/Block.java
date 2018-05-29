package com.fibremint.blockchain.blockchain;

import com.fibremint.blockchain.message.model.MessageTransaction;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;

public class Block implements Serializable {

    private transient Block previousBlock;
    private byte[] previousHash;
    private byte[] currentHash;
    private ArrayList<Transaction> transactions;
    private Gson gson;

    public Block() {
        transactions = new ArrayList<>();
        gson = new Gson();
    }

    public byte[] getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(byte[] prevHash) {
        this.previousHash = prevHash;
    }

    public byte[] getCurrentHash() {
        return currentHash;
    }

    public void setCurrentHash(byte[] currentHash) {
        this.currentHash = currentHash;
    }

    public Block getPreviousBlock() {
        return previousBlock;
    }

    public void setPreviousBlock(Block previousBlock) {
        this.previousBlock = previousBlock;
    }

    public ArrayList<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(ArrayList<Transaction> transactions) {
        this.transactions = transactions;
    }

    public void addTransaction(Transaction transaction) {
        this.transactions.add(transaction);
        this.currentHash = calculateHash();
    }

    // TODO: rework out.writeUTF
    public byte[] calculateHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            ByteArrayOutputStream baout = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baout);
            out.write(previousHash);
            for (Transaction tx : transactions) {
                out.writeUTF(gson.toJson(new MessageTransaction(tx.getSender(), tx.getContent())));
                //out.writeUTF("tx|" + tx.getSender() + "|" + tx.getContent());
            }
            byte[] bytes = baout.toByteArray();
            return digest.digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    public byte[] calculateHashWithNonce(int nonce) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            ByteArrayOutputStream baout = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baout);
            out.write(nonce);
            out.write(previousHash);
            for (Transaction tx : transactions) {
                out.writeUTF(gson.toJson(new MessageTransaction(tx.getSender(), tx.getContent())));
                //out.writeUTF("tx|" + tx.getSender() + "|" + tx.getContent());
            }
            byte[] bytes = baout.toByteArray();
            return digest.digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    public String toString() {
        String cutOffRule = new String(new char[81]).replace("\0", "-") + "\n";
        String prevHashString = String.format("|PreviousHash:|%65s|\n", Base64.getEncoder().encodeToString(previousHash));
        String hashString = String.format("|CurrentHash:|%66s|\n", Base64.getEncoder().encodeToString(calculateHash()));
        String transactionsString = "";
        for (Transaction tx : transactions) {
            transactionsString += tx.toString();
        }
        return "Block:\n"
                + cutOffRule
                + hashString
                + cutOffRule
                + transactionsString
                + cutOffRule
                + prevHashString
                + cutOffRule;
    }
}