package application.bootstrap.worldpipeline.blockmanager;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.worldpipeline.block.BlockHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class BlockManager extends ManagerPackage {

    /*
     * Owns the block palette by name and ID with a per-ID geometry type table,
     * the block orientation buffer, and BlockPlacementSystem, the single entry
     * point for world block edits.
     */

    // Internal
    private BlockBufferSystem internalBufferSystem;

    // Palette
    private Object2IntOpenHashMap<String> blockName2BlockID;
    private Int2ObjectOpenHashMap<BlockHandle> blockID2BlockHandle;
    private DynamicGeometryType[] blockID2GeometryType;

    // Base \\

    @Override
    protected void create() {

        this.blockName2BlockID = new Object2IntOpenHashMap<>();
        this.blockName2BlockID.defaultReturnValue(-1);

        this.blockID2BlockHandle = new Int2ObjectOpenHashMap<>();
        this.blockID2GeometryType = new DynamicGeometryType[EngineSetting.REGISTRY_SHORT_ID_COUNT];

        this.internalBufferSystem = create(BlockBufferSystem.class);
        create(BlockPlacementSystem.class);

        create(BlockLoader.class);
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
        blockID2GeometryType[blockHandle.getBlockID()] = blockHandle.getGeometry();
    }

    // On-Demand \\

    public void request(String blockName) {
        ((BlockLoader) internalLoader).request(blockName);
    }

    // Accessible \\

    public boolean hasBlock(String blockName) {
        return blockName2BlockID.containsKey(blockName);
    }

    public int getBlockIDFromBlockName(String blockName) {

        if (!blockName2BlockID.containsKey(blockName))
            request(blockName);

        if (!blockName2BlockID.containsKey(blockName))
            throwException("Block \"" + blockName + "\" was not registered after its on-demand load completed — "
                    + "the loaded file must declare a different block name than the one requested. "
                    + "Check for a resource-name/path mismatch between the block directory and its declared name.");

        return blockName2BlockID.getInt(blockName);
    }

    public BlockHandle getBlockHandleFromBlockID(int blockID) {

        BlockHandle handle = blockID2BlockHandle.get(blockID);

        if (handle == null)
            throwException("No handle registered for block ID: " + blockID);

        return handle;
    }

    public DynamicGeometryType getGeometryFromBlockID(short blockID) {

        DynamicGeometryType geometry = blockID2GeometryType[blockID];

        if (geometry == null)
            throwException("No geometry registered for block ID: " + blockID);

        return geometry;
    }

    public BlockHandle getBlockHandleFromBlockName(String blockName) {
        return getBlockHandleFromBlockID(getBlockIDFromBlockName(blockName));
    }
}