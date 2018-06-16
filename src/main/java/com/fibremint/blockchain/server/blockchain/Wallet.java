package com.fibremint.blockchain.server.blockchain;

import com.fibremint.blockchain.server.util.SignatureUtil;

import java.security.*;
import java.util.HashMap;

public class Wallet {
    public PrivateKey privateKey;
    public PublicKey publicKey;

    public HashMap<String, TransactionOutput> UTXOs;

    public Wallet() {
        UTXOs = new HashMap<>();
        KeyPair keyPair = SignatureUtil.generateKeyPair();
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
    }
}
