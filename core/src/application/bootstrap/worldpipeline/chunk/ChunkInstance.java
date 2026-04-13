package application.bootstrap.worldpipeline.chunk;

import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worlditem.WorldItemInstancePaletteHandle;
import application.bootstrap.worldpipeline.worldrendermanager.RenderType;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderInstance;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import application.core.settings.EngineSetting;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

public class ChunkInstance extends WorldRenderInstance {

    /*
     * A single 16x16 column of subchunks representing one loaded world chunk.
     * Owns its subchunks permanently — they are never pooled separately.
     * Pooled and reused by ChunkQueueManager. Must be reset via reset() before
     * reuse. Geometry is assembled by merging all subchunk packets into one packet.
     */

    // Internal
    private ChunkDataSyncContainer chunkDataSyncContainer;
    private SubChunkInstance[] subChunks;
    private ChunkNeighborStruct chunkNeighbors;
    private WorldItemInstancePaletteHandle worldItemInstancePaletteHandle;

    // Scratch — pre-allocated, reused per merge call
    private int[] vertPositionArray;
    private float[] mergeOffsetValues;

    // Settings
    private int chunkSize;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.chunkDataSyncContainer = create(ChunkDataSyncContainer.class);
        this.worldItemInstancePaletteHandle = create(WorldItemInstancePaletteHandle.class);
        this.worldItemInstancePaletteHandle.constructor();

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
            short defaultBiomeId,
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
                    airBlockId,
                    defaultBiomeId);

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

        return success;
    }

    // Accessible \\

    public ChunkDataSyncContainer getChunkDataSyncContainer() {
        return chunkDataSyncContainer;
    }

    public SubChunkInstance[] getSubChunks() {
        return subChunks;
    }

    public SubChunkInstance getSubChunk(int subChunkCoordinate) {
        return subChunks[subChunkCoordinate];
    }

    public ChunkNeighborStruct getChunkNeighbors() {
        return chunkNeighbors;
    }

    public WorldItemInstancePaletteHandle getWorldItemInstancePaletteHandle() {
        return worldItemInstancePaletteHandle;
    }
}