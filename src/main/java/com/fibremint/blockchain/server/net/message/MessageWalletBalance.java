package com.fibremint.blockchain.server.net.message;

import com.fibremint.blockchain.server.blockchain.TransactionOutput;

import java.util.HashMap;
import java.util.Map;

public class MessageWalletBalance extends MessageBase {
    public String publicKey;
    public float balance;
    public HashMap<String, MessageTransactionOutput> UTXOs;
    public boolean isBalanceOnly;

    public MessageWalletBalance(
            String publicKey, boolean isBalanceOnly, float balance, HashMap<String, TransactionOutput> UTXOs) {
        super(MessageType.walletBalance);
        this.publicKey = publicKey;
        this.isBalanceOnly = isBalanceOnly;
        this.balance = balance;
        this.UTXOs = new HashMap<>();
        for(Map.Entry<String, TransactionOutput> item : UTXOs.entrySet())
            this.UTXOs.put(item.getKey(), new MessageTransactionOutput(item.getValue()));
    }

    public MessageWalletBalance(String publicKey, boolean isBalanceOnly, float balance) {
        super(MessageType.walletBalance);
        this.publicKey = publicKey;
        this.isBalanceOnly = isBalanceOnly;
        this.balance = balance;
    }

}
