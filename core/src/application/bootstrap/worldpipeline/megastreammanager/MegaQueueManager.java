package application.bootstrap.worldpipeline.megastreammanager;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel;
import application.bootstrap.worldpipeline.gridslot.GridSlotHandle;
import application.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import application.bootstrap.worldpipeline.megachunk.MegaData;
import application.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer;
import application.bootstrap.worldpipeline.megachunk.MegaDataUtility;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;

class MegaQueueManager extends ManagerPackage {

    /*
     * Drives the per-frame mega chunk pipeline for every grid with its own GPU
     * upload budget. Mega resolution and the shared pool are main-thread only;
     * merges run on workers under the mega's lock. Invalidating a mega before
     * its chunk is pooled blocks on that lock so no stale reference survives.
     * A mega is never dumped while it still stands in for chunks awaiting their
     * own upload, and an upload the budget turns away keeps its place at the
     * front of the round robin for the next frame. The moment a grid moves,
     * every mega it left behind is unloaded rather than waiting its turn, so
     * travel never keeps a mega the grid no longer reaches. A mega's tree
     * stand-ins are built and uploaded as a layer of their own once its
     * terrain is on the GPU, and invalidateMegaTrees() rebuilds only them.
     */

    // Internal
    private WorldRenderManager worldRenderManager;
    private WorldStreamManager worldStreamManager;
    private ChunkStreamManager chunkStreamManager;
    private ThreadHandle worldStreamingThreadHandle;

    // Branches
    private MegaMergeBranch mergeBranch;
    private MegaAssessBranch assessBranch;
    private MegaRenderBranch renderBranch;
    private MegaDumpBranch dumpBranch;
    private MegaTreeBuildBranch treeBuildBranch;
    private MegaTreeRenderBranch treeRenderBranch;

    // Pool — shared across all grids
    private ObjectArrayList<MegaChunkInstance> megaPool;

    // Settings
    private int megaPoolMaxOverflow;
    private int megaChunkSize;
    private int megaScale;
    private int megaAssessPerFrame;

    // GPU Upload Throttle
    private int megaGpuUploadBudget;
    private int gpuUploadsThisFrame;
    private LongArrayList deferredUploads;

    // Internal \\

    @Override
    protected void create() {

        // Branches
        this.mergeBranch = create(MegaMergeBranch.class);
        this.assessBranch = create(MegaAssessBranch.class);
        this.renderBranch = create(MegaRenderBranch.class);
        this.dumpBranch = create(MegaDumpBranch.class);
        this.treeBuildBranch = create(MegaTreeBuildBranch.class);
        this.treeRenderBranch = create(MegaTreeRenderBranch.class);

        // Pool
        this.megaPool = new ObjectArrayList<>();

        // Settings
        this.megaPoolMaxOverflow = EngineSetting.MEGA_POOL_MAX_OVERFLOW;
        this.megaChunkSize = EngineSetting.MEGA_CHUNK_SIZE;
        this.megaScale = megaChunkSize * megaChunkSize;
        this.megaAssessPerFrame = EngineSetting.MEGA_ASSESS_PER_FRAME;

        // GPU Upload Throttle
        this.megaGpuUploadBudget = EngineSetting.MAX_MEGA_GPU_UPLOADS_PER_FRAME;
        this.deferredUploads = new LongArrayList();
    }

    @Override
    protected void get() {

        // Internal
        this.worldRenderManager = get(WorldRenderManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.chunkStreamManager = get(ChunkStreamManager.class);
        this.worldStreamingThreadHandle = getThreadHandleFromThreadName(EngineSetting.WORLD_STREAMING_THREAD_NAME);
    }

    @Override
    protected void update() {

        this.gpuUploadsThisFrame = 0;

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++)
            assessActiveMegas((GridInstance) elements[i]);
    }

    // Grid Events \\

    void onGridRebuilt(GridInstance grid) {
        flushActiveMegas(grid);
    }

    void onGridRemoved(GridInstance grid) {
        flushActiveMegas(grid);
    }

    // A mega still busy is left for the round robin to unload on a later pass
    void onGridMoved(GridInstance grid) {

        int megaMax = computeMegaMax(grid);
        var iterator = grid.getActiveMegaChunks().long2ObjectEntrySet().iterator();

        while (iterator.hasNext()) {

            var entry = iterator.next();
            long megaCoord = entry.getLongKey();

            if (grid.getGridSlotForChunk(megaCoord) == null && unloadMega(entry.getValue(), megaCoord, megaMax))
                iterator.remove();
        }
    }

    // Mega Max \\

    private int computeMegaMax(GridInstance grid) {
        return (grid.getTotalSlots() / megaScale) + megaPoolMaxOverflow;
    }

