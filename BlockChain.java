// The BlockChain class should maintain only limited block nodes to satisfy the functionality.
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

// I acknowledge that I am aware of the academic integrity guidelines of this
// course, and that I worked on this assignment independently without any
// unauthorized help with coding or testing.
// - Omar_Zydan

/*
 * Additional exercise:
 * Problem: the coinbase hash depends only on its value (25) and the miner's address, so two
 * blocks mined by the same address have identical coinbase transactions with the same hash. The
 * second one overwrites the first one's UTXO (one reward is lost), or brings back an already
 * spent coinbase output.
 * Bitcoin's fix: 
 * BIP30 rejects a transaction whose hash matches an earlier one that still has unspent outputs
 * BIP34 puts the block height in the coinbase input, so every coinbase (and its hash) is unique.
 */

import java.util.ArrayList;
import java.util.HashMap;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;

    /** A block in the tree, plus what the chain computed about it. */
    private class Node {
        Block block;
        Node parent; // null for genesis, or once the parent has been pruned
        int height; // genesis is at height 1
        UTXOPool utxoPool; // after this block

        Node(Block block, Node parent, int height, UTXOPool utxoPool) {
            this.block = block;
            this.parent = parent;
            this.height = height;
            this.utxoPool = utxoPool;
        }
    }

    private HashMap<ByteArrayWrapper, Node> nodes; // block hash -> node
    private Node maxNode; // tip of the main chain
    private TransactionPool txPool; // pending transactions

    /**
     * create an empty blockchain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        // IMPLEMENT THIS
        UTXOPool pool = new UTXOPool();
        addCoinbase(genesisBlock, pool);
        Node genesis = new Node(genesisBlock, null, 1, pool);

        nodes = new HashMap<ByteArrayWrapper, Node>();
        nodes.put(new ByteArrayWrapper(genesisBlock.getHash()), genesis);
        maxNode = genesis;
        txPool = new TransactionPool();
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        // IMPLEMENT THIS
        return maxNode.block;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        // IMPLEMENT THIS
        return new UTXOPool(maxNode.utxoPool);
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        // IMPLEMENT THIS
        return txPool;
    }

    /**
     * Add {@code block} to the blockchain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}, where maxHeight is 
     * the current height of the blockchain.
	 * <p>
	 * Assume the Genesis block is at height 1.
     * For example, you can try creating a new block over the genesis block (i.e. create a block at 
	 * height 2) if the current blockchain height is less than or equal to CUT_OFF_AGE + 1. As soon as
	 * the current blockchain height exceeds CUT_OFF_AGE + 1, you cannot create a new block at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        // IMPLEMENT THIS
        // reject invalid blocks
        if (block == null || block.getHash() == null)
            return false;
        if (block.getPrevBlockHash() == null) // a new genesis block is never accepted
            return false;
        if (nodes.containsKey(new ByteArrayWrapper(block.getHash()))) // already added
            return false;
        Node parent = nodes.get(new ByteArrayWrapper(block.getPrevBlockHash()));
        if (parent == null) // unknown or pruned parent
            return false;
        int height = parent.height + 1;
        if (height <= maxNode.height - CUT_OFF_AGE) // too old to fork from
            return false;

        TxHandler handler = new TxHandler(parent.utxoPool);
        Transaction[] txs = block.getTransactions().toArray(new Transaction[0]);
        if (handler.handleTxs(txs).length != txs.length) // every tx must be valid
            return false;

        // store the block with the pool after adding coinbase to its txs 
        UTXOPool pool = handler.getUTXOPool();
        addCoinbase(block, pool);
        Node node = new Node(block, parent, height, pool);
        nodes.put(new ByteArrayWrapper(block.getHash()), node);

        Node oldMax = maxNode;
        if (height > maxNode.height) // strictly higher, so ties keep the oldest block
            maxNode = node;

        // keep the transaction pool in sync with the main chain
        if (parent == oldMax)
            removeTxs(block); // extends the main chain so remove its txs from the pending pool to not be mined again
        else if (maxNode == node)
            reorg(oldMax, node); // a side branch became the main chain

        prune();
        return true;
    }

    /**
     * Moves the main chain from {@code oldTip} to {@code newTip}: txs of the abandoned blocks go
     * back to the pool, then txs of the new main blocks are removed from it.
     */
    private void reorg(Node oldTip, Node newTip) {
        ArrayList<Block> oldBranch = new ArrayList<Block>();
        ArrayList<Block> newBranch = new ArrayList<Block>();
        Node a = oldTip, b = newTip;
        while (a != null && b != null && a != b) {
            if (a.height >= b.height) {
                oldBranch.add(a.block);
                a = a.parent;
            } else {
                newBranch.add(b.block);
                b = b.parent;
            }
        }
        for (Block blk : oldBranch)
            for (Transaction tx : blk.getTransactions())
                txPool.addTransaction(tx);
        for (Block blk : newBranch)
            removeTxs(blk);
    }

    /** Removes every transaction of {@code block} from the transaction pool. */
    private void removeTxs(Block block) {
        for (Transaction tx : block.getTransactions())
            txPool.removeTransaction(tx.getHash());
    }

    /** Drops nodes that can never get a new child, so memory stays bounded. */
    private void prune() {
        int limit = maxNode.height - CUT_OFF_AGE;
        nodes.values().removeIf(n -> n.height < limit);
        for (Node n : nodes.values())
            if (n.parent != null && n.parent.height < limit)
                n.parent = null;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        // IMPLEMENT THIS
        txPool.addTransaction(tx);
    }

    /** Adds the coinbase output of {@code block} to {@code pool}. */
    private void addCoinbase(Block block, UTXOPool pool) {
        Transaction coinbase = block.getCoinbase();
        for (int i = 0; i < coinbase.numOutputs(); i++)
            pool.addUTXO(new UTXO(coinbase.getHash(), i), coinbase.getOutput(i));
    }
}