package com.fibremint.blockchain.server.blockchain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Blockchain {
    public static ArrayList<Block> blockchain = new ArrayList<>();
    public static HashMap<String, TransactionOutput> UTXOs = new HashMap<>();
    public static int difficulty = 5;
    public static float minimumTransaction = 0.1f;
    public static float miningReward = 100f;

    public static Block getBlock(String blockHash) {
        return blockchain.stream().filter(block -> blockHash.equals(block.header.hash))
                .findFirst().orElse(null);
    }

    public static Block getLatestBlock() {
        return (blockchain.size() == 0) ? null : blockchain.get(blockchain.size() - 1);
    }

    public static int getLength() {
        return blockchain.size();
    }

    public static synchronized void catchUp(List<Block> catchUpBlocks, HashMap<String, TransactionOutput> catchUpUXTOs) {
        blockchain.addAll(catchUpBlocks);
        UTXOs = catchUpUXTOs;
        // TODO: validation would be required.
    }

    public static Boolean isChainValid(ArrayList<Block> blockchain) {
        Block currentBlock;
        Block previousBlock;
        String hashTarget = new String(new char[Blockchain.difficulty]).replace('\0', '0');
        HashMap<String,TransactionOutput> tempUTXOs = new HashMap<>(); //a temporary working list of unspent transactions at a given block state.

        //loop through blockchain to check hashes:
        for(int i=1; i < blockchain.size(); i++) {

            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i-1);
            //compare registered hash and calculated hash:
            if(!currentBlock.header.hash.equals(currentBlock.header.calculateHash()) ){
                System.out.println("#Current Hashes not equal");
                return false;
            }
            //compare previous hash and registered previous hash
            if(!previousBlock.header.hash.equals(currentBlock.header.previousHash) ) {
                System.out.println("#Previous Hashes not equal");
                return false;
            }
            //check if hash is solved
            if(!currentBlock.header.hash.substring( 0, Blockchain.difficulty).equals(hashTarget)) {
                System.out.println("#This block hasn't been mined");
                return false;
            }

            //loop thru blockchains transactions:
            TransactionOutput tempOutput;
            for(int t=0; t <currentBlock.transactions.size(); t++) {
                Transaction currentTransaction = currentBlock.transactions.get(t);
                if (!currentTransaction.hash.equals("0")) {
                    if (!currentTransaction.verifySignature()) {
                        System.out.println("#Signature on Transaction(" + t + ") is Invalid");
                        return false;
                    }
                    if (currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
                        System.out.println("#Inputs are not equal to outputs on Transaction(" + t + ")");
                        return false;
                    }

                    for (TransactionInput input : currentTransaction.inputs) {
                        tempOutput = tempUTXOs.get(input.transactionOutputHash);

                        if (tempOutput == null) {
                            System.out.println("#Referenced input on Transaction(" + t + ") is Missing");
                            return false;
                        }

                        if (input.UTXO.value != tempOutput.value) {
                            System.out.println("#Referenced input Transaction(" + t + ") value is Invalid");
                            return false;
                        }

                        tempUTXOs.remove(input.transactionOutputHash);
                    }

                    for (TransactionOutput output : currentTransaction.outputs) {
                        tempUTXOs.put(output.hash, output);
                    }

                    if (currentTransaction.outputs.get(0).recipient != currentTransaction.recipient) {
                        System.out.println("#Transaction(" + t + ") output reciepient is not who it should be");
                        return false;
                    }
                    if (currentTransaction.outputs.get(1).recipient != currentTransaction.sender) {
                        System.out.println("#Transaction(" + t + ") output 'change' is not sender.");
                        return false;
                    }
                }

            }

        }
        System.out.println("Blockchain is valid");
        return true;
    }


}
