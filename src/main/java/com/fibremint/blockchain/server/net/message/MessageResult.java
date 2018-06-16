package com.fibremint.blockchain.server.net.message;

import com.google.gson.annotations.SerializedName;

public class MessageResult extends MessageBase {
    private Type result;
    public MessageType attribute;
    private String description;

    public MessageResult(Type result) {
        super(MessageType.result);
        this.result = result;
    }

    public MessageResult(Type result, MessageType attribute) {
        super(MessageType.result);
        this.result = result;
        this.attribute = attribute;
    }

    public MessageResult(Type result, MessageType attribute, String description) {
        super(MessageType.result);
        this.result = result;
        this.attribute = attribute;
        this.description = description;
    }

    public enum Type {
        @SerializedName("accepted") accepted,
        @SerializedName("denied") denied,
        @SerializedName("error") error
    }

}
