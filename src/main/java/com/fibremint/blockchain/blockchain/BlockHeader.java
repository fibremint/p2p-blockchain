package com.fibremint.blockchain.blockchain;

import com.fibremint.blockchain.util.StringUtil;
import com.google.gson.Gson;

import java.util.Date;

public class BlockHeader {
    private int version;
    public String hash;
    public String previousHash;
    public String merkleRootHash;
    private long timestamp;
    public int nonce;

    public BlockHeader(String previousHash) {
        this.version = 1;
        this.previousHash = previousHash;
        this.timestamp = new Date().getTime();

        this.hash = calculateHash();
    }

    /*public int getVersion() {
        return version;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public int getMerkleRootHash() {
        return merkleRootHash;
    }

    public void setMerkleRootHash(int merkleRootHash) {
        this.merkleRootHash = merkleRootHash;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getAsSerialized() {
        return Integer.toString(version)
                + previousHash
                + Integer.toString(merkleRootHash)
                + Long.toString(timestamp);
    }*/

    public String calculateHash() {
        return StringUtil.applySHA256(
                previousHash +
                        Long.toString(timestamp) +
                        Integer.toString(nonce) +
                        merkleRootHash
        );
    }

    /*public void calculateHash() {
        this.hash =  StringUtil.applySHA256(header.getAsSerialized() + new Gson().toJson(transactions));
    }*/


}
