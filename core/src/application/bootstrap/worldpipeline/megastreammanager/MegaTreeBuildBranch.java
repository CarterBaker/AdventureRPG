package application.bootstrap.worldpipeline.megastreammanager;

import application.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel;
import application.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import application.bootstrap.worldpipeline.megachunk.MegaData;
import application.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer;
import application.bootstrap.worldpipeline.tree.TreeDetail;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import application.bootstrap.worldpipeline.util.TreeModelUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MegaTreeBuildBranch extends BranchPackage {

    /*
     * Async — draws a stand-in for every tree rooted in a mega's chunks into
     * the mega's own tree packet on the WorldStreaming thread, under the
     * mega's lock alone: each chunk's tree palette is read without a lock,
     * and a stand-in never keeps a shape for the tree it stands for. A mega
     * near the player sketches each tree from the skeleton its seed grows,
     * and a distant one outlines it from its size alone; resolveDetail() is
     * the one place a mega's distance picks its detail, and the mega records
     * the detail it was built at so a change of distance builds it again. A
     * tree reaching into several chunks is drawn once, by the mega its root
     * stands in. The mega's coordinate is captured when the build is
     * reserved, so a pooled mega that has since moved or lost its batch is
     * left be. Sets TREE_DATA on success.
     */

    // Internal
    private ThreadHandle threadHandle;
    private TreeManager treeManager;
    private MegaTreeAsyncContainer megaTreeAsyncContainer;

    // Settings
    private int chunkSize;
    private int batchDataIndex;
    private int treeDataIndex;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.megaTreeAsyncContainer = create(MegaTreeAsyncContainer.class);

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
    }

    @Override
    protected void get() {

        // Internal
        this.threadHandle = getThreadHandleFromThreadName(EngineSetting.WORLD_STREAMING_THREAD_NAME);
        this.treeManager = get(TreeManager.class);

        // Settings
        this.batchDataIndex = MegaData.BATCH_DATA.index;
        this.treeDataIndex = MegaData.TREE_DATA.index;
    }

    // Detail \\

    // How finely a mega at a grid slot's detail level draws its trees
    public TreeDetail resolveDetail(GridSlotDetailLevel slotLevel) {
        return slotLevel == GridSlotDetailLevel.DISTANT ? TreeDetail.OUTLINE : TreeDetail.SKETCH;
    }

    // Tree Build \\

    public void buildTrees(MegaChunkInstance mega, TreeDetail detail) {

        MegaDataSyncContainer sync = mega.getMegaDataSyncContainer();
        long expectedMegaCoordinate = mega.getCoordinate();

        executeAsync(threadHandle, () -> {

            MegaTreeAsyncContainer scratch = megaTreeAsyncContainer.getInstance();

            try {
                sync.acquire();
                try {
                    if (mega.getCoordinate() == expectedMegaCoordinate && sync.getData()[batchDataIndex])
                        sync.getData()[treeDataIndex] = buildLocked(mega, detail, scratch);
                } finally {
                    sync.release();
                }
            } finally {
                sync.endWork(MegaDataSyncContainer.WORK_TREE);
                scratch.reset();
            }
        });
    }

    private boolean buildLocked(MegaChunkInstance mega, TreeDetail detail, MegaTreeAsyncContainer scratch) {

        scratch.reset();

        long megaCoordinate = mega.getCoordinate();
        WorldHandle worldHandle = mega.getWorldHandle();
        long originX = (long) Coordinate2Long.unpackX(megaCoordinate) * chunkSize;
        long originZ = (long) Coordinate2Long.unpackY(megaCoordinate) * chunkSize;
        ObjectArrayList<ChunkInstance> chunks = mega.getBatchedChunkList();

        for (int i = 0; i < chunks.size(); i++) {

            ChunkInstance chunk = chunks.get(i);
            long chunkCoordinate = chunk.getCoordinate();
            TreeInstance[] trees = chunk.getTreePaletteHandle().getTrees();

            for (int t = 0; t < trees.length; t++) {

                TreeInstance tree = trees[t];

                if (tree.getRootChunkCoordinate() != chunkCoordinate)
                    continue;

                TreeModelUtility.emit(
                        tree,
                        WorldWrapUtility.wrappedBlockDeltaX(worldHandle, tree.getAnchorX(), originX)
                                + EngineSetting.TREE_ROOT_CENTER_BLOCKS,
                        WorldWrapUtility.wrappedBlockDeltaZ(worldHandle, tree.getAnchorZ(), originZ)
                                + EngineSetting.TREE_ROOT_CENTER_BLOCKS,
                        detail,
                        scratch.model,
                        scratch.bark,
                        scratch.leaves);
            }
        }

        DynamicPacketInstance treePacketInstance = mega.getTreePacketInstance();

        treePacketInstance.beginGenerating();
        treePacketInstance.clearModels();

        boolean success = treePacketInstance.addVertices(treeManager.getBarkMaterialID(), scratch.bark)
                && treePacketInstance.addVertices(treeManager.getLeafMaterialID(), scratch.leaves);

        if (treePacketInstance.hasModels())
            treePacketInstance.setReady();
        else
            treePacketInstance.unlock();

        if (success)
            mega.setTreeDetail(detail);

        return success;
    }
}
