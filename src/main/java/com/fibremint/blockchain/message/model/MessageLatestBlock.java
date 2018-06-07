package com.fibremint.blockchain.message.model;


public class MessageLatestBlock extends MessageBase {
    public int localPort;
    public int blockchainLength;
    public int transactionLength;
    public String latestHash;

    public MessageLatestBlock(int localPort, int blockchainLength, int transactionLength) {
        super(MessageType.latestBlock);
        this.localPort = localPort;
        this.blockchainLength = blockchainLength;
        this.transactionLength = transactionLength;
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
