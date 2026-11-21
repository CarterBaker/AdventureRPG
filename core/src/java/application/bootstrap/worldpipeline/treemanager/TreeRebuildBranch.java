package application.bootstrap.worldpipeline.treemanager;

import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;

class TreeRebuildBranch extends BranchPackage {

    /*
     * Redraws a tree that changed, and only the tree: every loaded chunk
     * holding it whose columns the change reaches draws its trees again, and
     * the mega its root stands in rebuilds its stand-ins, while the ground
     * around it is never touched. A notch or a leaf knocked off redraws only
     * the chunks about it, never the whole of a great tree's reach. A change
     * that must show at once is drawn on the main thread; every other one
     * goes through the stream, the old tree standing until the new one lands,
     * and isTreeRedrawn() says when every chunk holding it has drawn it again.
     * Every redraw goes through WorldStreamManager.
     */

    // Internal
    private WorldStreamManager worldStreamManager;

    // Settings
    private int chunkSize;
    private int treeRenderDataIndex;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.treeRenderDataIndex = ChunkData.TREE_RENDER_DATA.index;
    }

    @Override
    protected void get() {
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    // Rebuild \\

    // The whole tree, over every chunk it reaches
    void rebuildTree(TreeInstance tree, boolean immediate) {

        float reach = tree.getTreeHandle().getReachBlocks();

        rebuildAround(tree, -reach, -reach, reach, reach, immediate);
    }

    // Only the chunks holding the tree whose columns meet a box about the centre of its root, in blocks, widened by
    // the margin a chunk's tree mesh reads beyond its own columns
    void rebuildAround(TreeInstance tree, float minX, float minZ, float maxX, float maxZ, boolean immediate) {

        float margin = EngineSetting.TREE_GEOMETRY_MARGIN_BLOCKS;
        float centerX = tree.getAnchorX() + EngineSetting.TREE_ROOT_CENTER_BLOCKS;
        float centerZ = tree.getAnchorZ() + EngineSetting.TREE_ROOT_CENTER_BLOCKS;
        int firstX = Math.floorDiv((int) Math.floor(centerX + minX - margin), chunkSize);
        int firstZ = Math.floorDiv((int) Math.floor(centerZ + minZ - margin), chunkSize);
        int lastX = Math.floorDiv((int) Math.floor(centerX + maxX + margin), chunkSize);
        int lastZ = Math.floorDiv((int) Math.floor(centerZ + maxZ + margin), chunkSize);

        for (int chunkZ = firstZ; chunkZ <= lastZ; chunkZ++)
            for (int chunkX = firstX; chunkX <= lastX; chunkX++) {

                ChunkInstance chunk = resolveChunk(tree, chunkX, chunkZ);

                if (chunk != null)
                    worldStreamManager.refreshChunkTrees(chunk, immediate);
            }

        worldStreamManager.refreshMegaTrees(tree.getRootChunkCoordinate());
    }

    // True once every loaded chunk holding the tree has its trees drawn again and on the GPU
    boolean isTreeRedrawn(TreeInstance tree) {

        int reach = (int) Math.ceil(tree.getTreeHandle().getReachBlocks());
        int firstX = (int) Math.floorDiv(tree.getAnchorX() - reach, chunkSize);
        int firstZ = (int) Math.floorDiv(tree.getAnchorZ() - reach, chunkSize);
        int lastX = (int) Math.floorDiv(tree.getAnchorX() + reach, chunkSize);
        int lastZ = (int) Math.floorDiv(tree.getAnchorZ() + reach, chunkSize);

        for (int chunkZ = firstZ; chunkZ <= lastZ; chunkZ++)
            for (int chunkX = firstX; chunkX <= lastX; chunkX++) {

                ChunkInstance chunk = resolveChunk(tree, chunkX, chunkZ);

                if (chunk != null && !chunk.getChunkDataSyncContainer().getData()[treeRenderDataIndex])
                    return false;
            }

        return true;
    }

    // The loaded chunk at a chunk position, when it holds the tree
    private ChunkInstance resolveChunk(TreeInstance tree, int chunkX, int chunkZ) {

        WorldHandle worldHandle = tree.getWorldHandle();
        ChunkInstance chunk = worldStreamManager.getChunkInstance(
                WorldWrapUtility.wrapAroundWorld(worldHandle, Coordinate2Long.pack(chunkX, chunkZ)));

        return chunk != null && chunk.getWorldHandle() == worldHandle && chunk.getTreePaletteHandle().contains(tree)
                ? chunk
                : null;
    }
}
