package com.fibremint.blockchain.server.net.message;

import com.google.gson.annotations.SerializedName;

public enum MessageType {
    @SerializedName("catchUp") catchUp,
    @SerializedName("heartbeat") heartbeat,
    @SerializedName("latestBlock") latestBlock,
    @SerializedName("mineBlock") mineBlock,
    @SerializedName("properties") properties,
    @SerializedName("result") result,
    @SerializedName("walletBalance") walletBalance,
    @SerializedName("sendFund") sendFund,
    @SerializedName("serverInQuestion") serverInQuestion,
    @SerializedName("transaction") transaction,
}
