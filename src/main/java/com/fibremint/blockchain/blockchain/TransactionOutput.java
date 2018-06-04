package com.fibremint.blockchain.blockchain;

import com.fibremint.blockchain.util.StringUtil;

import java.security.PublicKey;

public class TransactionOutput {
    public String hash;
    public PublicKey reciepient;
    public float value;
    public String parentTransactionHash;

    public TransactionOutput(PublicKey reciepient, float value, String parentTransactionHash) {
        this.reciepient = reciepient;
        this.value = value;
        this.parentTransactionHash = parentTransactionHash;
        this.hash = StringUtil.applySHA256(
                StringUtil.getStringFromKey(reciepient) +
                        Float.toString(value) +
                        parentTransactionHash);
    }

    public boolean isMine(PublicKey publicKey) {
        return publicKey == reciepient;
    }
}
