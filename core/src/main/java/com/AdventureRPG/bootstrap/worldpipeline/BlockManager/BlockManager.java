package com.AdventureRPG.bootstrap.worldpipeline.BlockManager;

import com.AdventureRPG.bootstrap.worldpipeline.block.BlockHandle;
import com.AdventureRPG.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class BlockManager extends ManagerPackage {

    // Internal
    private InternalLoadManager internalLoadManager;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> blockName2BlockID;
    private Int2ObjectOpenHashMap<BlockHandle> blockID2Block;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalLoadManager = create(InternalLoadManager.class);

        // Retrieval Mapping
        this.blockName2BlockID = new Object2IntOpenHashMap<>();
        this.blockID2Block = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void awake() {
        compileBlocks();
    }

    @Override
    protected void release() {
        internalLoadManager = release(InternalLoadManager.class);
    }

    // Block Management \\

    private void compileBlocks() {
        internalLoadManager.loadBlocks();
    }

    void addBlock(BlockHandle block) {
        blockName2BlockID.put(block.getBlockName(), block.getBlockID());
        blockID2Block.put(block.getBlockID(), block);
    }

    // Accessible \\

    public int getBlockIDFromBlockName(String blockName) {

        if (!blockName2BlockID.containsKey(blockName))
            throwException("Block not found: " + blockName);

        return blockName2BlockID.getInt(blockName);
    }

    public BlockHandle getBlockFromBlockID(int blockID) {

        BlockHandle block = blockID2Block.get(blockID);

        if (block == null)
            throwException("Block ID not found: " + blockID);

        return block;
    }
}