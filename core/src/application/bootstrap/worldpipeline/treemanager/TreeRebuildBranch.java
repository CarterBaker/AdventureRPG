package application.bootstrap.worldpipeline.treemanager;

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
     * Main thread — redraws a tree that changed, and only the tree: every
     * loaded chunk holding it draws its trees again, and the mega its root
     * stands in rebuilds its stand-ins, while the ground around it is never
     * touched. A change the player made — a notch, a cut, a sapling — shows
     * at once; one the world made, a tree growing or its species edited live,
     * goes through the stream, the old tree standing until the new one lands.
     * Every redraw goes through WorldStreamManager.
     */

    // Internal
    private WorldStreamManager worldStreamManager;

    // Settings
    private int chunkSize;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
    }

    @Override
    protected void get() {
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    // Rebuild \\

    void rebuildTree(TreeInstance tree, boolean immediate) {

        WorldHandle worldHandle = tree.getWorldHandle();
        int reach = (int) Math.ceil(tree.getTreeHandle().getReachBlocks());
        int firstX = (int) Math.floorDiv(tree.getAnchorX() - reach, chunkSize);
        int firstZ = (int) Math.floorDiv(tree.getAnchorZ() - reach, chunkSize);
        int lastX = (int) Math.floorDiv(tree.getAnchorX() + reach, chunkSize);
        int lastZ = (int) Math.floorDiv(tree.getAnchorZ() + reach, chunkSize);

        for (int chunkZ = firstZ; chunkZ <= lastZ; chunkZ++)
            for (int chunkX = firstX; chunkX <= lastX; chunkX++) {

                ChunkInstance chunk = worldStreamManager.getChunkInstance(
                        WorldWrapUtility.wrapAroundWorld(worldHandle, Coordinate2Long.pack(chunkX, chunkZ)));

                if (chunk != null && chunk.getWorldHandle() == worldHandle
                        && chunk.getTreePaletteHandle().contains(tree))
                    worldStreamManager.refreshChunkTrees(chunk, immediate);
            }

        worldStreamManager.refreshMegaTrees(tree.getRootChunkCoordinate());
    }
}
