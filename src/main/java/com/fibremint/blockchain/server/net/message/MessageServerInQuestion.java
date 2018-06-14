package com.fibremint.blockchain.server.net.message;

public class MessageServerInQuestion extends MessageBase {
    private int localPort;
    private String remoteHost;
    private int remotePort;

    public MessageServerInQuestion(int localPort, String remoteHost, int remotePort) {
        super(MessageType.serverInQuestion);
        this.localPort = localPort;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;

    }

    public int getLocalPort() {
        return localPort;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public int getRemotePort() {
        return remotePort;
    }

}
