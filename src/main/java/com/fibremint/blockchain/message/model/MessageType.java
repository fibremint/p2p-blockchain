package com.fibremint.blockchain.message.model;

import com.google.gson.annotations.SerializedName;

public enum MessageType {
    @SerializedName("catchUp") catchUp,
    @SerializedName("lastBlock") lastBlock,
    @SerializedName("printBlock") printBlock,
    @SerializedName("result") result,
    @SerializedName("serverInQuestion") serverInQuestion,
    @SerializedName("transaction") transaction,
    @SerializedName("heartbeat") heartbeat
}
