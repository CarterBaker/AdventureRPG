package application.bootstrap.worldpipeline.megachunk;

import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldrendermanager.RenderType;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderInstance;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import engine.settings.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MegaChunkInstance extends WorldRenderInstance {

    /*
     * A merged geometry batch composed of MEGA_CHUNK_SIZE^2 adjacent
     * ChunkInstances.
     * Geometry is accumulated incrementally via batchAndMerge() as each chunk
     * contributes. Re-contribution triggers a full re-merge of all registered
     * chunks.
     * Once all chunks are present, finalizeGeometry() marks the packet ready for
     * GPU upload. Threading is governed by MegaDataSyncContainer.
     */

    // Internal
    private MegaDataSyncContainer megaDataSyncContainer;
    private MegaBatchStruct megaBatchStruct;

    // Scratch — pre-allocated, reused per merge call
    private int[] vertPositionArray;
    private float[] mergeOffsetValues;

    // State
    private int megaScale;
    private int megaX;
    private int megaZ;

    // Settings
    private int chunkSize;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.megaDataSyncContainer = create(MegaDataSyncContainer.class);
        this.megaBatchStruct = new MegaBatchStruct();

        // Scratch
        this.vertPositionArray = new int[] { 0, 2 };
        this.mergeOffsetValues = new float[2];

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;

        super.create();
    }

    // Constructor \\

    public void constructor(
            WorldRenderManager worldRenderManager,
            WorldHandle worldHandle,
            long megaChunkCoordinate,
            VAOHandle vaoHandle,
            int megaScale) {

        super.constructor(
                worldRenderManager,
                worldHandle,
                RenderType.BATCHED,
                megaChunkCoordinate,
                vaoHandle);

        this.megaX = Coordinate2Long.unpackX(megaChunkCoordinate);
        this.megaZ = Coordinate2Long.unpackY(megaChunkCoordinate);
        this.megaScale = megaScale;

        megaBatchStruct.constructor(megaChunkCoordinate, megaScale);
        megaDataSyncContainer.resetData();
    }

    // Reset \\

    public void reset() {
        megaDataSyncContainer.resetData();
        getDynamicPacket().clear();
        megaBatchStruct.reset();
    }

    // Geometry \\

    /*
     * Fresh contribution: merge the chunk's geometry into the packet and register
     * it. Re-contribution: clear the packet and re-merge all registered chunks in
     * full using the updated geometry. Returns false if any merge step fails.
     */
    public boolean batchAndMerge(ChunkInstance chunkInstance) {

        long chunkCoord = chunkInstance.getCoordinate();
        boolean isRemerge = megaBatchStruct.getBatchedChunks().containsKey(chunkCoord);

        if (isRemerge) {
            megaBatchStruct.updateChunk(chunkCoord, chunkInstance);
            megaBatchStruct.clearMerged();
            getDynamicPacket().clear();

            ObjectArrayList<ChunkInstance> list = megaBatchStruct.getBatchedChunkList();
            Object[] elements = list.elements();
            int size = list.size();

            for (int i = 0; i < size; i++) {
                ChunkInstance batched = (ChunkInstance) elements[i];
                if (!mergeChunk(batched))
                    return false;
                megaBatchStruct.recordMerged(batched.getCoordinate());
            }

            return true;
        }

        if (!megaBatchStruct.registerChunk(chunkInstance))
            return false;

        if (!mergeChunk(chunkInstance))
            return false;

        megaBatchStruct.recordMerged(chunkCoord);
        return true;
    }

    private boolean mergeChunk(ChunkInstance chunkInstance) {

        long chunkCoord = chunkInstance.getCoordinate();
        int chunkX = Coordinate2Long.unpackX(chunkCoord);
        int chunkZ = Coordinate2Long.unpackY(chunkCoord);

        mergeOffsetValues[0] = (chunkX - megaX) * chunkSize;
        mergeOffsetValues[1] = (chunkZ - megaZ) * chunkSize;

        return getDynamicPacket().merge(
                chunkInstance.getDynamicPacketInstance(),
                vertPositionArray,
                mergeOffsetValues);
    }

    /*
     * Marks the packet ready for GPU upload once all chunks have contributed.
     * If the mega is empty after a full re-merge the packet is unlocked instead.
     */
    public void finalizeGeometry() {
        if (getDynamicPacket().hasModels())
            getDynamicPacket().setReady();
        else
            getDynamicPacket().unlock();
    }

    // Accessible \\

    public MegaDataSyncContainer getMegaDataSyncContainer() {
        return megaDataSyncContainer;
    }

    public boolean isReadyToRender() {
        return megaBatchStruct.isReadyToRender();
    }

    public Long2ObjectOpenHashMap<ChunkInstance> getBatchedChunks() {
        return megaBatchStruct.getBatchedChunks();
    }

    public ObjectArrayList<ChunkInstance> getBatchedChunkList() {
        return megaBatchStruct.getBatchedChunkList();
    }
}