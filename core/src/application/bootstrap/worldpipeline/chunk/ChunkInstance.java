package application.bootstrap.worldpipeline.chunk;

import java.util.concurrent.atomic.AtomicLong;

import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldgenerationmanager.GenerationCacheStruct;
import application.bootstrap.worldpipeline.worlditem.WorldItemInstancePaletteHandle;
import application.bootstrap.worldpipeline.worldrendermanager.RenderType;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderInstance;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

public class ChunkInstance extends WorldRenderInstance {

    /*
     * A single 16x16 column of subchunks representing one loaded world chunk.
     * Owns its subchunks permanently — they are never pooled separately.
     * Pooled and reused by ChunkQueueManager. Must be reset via reset() before
     * reuse. Geometry is assembled by merging all subchunk packets into one packet.
     * mergeVersion is a globally unique, monotonically increasing sequence number
     * bumped every time merge() actually rebuilds this chunk's CPU geometry — it
     * never resets, even across pooling reuse, so a mega's per-chunk merge
     * bookkeeping (see MegaBatchStruct) can never collide with a stale entry left
     * by a previous occupant of the same chunk coordinate.
     */

    // Internal
    private ChunkDataSyncContainer chunkDataSyncContainer;
    private SubChunkInstance[] subChunks;
    private ChunkNeighborStruct chunkNeighbors;
    private WorldItemInstancePaletteHandle worldItemInstancePaletteHandle;
    private GenerationCacheStruct terrainCache;

    // Scratch — pre-allocated, reused per merge call
    private int[] vertPositionArray;
    private float[] mergeOffsetValues;

    // Settings
    private int chunkSize;

    // Merge Version
    private static final AtomicLong MERGE_VERSION_SEQUENCE = new AtomicLong();
    private volatile long mergeVersion;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.chunkDataSyncContainer = create(ChunkDataSyncContainer.class);
        this.worldItemInstancePaletteHandle = create(WorldItemInstancePaletteHandle.class);
        this.worldItemInstancePaletteHandle.constructor();
        this.terrainCache = new GenerationCacheStruct();

        this.subChunks = new SubChunkInstance[EngineSetting.WORLD_HEIGHT];
        for (short i = 0; i < EngineSetting.WORLD_HEIGHT; i++)
            subChunks[i] = create(SubChunkInstance.class);

        // Scratch
        this.vertPositionArray = new int[] { 1 };
        this.mergeOffsetValues = new float[1];

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;

        super.create();
    }

    // Constructor \\

    public void constructor(
            WorldRenderManager worldRenderManager,
            WorldHandle worldHandle,
            long coordinate,
            VAOHandle vaoHandle,
            short airBlockId,
            Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks) {

        super.constructor(
                worldRenderManager,
                worldHandle,
                RenderType.INDIVIDUAL,
                coordinate,
                vaoHandle);

        for (byte subChunkCoordinate = 0; subChunkCoordinate < EngineSetting.WORLD_HEIGHT; subChunkCoordinate++)
            subChunks[subChunkCoordinate].constructor(
                    worldRenderManager,
                    worldHandle,
                    subChunkCoordinate,
                    vaoHandle,
                    airBlockId);

        this.chunkNeighbors = new ChunkNeighborStruct(
                coordinate,
                this,
                activeChunks);
    }

    // Reset \\

    public void reset() {
        chunkDataSyncContainer.resetData();
        getDynamicPacket().clear();
        worldItemInstancePaletteHandle.clear();
        terrainCache.invalidate();

        for (SubChunkInstance subChunk : subChunks)
            subChunk.reset();
    }

    // Geometry \\

    public boolean merge() {

        boolean success = true;
        getDynamicPacket().clear();

        for (SubChunkInstance subChunk : subChunks) {
            mergeOffsetValues[0] = subChunk.getCoordinate() * chunkSize;
            if (!getDynamicPacket().merge(
                    subChunk.getDynamicPacketInstance(),
                    vertPositionArray,
                    mergeOffsetValues))
                success = false;
        }

        if (success && getDynamicPacket().hasModels())
            getDynamicPacket().setReady();
        else if (!getDynamicPacket().hasModels())
            getDynamicPacket().unlock();

        mergeVersion = MERGE_VERSION_SEQUENCE.incrementAndGet();

        return success;
    }

    // Accessible \\

    public ChunkDataSyncContainer getChunkDataSyncContainer() {
        return chunkDataSyncContainer;
    }

    public SubChunkInstance[] getSubChunks() {
        return subChunks;
    }

    /*
     * Bounds-checked on purpose: an out-of-range index here is always a
     * corrupt caller-side coordinate (never something this class can
     * recover from), so it fails loudly with the offending value and the
     * valid range rather than surfacing as a bare ArrayIndexOutOfBounds
     * with no context about which chunk or which caller was responsible.
     */
    public SubChunkInstance getSubChunk(int subChunkCoordinate) {

        if (subChunkCoordinate < 0 || subChunkCoordinate >= subChunks.length)
            throwException("Sub-chunk index " + subChunkCoordinate + " is outside valid range [0, "
                    + subChunks.length + ") for chunk at " + getCoordinate()
                    + " — caller passed a corrupt vertical coordinate.");

        return subChunks[subChunkCoordinate];
    }

    public ChunkNeighborStruct getChunkNeighbors() {
        return chunkNeighbors;
    }

    public WorldItemInstancePaletteHandle getWorldItemInstancePaletteHandle() {
        return worldItemInstancePaletteHandle;
    }

    public GenerationCacheStruct getTerrainCache() {
        return terrainCache;
    }

    public long getMergeVersion() {
        return mergeVersion;
    }
}