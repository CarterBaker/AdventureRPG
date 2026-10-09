package application.bootstrap.worldpipeline.blockmanager;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.worldpipeline.block.BlockHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BlockManager extends ManagerPackage {

    /*
     * Owns the block palette by name and ID with a per-ID geometry type table,
     * the block orientation buffer, and BlockPlacementSystem, the single entry
     * point for world block edits. Block IDs are assigned in registration
     * order, so every block name must be unique.
     */

    // Internal
    private BlockBufferSystem internalBufferSystem;

    // Palette
    private Object2IntOpenHashMap<String> blockName2BlockID;
    private ObjectArrayList<BlockHandle> blockID2BlockHandle;
    private DynamicGeometryType[] blockID2GeometryType;

    // Base \\

    @Override
    protected void create() {

        this.blockName2BlockID = RegistryUtility.createNameIndex();
        this.blockID2BlockHandle = RegistryUtility.createPalette();
        this.blockID2GeometryType = new DynamicGeometryType[EngineSetting.REGISTRY_SHORT_ID_COUNT];

        this.internalBufferSystem = create(BlockBufferSystem.class);
        create(BlockPlacementSystem.class);

        create(BlockLoader.class);
    }

    // Management \\

    short registerBlockName(String blockName) {
        return (short) RegistryUtility.registerID(
                blockName2BlockID, blockID2BlockHandle, blockName, EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addBlock(BlockHandle blockHandle) {

        short blockID = blockHandle.getBlockID();

        if (blockID2BlockHandle.get(blockID) != null)
            throwException("Duplicate block name: '" + blockHandle.getBlockName()
                    + "' is declared more than once — every block name must be unique");

        blockID2BlockHandle.set(blockID, blockHandle);
        blockID2GeometryType[blockID] = blockHandle.getGeometry();
    }

    // On-Demand \\

    public void request(String blockName) {
        ((BlockLoader) internalLoader).request(blockName);
    }

    // Accessible \\

    public boolean hasBlock(String blockName) {
        return RegistryUtility.getHandle(blockName2BlockID, blockID2BlockHandle, blockName) != null;
    }

    public int getBlockIDFromBlockName(String blockName) {

        if (!hasBlock(blockName))
            request(blockName);

        if (!hasBlock(blockName))
            throwException("Block \"" + blockName + "\" was not registered after its on-demand load completed — "
                    + "the loaded file must declare a different block name than the one requested. "
                    + "Check for a resource-name/path mismatch between the block directory and its declared name.");

        return blockName2BlockID.getInt(blockName);
    }

    public BlockHandle getBlockHandleFromBlockID(int blockID) {

        BlockHandle handle = RegistryUtility.getHandle(blockID2BlockHandle, blockID);

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