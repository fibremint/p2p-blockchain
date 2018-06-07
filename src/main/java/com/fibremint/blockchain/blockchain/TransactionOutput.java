package com.fibremint.blockchain.blockchain;

import com.fibremint.blockchain.util.HashUtil;

import java.io.Serializable;
import java.security.PublicKey;

public class TransactionOutput implements Serializable {
    public String hash;
    public PublicKey recipient;
    public float value;
    public String parentTransactionHash;

    public TransactionOutput(PublicKey recipient, float value, String parentTransactionHash) {
        this.recipient = recipient;
        this.value = value;
        this.parentTransactionHash = parentTransactionHash;
        this.hash = HashUtil.applySHA256(
                HashUtil.getStringFromKey(recipient) +
                        Float.toString(value) +
                        parentTransactionHash);
    }

    public boolean isMine(PublicKey publicKey) {
        return publicKey == recipient;
    }
}
