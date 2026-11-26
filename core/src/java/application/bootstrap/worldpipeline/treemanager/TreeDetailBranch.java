package application.bootstrap.worldpipeline.treemanager;

import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.util.TreeLodUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TreeDetailBranch extends BranchPackage {

    /*
     * Decides the LOD every tree is drawn at, whichever chunk, mega or macro
     * tile draws it, from the distance between its root chunk and the chunk
     * the nearest grid of its world is centred on, measured between chunks
     * the way grid slots measure theirs: in full within that grid's tree
     * detail distance, else the coarse level TreeLodUtility gives that
     * distance, drawn as its species' shared model from the grid's shared
     * level on. resolveLod() is the one place a tree's LOD is decided. The
     * anchors — every grid's world, active chunk, reach and shared level —
     * are published on the main thread the frame any of them changes and
     * read under their lock on any thread, so a tree first grown on a
     * streaming thread starts with its LOD set. A tree drawn in full keeps
     * its detail until its root lies a margin of chunks past the distance,
     * and a coarse level holds a margin of blocks past either end, so a
     * player pacing along a line never has the trees on it redrawn back and
     * forth. Every registered tree is judged again whenever the anchors
     * change and once a check besides, and each one whose LOD changed is
     * redrawn through TreeRebuildBranch alone, its old look standing until
     * the new one lands.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private TreeRegistryBranch treeRegistryBranch;
    private TreeRebuildBranch treeRebuildBranch;

    // Anchors — all five guarded by anchorChunks
    private LongArrayList anchorChunks;
    private ObjectArrayList<WorldHandle> anchorWorlds;
    private IntArrayList anchorReachesSq;
    private IntArrayList anchorKeepsSq;
    private IntArrayList anchorSharedLevels;

    // Settings
    private int chunkSize;

    // Walk
    private ObjectArrayList<TreeInstance> trees;
    private float sinceCheck;

    // Base \\

    @Override
    protected void create() {

        // Anchors
        this.anchorChunks = new LongArrayList();
        this.anchorWorlds = new ObjectArrayList<>();
        this.anchorReachesSq = new IntArrayList();
        this.anchorKeepsSq = new IntArrayList();
        this.anchorSharedLevels = new IntArrayList();

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;

        // Walk
        this.trees = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.worldStreamManager = get(WorldStreamManager.class);
        this.treeRegistryBranch = get(TreeRegistryBranch.class);
        this.treeRebuildBranch = get(TreeRebuildBranch.class);
    }

    // Update \\

    void update(float deltaTime) {

        sinceCheck += deltaTime;

        if (!publishAnchors() && sinceCheck < EngineSetting.TREE_DETAIL_CHECK_SECONDS)
            return;

        sinceCheck = 0f;
        treeRegistryBranch.collectTrees(trees);

        for (int i = 0; i < trees.size(); i++)
            judge(trees.get(i));

        trees.clear();
    }

    // A tree whose LOD changed is redrawn at its new one
    private void judge(TreeInstance tree) {

        int lod = resolveLod(tree.getWorldHandle(), tree.getRootChunkCoordinate(), tree.getLod());

        if (lod == tree.getLod())
            return;

        tree.setLod(lod);
        treeRebuildBranch.rebuildTree(tree, false);
    }

    // Anchors \\

    // True when a grid moved, came or went, or its distance changed, the anchors then published anew
    private boolean publishAnchors() {

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();

        synchronized (anchorChunks) {

            if (!hasChanged(grids))
                return false;

            anchorChunks.clear();
            anchorWorlds.clear();
            anchorReachesSq.clear();
            anchorKeepsSq.clear();
            anchorSharedLevels.clear();

            for (int i = 0; i < grids.size(); i++) {

                GridInstance grid = grids.get(i);
                int distance = grid.getTreeDetailDistance();
                int keep = distance + EngineSetting.TREE_DETAIL_KEEP_CHUNKS;

                anchorChunks.add(grid.getActiveChunkCoordinate());
                anchorWorlds.add(grid.getWorldHandle());
                anchorReachesSq.add(distance * distance);
                anchorKeepsSq.add(keep * keep);
                anchorSharedLevels.add(grid.getTreeSharedLevel());
            }

            return true;
        }
    }

    private boolean hasChanged(ObjectArrayList<GridInstance> grids) {

        if (grids.size() != anchorChunks.size())
            return true;

        for (int i = 0; i < grids.size(); i++) {

            GridInstance grid = grids.get(i);
            int distance = grid.getTreeDetailDistance();

            if (grid.getActiveChunkCoordinate() != anchorChunks.getLong(i)
                    || grid.getWorldHandle() != anchorWorlds.get(i)
                    || distance * distance != anchorReachesSq.getInt(i)
                    || grid.getTreeSharedLevel() != anchorSharedLevels.getInt(i))
                return true;
        }

        return false;
    }

    // Detail \\

    // Any thread — a tree first grown takes its LOD from where the grids stand now
    void adopt(TreeInstance tree) {
        tree.setLod(resolveLod(tree.getWorldHandle(), tree.getRootChunkCoordinate(), EngineSetting.TREE_LOD_NONE));
    }

    // Any thread — the LOD a tree rooted in a chunk is drawn at, judged against the nearest anchor in its world and
    // held near its current one, TREE_LOD_NONE holding to none; the finest coarse level while no grid stands in its
    // world
    int resolveLod(WorldHandle worldHandle, long rootChunk, int currentLod) {

        synchronized (anchorChunks) {

            int nearest = EngineSetting.INDEX_NOT_FOUND;
            int nearestSq = Integer.MAX_VALUE;

            for (int i = 0; i < anchorChunks.size(); i++) {

                if (anchorWorlds.get(i) != worldHandle)
                    continue;

                long delta = WorldWrapUtility.unwrapToGridCoordinate(worldHandle, anchorChunks.getLong(i), rootChunk);
                int deltaX = Coordinate2Long.unpackX(delta);
                int deltaZ = Coordinate2Long.unpackY(delta);
                int distanceSq = deltaX * deltaX + deltaZ * deltaZ;

                if (distanceSq < nearestSq) {
                    nearest = i;
                    nearestSq = distanceSq;
                }
            }

            if (nearest == EngineSetting.INDEX_NOT_FOUND)
                return EngineSetting.TREE_LOD_FULL + 1;

            int currentLevel = currentLod == EngineSetting.TREE_LOD_NONE
                    ? EngineSetting.TREE_LOD_NONE
                    : TreeLodUtility.toLevel(currentLod);
            int reachSq = currentLevel == EngineSetting.TREE_LOD_FULL
                    ? anchorKeepsSq.getInt(nearest)
                    : anchorReachesSq.getInt(nearest);

            if (nearestSq <= reachSq)
                return EngineSetting.TREE_LOD_FULL;

            int level = TreeLodUtility.resolveLevel((float) Math.sqrt(nearestSq) * chunkSize, currentLevel);

            return TreeLodUtility.encode(level, level >= anchorSharedLevels.getInt(nearest));
        }
    }
}
