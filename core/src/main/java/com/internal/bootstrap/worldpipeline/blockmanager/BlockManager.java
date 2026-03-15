package com.internal.bootstrap.worldpipeline.blockmanager;

import com.internal.bootstrap.worldpipeline.block.BlockHandle;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.util.RegistryUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class BlockManager extends ManagerPackage {

    // Internal
    private InternalBufferSystem internalBufferSystem;

    // Palette
    private Object2IntOpenHashMap<String> blockName2BlockID;
    private Int2ObjectOpenHashMap<BlockHandle> blockID2BlockHandle;

    // Base \\

    @Override
    protected void create() {

        this.blockName2BlockID = new Object2IntOpenHashMap<>();
        this.blockID2BlockHandle = new Int2ObjectOpenHashMap<>();

        this.internalBufferSystem = create(InternalBufferSystem.class);

        create(InternalLoader.class);
    }

    // Management \\

    void addBlock(BlockHandle blockHandle) {

        if (blockID2BlockHandle.containsKey(blockHandle.getBlockID())) {
            BlockHandle existing = blockID2BlockHandle.get(blockHandle.getBlockID());
            if (RegistryUtility.isCollision(blockHandle.getBlockName(), existing.getBlockName(),
                    blockHandle.getBlockID()))
                throwException("Block ID collision: '"
                        + blockHandle.getBlockName() + "' collides with '"
                        + existing.getBlockName() + "' (ID " + blockHandle.getBlockID()
                        + ") — rename one block to resolve");
        }

        blockName2BlockID.put(blockHandle.getBlockName(), blockHandle.getBlockID());
        blockID2BlockHandle.put(blockHandle.getBlockID(), blockHandle);
    }

    // On-Demand \\

    public void request(String blockName) {
        ((InternalLoader) internalLoader).request(blockName);
    }

    // Accessible \\

    public boolean hasBlock(String blockName) {
        return blockName2BlockID.containsKey(blockName);
    }

    public int getBlockIDFromBlockName(String blockName) {

        if (!blockName2BlockID.containsKey(blockName))
            request(blockName);

        return blockName2BlockID.getInt(blockName);
    }

    public BlockHandle getBlockHandleFromBlockID(int blockID) {

        BlockHandle handle = blockID2BlockHandle.get(blockID);

        if (handle == null)
            throwException("No handle registered for block ID: " + blockID);

        return handle;
    }

    public BlockHandle getBlockHandleFromBlockName(String blockName) {
        return getBlockHandleFromBlockID(getBlockIDFromBlockName(blockName));
    }
}