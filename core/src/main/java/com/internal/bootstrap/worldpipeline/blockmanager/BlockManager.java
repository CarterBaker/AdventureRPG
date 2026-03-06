package com.internal.bootstrap.worldpipeline.blockmanager;

import com.internal.bootstrap.worldpipeline.block.BlockHandle;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.util.RegistryUtility;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class BlockManager extends ManagerPackage {

    // Internal
    private InternalBufferSystem internalBufferSystem;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> blockName2BlockID;
    private Int2ObjectOpenHashMap<BlockHandle> blockID2Block;

    // Base \\

    @Override
    protected void create() {
        create(InternalLoader.class);
        this.internalBufferSystem = create(InternalBufferSystem.class);
        this.blockName2BlockID = new Object2IntOpenHashMap<>();
        this.blockID2Block = new Int2ObjectOpenHashMap<>();
    }

    // On-Demand Loading \\

    public void request(String blockName) {
        ((InternalLoader) internalLoader).request(blockName);
    }

    // Block Management \\

    void addBlock(BlockHandle block) {
        if (blockID2Block.containsKey(block.getBlockID())) {
            BlockHandle existing = blockID2Block.get(block.getBlockID());
            if (RegistryUtility.isCollision(block.getBlockName(), existing.getBlockName(), block.getBlockID()))
                throwException("Block ID collision: '"
                        + block.getBlockName() + "' collides with '"
                        + existing.getBlockName() + "' (ID " + block.getBlockID() + ") — rename one block to resolve");
        }
        blockName2BlockID.put(block.getBlockName(), block.getBlockID());
        blockID2Block.put(block.getBlockID(), block);
    }

    // Accessible \\

    public int getBlockIDFromBlockName(String blockName) {
        if (!blockName2BlockID.containsKey(blockName))
            request(blockName);
        return blockName2BlockID.getInt(blockName);
    }

    public BlockHandle getBlockFromBlockID(int blockID) {
        BlockHandle block = blockID2Block.get(blockID);
        if (block == null)
            throwException("Block ID not found: " + blockID);
        return block;
    }
}