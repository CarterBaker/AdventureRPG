package application.bootstrap.worldpipeline.treemanager;

import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;

class TreePlantBranch extends BranchPackage {

    /*
     * Main thread — a seed set into the ground grows into a tree of the
     * species it names, rooted in the block above the one it was set on,
     * planted on this game day and grown from a seed of its own. The new tree
     * is registered for the session and handed to every loaded chunk its
     * species can reach, each holding it once, and the ground around it is
     * redrawn so the sapling shows at once.
     */

    // Internal
    private TreeManager treeManager;
    private TreeRegistryBranch treeRegistryBranch;
    private TreeRebuildBranch treeRebuildBranch;
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
        this.treeManager = get(TreeManager.class);
        this.treeRegistryBranch = get(TreeRegistryBranch.class);
        this.treeRebuildBranch = get(TreeRebuildBranch.class);
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    // Plant \\

    // A tree of the species rooted at a world block column — false when one of its kind already stands there
    boolean plant(TreeHandle treeHandle, WorldHandle worldHandle, long anchorX, long anchorZ, int baseY) {

        long wrappedX = WorldWrapUtility.wrapBlockX(worldHandle, anchorX);
        long wrappedZ = WorldWrapUtility.wrapBlockZ(worldHandle, anchorZ);
        double currentDay = treeManager.getCurrentDay();
        long seed = BiomeFieldUtility.hashCell(
                worldHandle.getSeed() ^ EngineSetting.TREE_PLANTED_SEED_SALT
                        ^ Double.doubleToLongBits(currentDay),
                (int) wrappedX, (int) wrappedZ);

        TreeInstance tree = create(TreeInstance.class);
        tree.constructor(treeHandle, worldHandle, wrappedX, wrappedZ, baseY, seed, currentDay, true, currentDay);

        if (!treeRegistryBranch.plant(tree))
            return false;

        handToChunks(tree);
        treeRebuildBranch.rebuildTree(tree, true);

        return true;
    }

    // Every loaded chunk the species can reach takes the tree into its palette
    private void handToChunks(TreeInstance tree) {

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
                        && chunk.getTreePaletteHandle().add(tree))
                    treeRegistryBranch.hold(tree);
            }
    }
}