    // Flush \\

    private void flushActiveMegas(GridInstance grid) {

        Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks = grid.getActiveMegaChunks();
        int megaMax = computeMegaMax(grid);

        var iterator = activeMegaChunks.long2ObjectEntrySet().iterator();

        while (iterator.hasNext()) {

            var entry = iterator.next();
            long megaCoord = entry.getLongKey();
            MegaChunkInstance mega = entry.getValue();
            iterator.remove();

            MegaDataSyncContainer sync = mega.getMegaDataSyncContainer();

            if (!sync.tryAcquire()) {
                activeMegaChunks.put(megaCoord, mega);
                continue;
            }

            try {
                worldRenderManager.removeMegaInstance(megaCoord);
                mega.reset();
            } finally {
                sync.release();
            }

            if (megaPool.size() < megaMax)
                megaPool.push(mega);
            else
                mega.dispose();
        }
    }

    // Batching \\

    MegaChunkInstance resolveMegaForChunk(ChunkInstance chunkInstance, GridInstance grid) {

        Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks = grid.getActiveMegaChunks();
        int megaMax = computeMegaMax(grid);

        long megaCoord = Coordinate2Long.toMegaChunkCoordinate(chunkInstance.getCoordinate());
        MegaChunkInstance mega = activeMegaChunks.get(megaCoord);

        if (mega != null)
            return mega;

        mega = createMega(megaCoord, grid, megaMax, activeMegaChunks);

        if (mega == null)
            return null;

        activeMegaChunks.put(megaCoord, mega);
        return mega;
    }

    void mergeIntoMega(ChunkInstance chunkInstance, MegaChunkInstance mega, long expectedMegaCoordinate) {
        mergeBranch.mergeChunkIntoMega(chunkInstance, mega, expectedMegaCoordinate);
    }

    private MegaChunkInstance createMega(
            long megaCoord,
            GridInstance grid,
            int megaMax,
            Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks) {

        if (!megaPool.isEmpty()) {

            MegaChunkInstance pooled = megaPool.top();
            MegaDataSyncContainer sync = pooled.getMegaDataSyncContainer();

            if (!sync.tryAcquire())
                return null;

            try {
                megaPool.pop();
                return configureMega(pooled, megaCoord, grid);
            } finally {
                sync.release();
            }
        }

        if (activeMegaChunks.size() >= megaMax)
            return null;

        return configureMega(create(MegaChunkInstance.class), megaCoord, grid);
    }

    private MegaChunkInstance configureMega(
            MegaChunkInstance mega,
            long megaCoord,
            GridInstance grid) {

        mega.constructor(
                worldRenderManager,
                grid.getWorldHandle(),
                megaCoord,
                chunkStreamManager.getChunkVAO(),
                megaScale);

        return mega;
    }

    // Assessment \\

    // Round robin from the front of the linked map; an assessed mega moves to the back
    private void assessActiveMegas(GridInstance grid) {

        Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks = grid.getActiveMegaChunks();

        if (activeMegaChunks.isEmpty())
            return;

        int megaMax = computeMegaMax(grid);
        int assessCount = Math.min(megaAssessPerFrame, activeMegaChunks.size());

        deferredUploads.clear();

        for (int assessed = 0; assessed < assessCount; assessed++) {

            long megaCoord = activeMegaChunks.firstLongKey();
            MegaChunkInstance mega = activeMegaChunks.getAndMoveToLast(megaCoord);
            MegaDataSyncContainer sync = mega.getMegaDataSyncContainer();
            GridSlotHandle gridSlotHandle = grid.getGridSlotForChunk(megaCoord);

            if (gridSlotHandle == null) {

                if (unloadMega(mega, megaCoord, megaMax))
                    activeMegaChunks.remove(megaCoord);

                continue;
            }

            MegaQueueOperation op = determineOperation(grid, megaCoord, sync, gridSlotHandle);

            switch (op) {
                case ASSESS -> assessBranch.assessMega(mega);
                case RENDER -> {
                    if (gpuUploadsThisFrame < megaGpuUploadBudget) {
                        renderBranch.renderMega(mega, sync);
                        gpuUploadsThisFrame++;
                    } else
                        deferredUploads.add(megaCoord);
                }
                case TREE_BUILD -> treeBuildBranch.buildTrees(mega);
                case TREE_RENDER -> {
                    if (gpuUploadsThisFrame < megaGpuUploadBudget) {
                        treeRenderBranch.renderTrees(mega, sync);
                        gpuUploadsThisFrame++;
                    } else
                        deferredUploads.add(megaCoord);
                }
                case DUMP -> dumpBranch.dumpMega(mega, sync, megaCoord);
                case SKIP -> {
                }
            }
        }

        for (int i = deferredUploads.size() - 1; i >= 0; i--)
            grid.promoteMega(deferredUploads.getLong(i));
    }

