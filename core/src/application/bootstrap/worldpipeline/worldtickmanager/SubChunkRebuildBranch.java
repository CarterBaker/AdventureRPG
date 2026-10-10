package application.bootstrap.worldpipeline.worldtickmanager;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkDataUtility;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.chunk.ChunkLockBatchStruct;
import application.bootstrap.worldpipeline.chunk.ChunkLockResult;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Direction2Vector;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2LongMap;

public class SubChunkRebuildBranch extends BranchPackage {

    /*
     * The one path the world ticks rebuild the terrain they change through. A
     * tick only queues the subchunks it touched, and rebuildPending() drains
     * the queue each frame within its own budget, oldest chunk first, an
     * urgent change ahead of the rest, so no tick ever stalls a frame. A
     * rebuild reads into all eight neighbors, so like the stream's own build
     * it takes the chunk's lock and every neighbor's together and requires
     * each to be generated; a busy lock waits for a later frame. A chunk the
     * stream has yet to build is dropped, since it builds from its data as it
     * now stands, and one whose neighborhood is no longer whole loses
     * NEIGHBOR_DATA and the build that rests on it, so the stream reassesses
     * it and builds it whole once every neighbor is back.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private DynamicGeometryManager dynamicGeometryManager;
    private DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer;

    // Settings
    private int rebuildsPerFrame;
    private int generationDataIndex;
    private int neighborDataIndex;
    private int buildDataIndex;

    // State
    private int budget;

    // Pending — the subchunks of each chunk still waiting on a rebuild, one bit per subchunk
    private Reference2LongLinkedOpenHashMap<ChunkInstance> chunk2PendingSubChunks;

    // Scratch
    private ChunkLockBatchStruct lockBatch;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.rebuildsPerFrame = EngineSetting.WORLD_TICK_REBUILDS_PER_FRAME;
        this.generationDataIndex = ChunkData.GENERATION_DATA.index;
        this.neighborDataIndex = ChunkData.NEIGHBOR_DATA.index;
        this.buildDataIndex = ChunkData.BUILD_DATA.index;

        if (EngineSetting.WORLD_HEIGHT > Long.SIZE)
            throwException("Subchunk rebuilds track a chunk's subchunks in one long — WORLD_HEIGHT "
                    + EngineSetting.WORLD_HEIGHT + " exceeds " + Long.SIZE + " subchunks.");

        // Pending
        this.chunk2PendingSubChunks = new Reference2LongLinkedOpenHashMap<>();

        // Scratch
        this.lockBatch = new ChunkLockBatchStruct(Direction2Vector.LENGTH + 1);
    }

    @Override
    protected void get() {

        // Internal
        this.worldStreamManager = get(WorldStreamManager.class);
        this.dynamicGeometryManager = get(DynamicGeometryManager.class);
        this.dynamicGeometryAsyncContainer = dynamicGeometryManager.getDynamicGeometryAsyncInstance();
    }

    // Queue \\

    // Subchunks queued behind every chunk already waiting, a chunk already queued keeping its place
    public void queueRebuild(ChunkInstance chunk, long subChunkMask) {
        chunk2PendingSubChunks.put(chunk, chunk2PendingSubChunks.getLong(chunk) | subChunkMask);
    }

    // Subchunks queued ahead of every chunk already waiting, for a change the player can see happen
    public void queueUrgentRebuild(ChunkInstance chunk, long subChunkMask) {
        chunk2PendingSubChunks.putAndMoveToFirst(chunk, chunk2PendingSubChunks.getLong(chunk) | subChunkMask);
    }

    public static long toSubChunkBit(int subChunkY) {
        return 1L << subChunkY;
    }

    // Rebuild \\

    // Queued subchunks rebuilt from the front of the queue up to the frame's budget
    public void rebuildPending() {

        this.budget = rebuildsPerFrame;
        ObjectIterator<Reference2LongMap.Entry<ChunkInstance>> iterator = chunk2PendingSubChunks
                .reference2LongEntrySet().fastIterator();

        while (budget > 0 && iterator.hasNext()) {

            Reference2LongMap.Entry<ChunkInstance> entry = iterator.next();
            long pending = rebuildChunk(entry.getKey(), entry.getLongValue());

            if (pending == 0L)
                iterator.remove();
            else
                entry.setValue(pending);
        }
    }

    // The chunk's pending subchunks rebuilt under its neighborhood's locks — the ones still pending returned
    private long rebuildChunk(ChunkInstance chunk, long pending) {

        lockBatch.clear();

        if (!lockBatch.addNeighborhood(chunk))
            return handToStream(chunk, pending);

        ChunkLockResult result = lockBatch.tryLockAll(generationDataIndex);

        if (result != ChunkLockResult.ACQUIRED) {
            lockBatch.unlockAll();
            return result == ChunkLockResult.BUSY ? pending : handToStream(chunk, pending);
        }

        try {
            boolean[] data = chunk.getChunkDataSyncContainer().getData();

            if (!data[neighborDataIndex] || !data[buildDataIndex])
                return 0L;

            pending = rebuildSubChunks(chunk, pending);
            ChunkDataUtility.cascadeClear(ChunkData.MERGE_DATA, data);
        } finally {
            lockBatch.unlockAll();
        }

        worldStreamManager.invalidateMegaForChunk(chunk.getCoordinate());

        return pending;
    }

    // Pending subchunks rebuilt lowest first while the frame's budget lasts — the ones left over returned
    private long rebuildSubChunks(ChunkInstance chunk, long pending) {

        while (pending != 0L && budget > 0) {

            int subChunkY = Long.numberOfTrailingZeros(pending);
            pending &= pending - 1L;
            budget--;

            chunk.getSubChunk(subChunkY).getDynamicPacketInstance().clear();
            dynamicGeometryManager.buildSubChunk(dynamicGeometryAsyncContainer, chunk, subChunkY);
        }

        return pending;
    }

    // A chunk missing a generated neighbor cannot rebuild in place, so the stream reassesses and builds it whole
    private long handToStream(ChunkInstance chunk, long pending) {

        ChunkDataSyncContainer syncContainer = chunk.getChunkDataSyncContainer();

        if (!syncContainer.tryAcquire())
            return pending;

        try {
            boolean[] data = syncContainer.getData();

            if (data[neighborDataIndex])
                ChunkDataUtility.cascadeClear(ChunkData.NEIGHBOR_DATA, data);
        } finally {
            syncContainer.release();
        }

        return 0L;
    }
}
