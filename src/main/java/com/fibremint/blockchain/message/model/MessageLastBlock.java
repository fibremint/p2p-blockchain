package com.fibremint.blockchain.message.model;

import com.fibremint.blockchain.util.HashUtil;

public class MessageLastBlock extends MessageBase {
    public int localPort;
    public int blockchainLength;
    public String latestHash;

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
        this.latestHash = HashUtil.getStringFromByteArray(latestHash);
    }
}
