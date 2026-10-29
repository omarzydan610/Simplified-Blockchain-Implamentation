// I acknowledge that I am aware of the academic integrity guidelines of this
// course, and that I worked on this assignment independently without any
// unauthorized help with coding or testing.
// - Omar_Zydan

import java.util.ArrayList;
import java.util.Arrays;

public class MaxFeeTxHandler {

    private UTXOPool utxoPool;
    private ArrayList<Transaction> txs; // candidates, parents before children
    private int maxTxs;
    private ArrayList<Transaction> best; // best set found so far
    private double bestFee;

    /** Creates a handler whose UTXOPool is a defensive copy of {@code utxoPool}. */
    public MaxFeeTxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * Returns a mutually valid set of at most {@code maxTxs} transactions from {@code possibleTxs}
     * with the maximum total fee, and updates the UTXO pool. The fee of a transaction is the sum of
     * its input values minus the sum of its output values.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs, int maxTxs) {
        // IMPLEMENT THIS
        txs = new ArrayList<Transaction>();
        for (Transaction tx : possibleTxs)
            addWithParents(tx, possibleTxs);
        this.maxTxs = maxTxs;
        best = new ArrayList<Transaction>();
        bestFee = 0;

        search(0, utxoPool, new ArrayList<Transaction>(), 0);

        for (Transaction tx : best)
            utxoPool = apply(tx, utxoPool);
        return best.toArray(new Transaction[0]);
    }

    /**
     * Decides txs[i]: try including it (if it is valid on {@code pool}) and excluding it.
     * {@code pool} is the pool after the txs in {@code chosen}, whose total fee is {@code fee}.
     */
    private void search(int i, UTXOPool pool, ArrayList<Transaction> chosen, double fee) {
        if (fee > bestFee) {
            bestFee = fee;
            best = new ArrayList<Transaction>(chosen);
        }
        if (i == txs.size() || chosen.size() >= maxTxs)
            return;

        Transaction tx = txs.get(i);
        if (new TxHandler(pool).isValidTx(tx)) { // include
            chosen.add(tx);
            search(i + 1, apply(tx, pool), chosen, fee + fee(tx, pool));
            chosen.remove(chosen.size() - 1);
        }
        search(i + 1, pool, chosen, fee); // exclude
    }

    /** Adds {@code tx} to txs, after its parents (the txs in {@code all} whose outputs it spends). */
    private void addWithParents(Transaction tx, Transaction[] all) {
        if (txs.contains(tx))
            return;
        for (Transaction.Input in : tx.getInputs())
            for (Transaction parent : all)
                if (Arrays.equals(parent.getHash(), in.prevTxHash))
                    addWithParents(parent, all);
        txs.add(tx);
    }

    /** Returns a copy of {@code pool} with {@code tx}'s inputs spent and its outputs added. */
    private UTXOPool apply(Transaction tx, UTXOPool pool) {
        UTXOPool result = new UTXOPool(pool);
        for (Transaction.Input in : tx.getInputs())
            result.removeUTXO(new UTXO(in.prevTxHash, in.outputIndex));
        for (int j = 0; j < tx.numOutputs(); j++)
            result.addUTXO(new UTXO(tx.getHash(), j), tx.getOutput(j));
        return result;
    }

    /** Sum of {@code tx}'s input values (looked up in {@code pool}) minus the sum of its outputs. */
    private double fee(Transaction tx, UTXOPool pool) {
        double fee = 0;
        for (Transaction.Input in : tx.getInputs())
            fee += pool.getTxOutput(new UTXO(in.prevTxHash, in.outputIndex)).value;
        for (Transaction.Output out : tx.getOutputs())
            fee -= out.value;
        return fee;
    }

}
