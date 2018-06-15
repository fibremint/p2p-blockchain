package com.fibremint.blockchain.server.net.message;


public class MessageLatestBlock extends MessageBase {
    public int localPort;
    public int blockchainLength;
    public int transactionLength;
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

    public void setTransactionLength(int transactionLength) {
        this.transactionLength = transactionLength;
    }
}
