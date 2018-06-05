package com.fibremint.blockchain.message.model;

public class MessageHeartbeat extends MessageBase {
    public int localPort;
    public int sequenceNumber;

    public MessageHeartbeat(int localPort, int sequenceNumber) {
        super(MessageType.heartbeat);
        this.localPort = localPort;
        this.sequenceNumber = sequenceNumber;

    }

    public int getLocalPort() {
        return localPort;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }
}
