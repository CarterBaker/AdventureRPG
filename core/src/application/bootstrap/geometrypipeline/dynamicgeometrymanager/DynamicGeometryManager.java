package application.bootstrap.geometrypipeline.dynamicgeometrymanager;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.coveringmanager.CoveringManager;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;

public class DynamicGeometryManager extends ManagerPackage {

    /*
     * Entry point for dynamic geometry assembly. Delegates chunk, subchunk and
     * chunk tree builds to GeometryBuildManager, exposes font glyph assembly
     * to the menu pipeline, and owns the shared async scratch used by
     * synchronous builds. The sources it meshes from — blocks, biomes,
     * coverings and trees — bind themselves in as they awake and the build
     * branches take them up at start, so geometry never asks for the world
     * and an engine without one, the launcher, simply never builds.
     */

    // Internal
    private DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer;
    private GeometryBuildManager internalBuildManager;

    // Settings
    private int worldHeight;

    // Sources — bound by each source as it awakes
    private BlockManager blockManager;
    private BlockHandle airBlockHandle;
    private BiomeManager biomeManager;
    private CoveringManager coveringManager;
    private TreeManager treeManager;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.dynamicGeometryAsyncContainer = create(DynamicGeometryAsyncContainer.class);
        this.internalBuildManager = create(GeometryBuildManager.class);

        // Settings
        this.worldHeight = EngineSetting.WORLD_HEIGHT;
    }

    // Sources \\

    public void bindBlockManager(BlockManager blockManager) {
        this.blockManager = blockManager;
        this.airBlockHandle = blockManager.getBlockHandleFromBlockName(EngineSetting.AIR_BLOCK_NAME);
    }

    public void bindBiomeManager(BiomeManager biomeManager) {
        this.biomeManager = biomeManager;
    }

    public void bindCoveringManager(CoveringManager coveringManager) {
        this.coveringManager = coveringManager;
    }

    public void bindTreeManager(TreeManager treeManager) {
        this.treeManager = treeManager;
    }

    BlockManager getBlockManager() {
        return blockManager;
    }

    BlockHandle getAirBlockHandle() {
        return airBlockHandle;
    }

    BiomeManager getBiomeManager() {
        return biomeManager;
    }

    CoveringManager getCoveringManager() {
        return coveringManager;
    }

    TreeManager getTreeManager() {
        return treeManager;
    }

    // Chunk Geometry \\

    public boolean build(
            DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer,
            ChunkInstance chunkInstance) {

        boolean success = true;
        SubChunkInstance[] subChunks = chunkInstance.getSubChunks();

        for (int i = 0; i < worldHeight; i++) {
            if (!internalBuildManager.build(dynamicGeometryAsyncContainer, chunkInstance, subChunks[i]))
                success = false;
        }

        return success;
    }

    public boolean buildSubChunk(
            DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer,
            ChunkInstance chunkInstance,
            int subChunkIndex) {

        return internalBuildManager.build(
                dynamicGeometryAsyncContainer,
                chunkInstance,
                chunkInstance.getSubChunks()[subChunkIndex]);
    }

    // Tree Geometry \\

    // Callers hold the chunk's own lock — trees read nothing of any neighbor, so no other lock is needed
    public boolean buildTrees(ChunkInstance chunkInstance) {
        return internalBuildManager.buildTrees(chunkInstance);
    }

    // Accessible \\

    public DynamicGeometryAsyncContainer getDynamicGeometryAsyncInstance() {
        return dynamicGeometryAsyncContainer;
    }
}