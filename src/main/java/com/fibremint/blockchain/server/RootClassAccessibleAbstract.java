package com.fibremint.blockchain.server;


public abstract class RootClassAccessibleAbstract {
    private BlockchainServer blockchainServer;

    public RootClassAccessibleAbstract(BlockchainServer blockchainServer) {
        this.blockchainServer = blockchainServer;
    }

    public BlockchainServer getBlockchainServer() {
        return blockchainServer;
    }
}
