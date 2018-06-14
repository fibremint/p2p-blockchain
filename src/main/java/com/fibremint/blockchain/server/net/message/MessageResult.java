package com.fibremint.blockchain.server.net.message;

import com.google.gson.annotations.SerializedName;

public class MessageResult extends MessageBase {
    private Type result;
    private String description;

    public MessageResult(Type result) {
        super(MessageType.result);
        this.result = result;
    }

    public MessageResult(Type result, String description) {
        super(MessageType.result);
        this.result = result;
        this.description = description;
    }

    public enum Type {
        @SerializedName("accepted") accepted,
        @SerializedName("denied") denied,
        @SerializedName("error") error
    }

}
