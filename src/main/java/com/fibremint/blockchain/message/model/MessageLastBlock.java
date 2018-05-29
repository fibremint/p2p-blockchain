package com.fibremint.blockchain.message.model;

import java.util.Base64;

public class MessageLastBlock extends MessageBase {
    private int localPort;
    private int blockchainLength;
    private String latestHash;

    public MessageLastBlock(int localPort, int blockchainLength) {
        super(MessageType.lastBlock);
        this.localPort = localPort;
        this.blockchainLength = blockchainLength;
    }

    public int getLocalPort() {
        return localPort;
    }

    public int getBlockchainLength() {
        return blockchainLength;
    }

    public String getLatestHash() {
        return latestHash;
    }

    public void setLatestHash(byte[] latestHash) {
        this.latestHash = Base64.getEncoder().encodeToString(latestHash);
    }
}
