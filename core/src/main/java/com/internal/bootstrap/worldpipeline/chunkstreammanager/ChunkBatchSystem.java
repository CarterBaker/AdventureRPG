package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridManager;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotHandle;
import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaState;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderSystem;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.ThreadHandle;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

import java.util.ArrayDeque;

public class ChunkBatchSystem extends SystemPackage {

    // Internal
    private ThreadHandle threadHandle;
    private WorldRenderSystem worldRenderSystem;
    private ChunkStreamManager chunkStreamManager;
    private ChunkPositionSystem chunkPositionSystem;
    private GridManager gridManager;

    private Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks;
    private LongLinkedOpenHashSet unloadRequests;

    // Mega Pool
    private ArrayDeque<MegaChunkInstance> megaPool;
    private int MEGA_POOL_MAX_OVERFLOW;

    private int MEGA_CHUNK_SIZE;
    private int megaScale;
    private int batchDataIndex;

    @Override
    protected void get() {

        this.threadHandle = getThreadHandleFromThreadName("WorldStreaming");
        this.worldRenderSystem = get(WorldRenderSystem.class);
        this.chunkStreamManager = get(ChunkStreamManager.class);
        this.chunkPositionSystem = get(ChunkPositionSystem.class);
        this.gridManager = get(GridManager.class);

        this.MEGA_CHUNK_SIZE = EngineSetting.MEGA_CHUNK_SIZE;
        this.megaScale = MEGA_CHUNK_SIZE * MEGA_CHUNK_SIZE;
        this.batchDataIndex = ChunkData.BATCH_DATA.index;

        this.unloadRequests = new LongLinkedOpenHashSet();
        this.megaPool = new ArrayDeque<>();
        this.MEGA_POOL_MAX_OVERFLOW = EngineSetting.MEGA_POOL_MAX_OVERFLOW;
    }

    @Override
    protected void update() {
        assessMegaChunks();
    }

    // Batching Logic \\

    public void batchChunk(ChunkInstance chunkInstance) {

        long megaChunkCoordinate = Coordinate2Long.toMegaChunkCoordinate(chunkInstance.getCoordinate());

        MegaChunkInstance megaChunkInstance = activeMegaChunks.computeIfAbsent(
                megaChunkCoordinate,
                this::createMegaChunkInstance);

        megaChunkInstance.batchChunk(chunkInstance);

        MegaState state = megaChunkInstance.getMegaState();

        if (megaChunkInstance.isComplete() &&
                (state == MegaState.UNINITIALIZED ||
                        state == MegaState.PARTIAL ||
                        state == MegaState.UPLOADED))
            megaChunkInstance.setMegaState(MegaState.NEEDS_MERGE);
    }

    private MegaChunkInstance createMegaChunkInstance(long megaChunkCoordinate) {

        MegaChunkInstance megaChunkInstance = megaPool.isEmpty()
                ? create(MegaChunkInstance.class)
                : megaPool.poll();

        megaChunkInstance.constructor(
                worldRenderSystem,
                chunkStreamManager.getActiveWorldHandle(),
                megaChunkCoordinate,
                chunkStreamManager.getChunkVAO(),
                megaScale);

        GridSlotHandle gridSlotHandle = chunkPositionSystem.getGridSlotHandleForChunk(megaChunkCoordinate);
        megaChunkInstance.setGridSlotHandle(gridSlotHandle);

        return megaChunkInstance;
    }

    // Assessment & Processing \\

    private void assessMegaChunks() {

        var iterator = activeMegaChunks.long2ObjectEntrySet().iterator();

        while (iterator.hasNext()) {

            var entry = iterator.next();
            long megaCoordinate = entry.getLongKey();
            MegaChunkInstance megaChunkInstance = entry.getValue();
            MegaState state = megaChunkInstance.getMegaState();

            // Pending unload
            if (unloadRequests.contains(megaCoordinate)) {

                // Thread is active - defer
                if (state == MegaState.MERGING)
                    continue;

                unloadRequests.remove(megaCoordinate);
                iterator.remove();

                worldRenderSystem.removeMegaInstance(megaCoordinate);
                megaChunkInstance.reset();

                int megaSlots = gridManager.getGrid().getTotalSlots() / megaScale;
                if (megaPool.size() < megaSlots + MEGA_POOL_MAX_OVERFLOW)
                    megaPool.push(megaChunkInstance);
                else
                    megaChunkInstance.dispose();

                continue;
            }

            switch (state) {
                case NEEDS_MERGE -> mergeMega(megaChunkInstance);
                case MERGED -> uploadMegaToGPU(megaChunkInstance);
                default -> {
                }
            }
        }
    }

    // Merge \\

    private void mergeMega(MegaChunkInstance megaChunkInstance) {

        if (!megaChunkInstance.tryBeginOperation(MegaState.MERGING))
            return;

        executeAsync(threadHandle, () -> {

            if (megaChunkInstance.merge())
                megaChunkInstance.setMegaState(MegaState.MERGED);
            else
                megaChunkInstance.setMegaState(MegaState.NEEDS_MERGE);
        });
    }

    // GPU Upload \\

    private void uploadMegaToGPU(MegaChunkInstance megaChunkInstance) {

        if (worldRenderSystem.addMegaInstance(megaChunkInstance)) {
            megaChunkInstance.setMegaState(MegaState.UPLOADED);

            Long2ObjectOpenHashMap<ChunkInstance> batchedChunks = megaChunkInstance.getBatchedChunks();

            for (ChunkInstance chunk : batchedChunks.values()) {
                if (chunk.getChunkDataSyncContainer().tryAcquire()) {
                    try {
                        chunk.getChunkDataSyncContainer().data[batchDataIndex] = true;
                    } finally {
                        chunk.getChunkDataSyncContainer().release();
                    }
                }
            }
        }
    }

    // Unload \\

    public void requestUnload(long megaChunkCoordinate) {

        if (activeMegaChunks.containsKey(megaChunkCoordinate))
            unloadRequests.add(megaChunkCoordinate);
    }

    // Accessible \\

    public void setActiveMegaChunks(Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks) {
        this.activeMegaChunks = activeMegaChunks;
    }

    public void invalidateMegaForChunk(long chunkCoordinate) {

        long megaCoordinate = Coordinate2Long.toMegaChunkCoordinate(chunkCoordinate);
        MegaChunkInstance mega = activeMegaChunks.get(megaCoordinate);

        if (mega == null)
            return;

        if (mega.getMegaState() == MegaState.MERGING)
            return;

        mega.setMegaState(MegaState.NEEDS_MERGE);
    }
}