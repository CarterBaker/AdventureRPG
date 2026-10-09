package application.bootstrap.worldpipeline.treemanager;

import application.bootstrap.worldpipeline.blockmanager.BlockPlacementSystem;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.tree.TreeShapeStruct;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;

class TreeRebuildBranch extends BranchPackage {

    /*
     * Main thread — redraws the stretch of the world a change to a tree
     * touched: every loaded subchunk a box of world blocks reaches, one block
     * wider all round, since a subchunk lays the wood around it to mesh its
     * borders. A tree that changed shape redraws everything either its old or
     * its new shape reaches. Every redraw goes through BlockPlacementSystem.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockPlacementSystem blockPlacementSystem;

    // Settings
    private int chunkSize;
    private int margin;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.margin = EngineSetting.TREE_GEOMETRY_MARGIN_BLOCKS;
    }

    @Override
    protected void get() {
        this.worldStreamManager = get(WorldStreamManager.class);
        this.blockPlacementSystem = get(BlockPlacementSystem.class);
    }

    // Rebuild \\

    // Everything either shape of a tree reaches
    void rebuildTree(TreeInstance tree, TreeShapeStruct before, TreeShapeStruct after) {

        float rootX = tree.getAnchorX() + EngineSetting.TREE_ROOT_CENTER_BLOCKS;
        float rootY = tree.getBaseY();
        float rootZ = tree.getAnchorZ() + EngineSetting.TREE_ROOT_CENTER_BLOCKS;

        rebuild(
                tree.getWorldHandle(),
                (long) Math.floor(rootX + Math.min(before.getMinX(), after.getMinX())),
                (int) Math.floor(rootY + Math.min(before.getMinY(), after.getMinY())),
                (long) Math.floor(rootZ + Math.min(before.getMinZ(), after.getMinZ())),
                (long) Math.floor(rootX + Math.max(before.getMaxX(), after.getMaxX())),
                (int) Math.floor(rootY + Math.max(before.getMaxY(), after.getMaxY())),
                (long) Math.floor(rootZ + Math.max(before.getMaxZ(), after.getMaxZ())));
    }

    // Every loaded subchunk a box of world blocks reaches, inclusive, with the mesher's margin around it
    void rebuild(WorldHandle worldHandle, long minX, int minY, long minZ, long maxX, int maxY, long maxZ) {

        int firstChunkX = (int) Math.floorDiv(minX - margin, chunkSize);
        int firstChunkZ = (int) Math.floorDiv(minZ - margin, chunkSize);
        int lastChunkX = (int) Math.floorDiv(maxX + margin, chunkSize);
        int lastChunkZ = (int) Math.floorDiv(maxZ + margin, chunkSize);
        int firstSubChunkY = Math.floorDiv(minY - margin, chunkSize);
        int lastSubChunkY = Math.floorDiv(maxY + margin, chunkSize);

        for (int chunkZ = firstChunkZ; chunkZ <= lastChunkZ; chunkZ++)
            for (int chunkX = firstChunkX; chunkX <= lastChunkX; chunkX++) {

                ChunkInstance chunk = worldStreamManager.getChunkInstance(
                        WorldWrapUtility.wrapAroundWorld(worldHandle, Coordinate2Long.pack(chunkX, chunkZ)));

                if (chunk != null && chunk.getWorldHandle() == worldHandle)
                    blockPlacementSystem.rebuildSubChunks(chunk, firstSubChunkY, lastSubChunkY);
            }
    }
}