    private MegaQueueOperation determineOperation(
            GridInstance grid,
            long megaCoord,
            MegaDataSyncContainer sync,
            GridSlotHandle gridSlotHandle) {

        if (!sync.tryAcquire())
            return MegaQueueOperation.SKIP;

        try {
            GridSlotDetailLevel slotLevel = gridSlotHandle.getDetailLevel();

            MegaData toDump = MegaDataUtility.nextToDump(sync.getData(), slotLevel);

            // Dumping a mega still standing in for its chunks would open a hole until they upload
            if (toDump != null)
                return worldRenderManager.isMegaStandingIn(grid, megaCoord)
                        ? MegaQueueOperation.SKIP
                        : MegaQueueOperation.DUMP;

            MegaData toLoad = MegaDataUtility.nextToLoad(sync.getData(), slotLevel);

            if (toLoad == null)
                return MegaQueueOperation.SKIP;

            MegaQueueOperation operation = toOperation(toLoad);

            // A saturated pool leaves the mega for a later pass before its build is reserved
            if (operation == MegaQueueOperation.TREE_BUILD
                    && (!worldStreamingThreadHandle.hasCapacity()
                            || !sync.beginWorkLocked(MegaDataSyncContainer.WORK_TREE)))
                return MegaQueueOperation.SKIP;

            return operation;
        } finally {
            sync.release();
        }
    }

    private MegaQueueOperation toOperation(MegaData stage) {
        return switch (stage) {
            case BATCH_DATA -> MegaQueueOperation.ASSESS;
            case RENDER_DATA -> MegaQueueOperation.RENDER;
            case TREE_DATA -> MegaQueueOperation.TREE_BUILD;
            case TREE_RENDER_DATA -> MegaQueueOperation.TREE_RENDER;
            default -> MegaQueueOperation.SKIP;
        };
    }

    // Unload \\

    private boolean unloadMega(MegaChunkInstance mega, long megaCoord, int megaMax) {

        MegaDataSyncContainer sync = mega.getMegaDataSyncContainer();

        if (!sync.tryAcquire())
            return false;

        try {
            worldRenderManager.removeMegaInstance(megaCoord);
            mega.reset();
        } finally {
            sync.release();
        }

        if (megaPool.size() < megaMax)
            megaPool.push(mega);
        else
            mega.dispose();

        return true;
    }

    // Invalidation \\

    void invalidateMegaForChunk(long chunkCoordinate) {

        long megaCoord = Coordinate2Long.toMegaChunkCoordinate(chunkCoordinate);

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++) {

            GridInstance grid = (GridInstance) elements[i];
            MegaChunkInstance mega = grid.getActiveMegaChunks().get(megaCoord);

            if (mega == null)
                continue;

            MegaDataSyncContainer sync = mega.getMegaDataSyncContainer();
            sync.acquire();

            try {
                worldRenderManager.removeMegaInstance(megaCoord);
                clearChunkBatchFlags(mega);
                mega.reset();
            } finally {
                sync.release();
            }
        }
    }

    // The stand-ins of the mega holding a chunk built again from its trees as they now stand, the old ones drawn
    // until the new ones land and the mega's terrain left as it is
    void invalidateMegaTrees(long chunkCoordinate) {

        long megaCoord = Coordinate2Long.toMegaChunkCoordinate(chunkCoordinate);

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++) {

            MegaChunkInstance mega = ((GridInstance) elements[i]).getActiveMegaChunks().get(megaCoord);

            if (mega == null)
                continue;

            MegaDataSyncContainer sync = mega.getMegaDataSyncContainer();
            sync.acquire();

            try {
                MegaDataUtility.cascadeClear(MegaData.TREE_DATA, sync.getData());
            } finally {
                sync.release();
            }
        }
    }

    private void clearChunkBatchFlags(MegaChunkInstance mega) {

        ObjectArrayList<ChunkInstance> list = mega.getBatchedChunkList();
        Object[] elements = list.elements();
        int size = list.size();

        for (int i = 0; i < size; i++) {

            ChunkInstance chunk = (ChunkInstance) elements[i];
            ChunkDataSyncContainer chunkSync = chunk.getChunkDataSyncContainer();

            chunkSync.acquire();
            try {
                chunkSync.getData()[ChunkData.BATCH_DATA.index] = false;
            } finally {
                chunkSync.release();
            }
        }
    }
}