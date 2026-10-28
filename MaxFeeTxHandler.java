public class MaxFeeTxHandler {

    private UTXOPool utxoPool;

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
    }

}
