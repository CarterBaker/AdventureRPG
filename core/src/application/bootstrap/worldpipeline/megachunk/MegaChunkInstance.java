package application.bootstrap.worldpipeline.megachunk;

import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldrendermanager.RenderType;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderInstance;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MegaChunkInstance extends WorldRenderInstance {

    /*
     * A merged geometry batch composed of MEGA_CHUNK_SIZE^2 adjacent
     * ChunkInstances. batchAndMergeSingle() appends one chunk's own geometry
     * to the packet the first time that chunk joins the mega — safe with
     * only that chunk's own lock held, since no other member is touched.
     * batchAndMergeAll() instead rebuilds the whole packet from every
     * registered chunk's current geometry, since a chunk's prior
     * contribution can't be surgically removed from the shared vertex
     * buffer; this is only ever called by MegaMergeBranch once it holds
     * every registered chunk's own ChunkDataSyncContainer lock, since each
     * one's DynamicPacketInstance is otherwise mutated independently by its
     * own streaming and liquid-tick pipelines. Once all chunks are present,
     * finalizeGeometry() marks the packet ready for GPU upload. Threading
     * for the mega's own bookkeeping is governed by MegaDataSyncContainer.
     */

    // Internal
    private MegaDataSyncContainer megaDataSyncContainer;
    private MegaBatchHandle megaBatchStruct;

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
        this.megaBatchStruct = create(MegaBatchHandle.class);

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

    public boolean needsMerge(ChunkInstance chunkInstance) {
        return megaBatchStruct.needsMerge(chunkInstance.getCoordinate(), chunkInstance.getMergeVersion());
    }

    public boolean isRegistered(long chunkCoordinate) {
        return megaBatchStruct.getBatchedChunks().containsKey(chunkCoordinate);
    }

    public boolean batchAndMergeSingle(ChunkInstance chunkInstance) {

        if (!megaBatchStruct.registerChunk(chunkInstance))
            return false;

        if (!mergeChunk(chunkInstance))
            return false;

        megaBatchStruct.recordMerged(chunkInstance.getCoordinate());
        megaBatchStruct.recordMergedVersion(chunkInstance.getCoordinate(), chunkInstance.getMergeVersion());
        return true;
    }

    public boolean batchAndMergeAll(ChunkInstance chunkInstance) {

        megaBatchStruct.updateChunk(chunkInstance.getCoordinate(), chunkInstance);
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
            megaBatchStruct.recordMergedVersion(batched.getCoordinate(), batched.getMergeVersion());
        }

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