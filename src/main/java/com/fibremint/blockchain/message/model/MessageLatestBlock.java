package com.fibremint.blockchain.message.model;


public class MessageLatestBlock extends MessageBase {
    public int localPort;
    public int blockchainLength;
    public String latestHash;

    public MessageLatestBlock(int localPort, int blockchainLength) {
        super(MessageType.latestBlock);
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

    public void setLatestHash(String latestHash) {
        this.latestHash = latestHash;
    }
}