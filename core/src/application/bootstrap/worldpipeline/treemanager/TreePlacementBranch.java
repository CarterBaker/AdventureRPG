package application.bootstrap.worldpipeline.treemanager;

import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.tree.TreeSiteStruct;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;

class TreePlacementBranch extends BranchPackage {

    /*
     * Async — finds every tree that reaches one chunk, on that chunk's own
     * worker thread. Where the world roots its wild trees, and which of them
     * stand once each keeps its room, is TreeSiteBranch's answer for every
     * root within the widest reach of the chunk; here only the trees that
     * truly reach it are kept, and of those only the ones on dry ground. The
     * registry then hands every chunk a tree reaches the same instance, grown
     * from the site's seed to the site's age. Every tree a hand planted that
     * reaches the chunk is held for it too.
     */

    // Internal
    private TreeManager treeManager;
    private TreeRegistryBranch treeRegistryBranch;
    private TreeSiteBranch treeSiteBranch;
    private WorldGenerationManager worldGenerationManager;
    private TreePlacementAsyncContainer placementContainer;

    // Settings
    private int chunkSize;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.placementContainer = create(TreePlacementAsyncContainer.class);

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
    }

    @Override
    protected void get() {
        this.treeManager = get(TreeManager.class);
        this.treeRegistryBranch = get(TreeRegistryBranch.class);
        this.treeSiteBranch = get(TreeSiteBranch.class);
        this.worldGenerationManager = get(WorldGenerationManager.class);
    }

    // Placement \\

    // Every tree reaching the chunk, each held once more by the registry for it
    TreeInstance[] placeTrees(WorldHandle worldHandle, long chunkCoordinate, double currentDay) {

        TreePlacementAsyncContainer scratch = placementContainer.getInstance();

        if (scratch.worldHandle != worldHandle)
            scratch.anchor2Ground.clear();

        scratch.reset();
        scratch.worldHandle = worldHandle;
        scratch.chunkOriginX = (long) Coordinate2Long.unpackX(chunkCoordinate) * chunkSize;
        scratch.chunkOriginZ = (long) Coordinate2Long.unpackY(chunkCoordinate) * chunkSize;
        scratch.currentDay = currentDay;

        int reach = (int) Math.ceil(treeManager.getMaxReachBlocks());

        treeSiteBranch.collectSites(
                worldHandle,
                scratch.chunkOriginX - reach,
                scratch.chunkOriginZ - reach,
                scratch.chunkOriginX + chunkSize - 1 + reach,
                scratch.chunkOriginZ + chunkSize - 1 + reach,
                scratch.sites);

        for (int i = 0; i < scratch.sites.size(); i++)
            placeTree(scratch, scratch.sites.get(i));

        placePlanted(scratch);

        return scratch.trees.toArray(new TreeInstance[0]);
    }

    // Tree \\

    private void placeTree(TreePlacementAsyncContainer scratch, TreeSiteStruct site) {

        if (!reachesChunk(scratch, site.getTreeHandle(), site.getAnchorX(), site.getAnchorZ()))
            return;

        int ground = probeGround(scratch, site.getAnchorX(), site.getAnchorZ());

        if (ground == EngineSetting.TREE_GROUND_FLOODED)
            return;

        TreeInstance tree = treeRegistryBranch.acquire(site.getRegistryKey());

        if (tree == null)
            tree = treeRegistryBranch.register(growTree(scratch, site, ground + 1));

        scratch.trees.add(tree);
    }

    private boolean reachesChunk(
            TreePlacementAsyncContainer scratch,
            TreeHandle treeHandle,
            long anchorX,
            long anchorZ) {

        int reach = (int) Math.ceil(treeHandle.getReachBlocks());
        long relativeX = WorldWrapUtility.wrappedBlockDeltaX(scratch.worldHandle, anchorX, scratch.chunkOriginX);
        long relativeZ = WorldWrapUtility.wrappedBlockDeltaZ(scratch.worldHandle, anchorZ, scratch.chunkOriginZ);

        return relativeX + reach >= 0 && relativeX - reach < chunkSize
                && relativeZ + reach >= 0 && relativeZ - reach < chunkSize;
    }

    private boolean holds(TreePlacementAsyncContainer scratch, long key) {

        for (int i = 0; i < scratch.trees.size(); i++)
            if (scratch.trees.get(i).getRegistryKey() == key)
                return true;

        return false;
    }

    // A wild tree grown from its site's seed, planted as long ago as the site's age takes to grow
    private TreeInstance growTree(TreePlacementAsyncContainer scratch, TreeSiteStruct site, int baseY) {

        TreeHandle treeHandle = site.getTreeHandle();
        double plantedDay = scratch.currentDay - site.getAge() * treeHandle.getGrowth().getDays();

        TreeInstance tree = create(TreeInstance.class);
        tree.constructor(treeHandle, scratch.worldHandle, site.getAnchorX(), site.getAnchorZ(), baseY,
                site.getSeed(), plantedDay, false, scratch.currentDay);

        return tree;
    }

    // Planted \\

    // Every tree a hand planted in this world that can reach the chunk, held once more for it
    private void placePlanted(TreePlacementAsyncContainer scratch) {

        treeRegistryBranch.collectPlanted(scratch.planted);

        for (int i = 0; i < scratch.planted.size(); i++) {

            TreeInstance tree = scratch.planted.get(i);

            if (tree.getWorldHandle() != scratch.worldHandle
                    || !reachesChunk(scratch, tree.getTreeHandle(), tree.getAnchorX(), tree.getAnchorZ())
                    || holds(scratch, tree.getRegistryKey()))
                continue;

            treeRegistryBranch.hold(tree);
            scratch.trees.add(tree);
        }
    }

    // Ground \\

    // The top solid block under an anchor, TREE_GROUND_FLOODED where water stands over it
    private int probeGround(TreePlacementAsyncContainer scratch, long anchorX, long anchorZ) {

        long key = anchorX << Integer.SIZE | (anchorZ & 0xFFFFFFFFL);
        int ground = scratch.anchor2Ground.get(key);

        if (ground != EngineSetting.TREE_GROUND_UNKNOWN)
            return ground;

        ground = worldGenerationManager.probeFlooded(scratch.worldHandle, anchorX, anchorZ)
                ? EngineSetting.TREE_GROUND_FLOODED
                : worldGenerationManager.probeGroundHeight(scratch.worldHandle, anchorX, anchorZ);

        scratch.anchor2Ground.put(key, ground);

        return ground;
    }
}
