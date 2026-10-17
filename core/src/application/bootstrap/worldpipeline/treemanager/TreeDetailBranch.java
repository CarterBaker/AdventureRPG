package application.bootstrap.worldpipeline.treemanager;

import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TreeDetailBranch extends BranchPackage {

    /*
     * Decides which trees are drawn in full: those whose root chunk lies
     * within the tree detail distance of the chunk a grid of their world is
     * centred on, measured between chunks the way grid slots measure theirs.
     * The anchors — every grid's world and active chunk, and the distance —
     * are published on the main thread the frame any of them changes and read
     * under their lock on any thread, so a tree first grown on a streaming
     * thread starts with its flag set. Every registered tree is judged again
     * whenever the anchors change and once a check besides, and each one that
     * crosses the line is redrawn through TreeRebuildBranch alone, its old
     * look standing until the new one lands.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private TreeRegistryBranch treeRegistryBranch;
    private TreeRebuildBranch treeRebuildBranch;

    // Anchors — all three guarded by anchorChunks
    private LongArrayList anchorChunks;
    private ObjectArrayList<WorldHandle> anchorWorlds;
    private int appliedDistance;

    // Walk
    private ObjectArrayList<TreeInstance> trees;
    private float sinceCheck;

    // Base \\

    @Override
    protected void create() {

        // Anchors
        this.anchorChunks = new LongArrayList();
        this.anchorWorlds = new ObjectArrayList<>();

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

    // A tree whose side of the line changed is redrawn at its new detail
    private void judge(TreeInstance tree) {

        boolean detailed = isDetailed(tree);

        if (detailed == tree.isDetailed())
            return;

        tree.setDetailed(detailed);
        treeRebuildBranch.rebuildTree(tree, false);
    }

    // Anchors \\

    // True when a grid moved, came or went, or the distance changed, the anchors then published anew
    private boolean publishAnchors() {

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        int distance = settings.treeDetailDistance;

        synchronized (anchorChunks) {

            if (!hasChanged(grids, distance))
                return false;

            anchorChunks.clear();
            anchorWorlds.clear();

            for (int i = 0; i < grids.size(); i++) {
                anchorChunks.add(grids.get(i).getActiveChunkCoordinate());
                anchorWorlds.add(grids.get(i).getWorldHandle());
            }

            this.appliedDistance = distance;

            return true;
        }
    }

    private boolean hasChanged(ObjectArrayList<GridInstance> grids, int distance) {

        if (distance != appliedDistance || grids.size() != anchorChunks.size())
            return true;

        for (int i = 0; i < grids.size(); i++) {

            GridInstance grid = grids.get(i);

            if (grid.getActiveChunkCoordinate() != anchorChunks.getLong(i)
                    || grid.getWorldHandle() != anchorWorlds.get(i))
                return true;
        }

        return false;
    }

    // Detail \\

    // Any thread — a tree first grown takes its flag from where the grids stand now
    void adopt(TreeInstance tree) {
        tree.setDetailed(isDetailed(tree));
    }

    // Any thread — true when the tree's root chunk lies within the distance of an anchor in its world
    private boolean isDetailed(TreeInstance tree) {

        WorldHandle worldHandle = tree.getWorldHandle();
        long rootChunk = tree.getRootChunkCoordinate();

        synchronized (anchorChunks) {

            int reachSq = appliedDistance * appliedDistance;

            for (int i = 0; i < anchorChunks.size(); i++) {

                if (anchorWorlds.get(i) != worldHandle)
                    continue;

                long delta = WorldWrapUtility.unwrapToGridCoordinate(worldHandle, anchorChunks.getLong(i), rootChunk);
                int deltaX = Coordinate2Long.unpackX(delta);
                int deltaZ = Coordinate2Long.unpackY(delta);

                if (deltaX * deltaX + deltaZ * deltaZ <= reachSq)
                    return true;
            }

            return false;
        }
    }
}
