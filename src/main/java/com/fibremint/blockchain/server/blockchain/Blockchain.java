package com.fibremint.blockchain.server.blockchain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Blockchain {
    public ArrayList<Block> blockchain = new ArrayList<>();
    public HashMap<String, TransactionOutput> UTXOs = new HashMap<>();
    public static int difficulty = 5;
    public static float minimumTransaction = 0.1f;
    public static float miningReward = 100f;

    public Block getBlock(String blockHash) {
        return blockchain.stream().filter(block -> blockHash.equals(block.header.hash))
                .findFirst().orElse(null);
    }

    public boolean isBlockchainEmpty() {
        return blockchain.size() == 0;
    }

    public Block getLatestBlock() {
        return (!isBlockchainEmpty()) ? blockchain.get(blockchain.size() - 1) : null;
    }

    public int getLength() {
        return blockchain.size();
    }

    // TODO: validation would be required.
    public synchronized void catchUp(List<Block> catchUpBlocks, HashMap<String, TransactionOutput> catchUpUXTOs) {
        if (!isBlockchainEmpty()) {
            blockchain.remove(blockchain.size()-1);
        }
        blockchain.addAll(catchUpBlocks);
        UTXOs = catchUpUXTOs;
    }

    public boolean addBlock(Block candidateBlock) {
        String hashTarget = new String(new char[Blockchain.difficulty]).replace('\0', '0');

        Block latestBlock = this.getLatestBlock();
        if (latestBlock != null) {
            if (!candidateBlock.header.hash.equals(candidateBlock.header.calculateHash())) {
                System.out.println("#Current Hashes not equal");
                return false;
            }
            //compare previous hash and registered previous hash
            if (!latestBlock.header.hash.equals(candidateBlock.header.previousHash)) {
                System.out.println("#Previous Hashes not equal");
                return false;
            }
            //check if hash is solved
            if (!candidateBlock.header.hash.substring(0, Blockchain.difficulty).equals(hashTarget)) {
                System.out.println("#This block hasn't been mined");
                return false;
            }
        }

        this.blockchain.add(candidateBlock);
        return true;
    }

        public boolean addTransaction(Transaction transaction) {
        if (transaction == null) return false;
        if (!"0".equals(transaction.hash)) {
            if (!transaction.processTransaction(this)) {
                System.out.println("Transaction failed to process. Discarded.");
                return false;
            }
        }
        this.getLatestBlock().transactions.add(transaction);

        System.out.println("Transaction successfully added to block");
        return true;
    }

    public static Boolean isChainValid(Blockchain blockchain) {
        Block currentBlock;
        Block previousBlock;
        String hashTarget = new String(new char[Blockchain.difficulty]).replace('\0', '0');
        HashMap<String,TransactionOutput> tempUTXOs = new HashMap<>(blockchain.UTXOs); //a temporary working list of unspent transactions at a given block state.

        //loop through blockchain to check hashes:
        for(int i=1; i < blockchain.blockchain.size(); i++) {

            currentBlock = blockchain.blockchain.get(i);
            previousBlock = blockchain.blockchain.get(i-1);
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
