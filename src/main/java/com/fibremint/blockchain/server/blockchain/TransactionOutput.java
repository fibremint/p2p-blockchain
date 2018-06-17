package com.fibremint.blockchain.server.blockchain;

import com.fibremint.blockchain.server.net.message.MessageTransactionOutput;
import com.fibremint.blockchain.server.util.HashUtil;
import com.fibremint.blockchain.server.util.SignatureUtil;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.Date;

public class TransactionOutput implements Serializable {
    public String hash;
    public long timestamp;
    public PublicKey recipient;
    public float value;
    public String parentTransactionHash;

    public TransactionOutput(PublicKey recipient, float value, String parentTransactionHash) {
        this.timestamp = new Date().getTime();
        this.recipient = recipient;
        this.value = value;
        this.parentTransactionHash = parentTransactionHash;
        this.hash = HashUtil.applySHA256(
                HashUtil.getEncodedKey(recipient) +
                        Long.toString(timestamp) +
                        Float.toString(value) +
                        parentTransactionHash);
    }

    public TransactionOutput(MessageTransactionOutput message) {
        this.hash = message.hash;
        this.timestamp = message.timestamp;
        this.recipient = SignatureUtil.generatePublicKey(HashUtil.getDecoded(message.recipient));
        this.value = message.value;
        this.parentTransactionHash = message.parentTransactionHash;
    }

    public boolean isMine(PublicKey publicKey) {
        return recipient.equals(publicKey);
    }
}
