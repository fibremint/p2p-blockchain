/*
package com.fibremint.blockchain.blockchain;

import com.fibremint.blockchain.message.MessageSenderRunnable;
import com.fibremint.blockchain.message.model.MessageProperties;
import com.fibremint.blockchain.net.ServerInfo;
import com.fibremint.blockchain.util.HashUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class BlockMiner implements Runnable {
    private Block newBlock;
    private Wallet wallet;
    private ServerInfo serverInfo;
    private Socket socket;

    public BlockMiner(Wallet wallet, ServerInfo serverInfo) {
        this.wallet = wallet;
        this.serverInfo = serverInfo;

    }

    @Override
    public void run() {
        JsonParser jsonParser = new JsonParser();
        Gson gson = new GsonBuilder().create();
        try {
            socket = new Socket(serverInfo.getHost(), serverInfo.getPort());
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            new Thread(new MessageSenderRunnable(serverInfo, gson.toJson(new MessageProperties()))).run();

            String inputLine = inputReader.readLine();

            MessageProperties message = gson.fromJson(
                    jsonParser.parse(inputLine), MessageProperties.class);

            Block block = new Block(new BlockHeader(message.blockHash));

            Transaction genesisTransaction = new Transaction(wallet.privateKey, wallet.publicKey, message.miningReward);
            // TODO: check mining reward income works
            genesisTransaction.outputs.add(new TransactionOutput(
                    genesisTransaction.recipient,
                    genesisTransaction.value,
                    genesisTransaction.hash));
            block.addTransaction(genesisTransaction);
            block.header.merkleRootHash = HashUtil.getMerkleRoot(block.transactions);
            String target = HashUtil.getDifficultyString(message.difficulty);
            while(!block.header.hash.substring(0, message.difficulty).equals(target)) {
                block.header.nonce++;
                block.header.hash = block.header.calculateHash();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Block mined");
    }
}

*/
