package com.fibremint.blockchain.server.net.message;

import com.fibremint.blockchain.server.blockchain.TransactionOutput;

import java.util.HashMap;
import java.util.Map;

public class MessageWalletBalance extends MessageBase {
    public String publicKey;
    public float balance;
    public HashMap<String, MessageTransactionOutput> UTXOs;

    public MessageWalletBalance(String publicKey, float balance, HashMap<String, TransactionOutput> UTXOs) {
        super(MessageType.walletBalance);
        this.publicKey = publicKey;
        this.balance = balance;
        this.UTXOs = new HashMap<>();
        for(Map.Entry<String, TransactionOutput> item : UTXOs.entrySet())
            this.UTXOs.put(item.getKey(), new MessageTransactionOutput(item.getValue()));
    }

}
