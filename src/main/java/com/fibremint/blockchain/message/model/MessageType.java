package com.fibremint.blockchain.message.model;

import com.google.gson.annotations.SerializedName;

public enum MessageType {
    @SerializedName("catchUp") catchUp,
    @SerializedName("heartbeat") heartbeat,
    @SerializedName("latestBlock") latestBlock,
    @SerializedName("mine") mineBlock,
    @SerializedName("properties") properties,
    @SerializedName("result") result,
    @SerializedName("sendFund") sendFund,
    @SerializedName("serverInQuestion") serverInQuestion,
    @SerializedName("transaction") transaction
}
