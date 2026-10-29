// I acknowledge that I am aware of the academic integrity guidelines of this
// course, and that I worked on this assignment independently without any
// unauthorized help with coding or testing.
// - Omar_Zydan

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. 
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /** @return the current UTXO pool (after any accepted transactions were applied) */
    public UTXOPool getUTXOPool() {
        return utxoPool;
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        Set<UTXO> claimed = new HashSet<UTXO>();
        double inputSum = 0;

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input in = tx.getInput(i);
            if (in.prevTxHash == null)
                return false;
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);

            if (!utxoPool.contains(utxo)) // (1)
                return false;
            if (!claimed.add(utxo)) // (3)
                return false;

            Transaction.Output spent = utxoPool.getTxOutput(utxo);
            if (!Crypto.verifySignature(spent.address, tx.getRawDataToSign(i), in.signature)) // (2)
                return false;

            inputSum += spent.value;
        }

        double outputSum = 0;
        for (Transaction.Output out : tx.getOutputs()) {
            if (out.value < 0) // (4)
                return false;
            outputSum += out.value;
        }
        if(inputSum < outputSum) // (5)
            return false;
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        ArrayList<Transaction> accepted = new ArrayList<Transaction>();
        ArrayList<Transaction> remaining = new ArrayList<Transaction>();
        for (Transaction tx : possibleTxs)
            remaining.add(tx);

        // passing over the txs until no more can be accepted.
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Transaction tx : new ArrayList<Transaction>(remaining)) {
                if (isValidTx(tx)) {
                    applyTx(tx);
                    accepted.add(tx);
                    remaining.remove(tx);
                    changed = true;
                }
            }
        }

        return accepted.toArray(new Transaction[0]);
    }

    /** Removes the UTXOs spent by {@code tx} and adds its outputs as new UTXOs. */
    private void applyTx(Transaction tx) {
        for (Transaction.Input in : tx.getInputs())
            utxoPool.removeUTXO(new UTXO(in.prevTxHash, in.outputIndex));
        for (int j = 0; j < tx.numOutputs(); j++)
            utxoPool.addUTXO(new UTXO(tx.getHash(), j), tx.getOutput(j));
    }

}
