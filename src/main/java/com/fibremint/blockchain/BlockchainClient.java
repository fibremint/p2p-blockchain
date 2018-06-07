package com.fibremint.blockchain;

import com.fibremint.blockchain.blockchain.*;
import com.fibremint.blockchain.net.ServerInfo;
import org.bouncycastle.asn1.eac.ECDSAPublicKey;

import java.security.Security;

public class BlockchainClient {

    public static void main(String[] args) {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        if (args.length != 2) {
            return;
        }

        int remotePort = 0;
        String remoteHost = null;

        try {
            remoteHost = args[0];
            remotePort = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            return;
        }

        try {
            ServerInfo serv = new ServerInfo(remoteHost, remotePort);
            /*Scanner sc = new Scanner(System.in);
            while (true) {
                String message = sc.nextLine();
                new Thread(new MessageSenderRunnable(serv, message)).start();
            }*/
            Wallet wallet = new Wallet();
            Thread miner = new Thread(new BlockMiner(wallet, serv));
            miner.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


/*
        Transaction genesisTransaction = new Transaction(coinbase.publicKey, 100f);
        genesisTransaction.generateSignature(coinbase.privateKey);
        genesisTransaction.hash = "0";
        genesisTransaction.outputs.add(new TransactionOutput(
                genesisTransaction.recipient,
                genesisTransaction.value,
                genesisTransaction.hash));
        Blockchain.UTXOs.put(genesisTransaction.outputs.get(0).hash, genesisTransaction.outputs.get(0));
        Block genesis = new Block(new BlockHeader("0"));
        genesis.mineBlock(Blockchain.difficulty);
        Blockchain.blockchain.add(genesis);
*/


}
