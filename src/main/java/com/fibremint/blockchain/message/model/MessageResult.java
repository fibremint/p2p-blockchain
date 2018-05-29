package com.fibremint.blockchain.message.model;

import com.google.gson.annotations.SerializedName;

public class MessageResult extends MessageBase {
    private Type result;

    public MessageResult(Type result) {
        super(MessageType.result);
        this.result = result;
    }

    public enum Type {
        @SerializedName("accepted") accepted,
        @SerializedName("denied") denied
    }

}
