package com.fibremint.blockchain.server.blockchain;

import com.fibremint.blockchain.server.util.HashUtil;

import java.io.Serializable;
import java.util.Date;

public class BlockHeader implements Serializable {
    private int version;
    public String hash;
    public String previousHash;
    public String merkleRootHash;
    private long timestamp;
    public int nonce;

/*    public BlockHeader(String previousHash) {
        this.version = 1;
        this.previousHash = previousHash;
        this.timestamp = new Date().getTime();

        this.hash = calculateHash();
    }

    public BlockHeader(String hash, String previousHash, String merkleRootHash, long timestamp, int nonce) {
        this.version = 1;
        this.hash = hash;
        this.previousHash = previousHash;
        this.merkleRootHash = merkleRootHash;
        this.timestamp = timestamp;
        this.nonce = nonce;
    }*/

    public String calculateHash() {
        return HashUtil.applySHA256(
                previousHash +
                        Long.toString(timestamp) +
                        Integer.toString(nonce) +
                        merkleRootHash
        );
    }
}
