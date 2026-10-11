package application.bootstrap.worldpipeline.megachunk;

import application.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance;
import application.bootstrap.geometrypipeline.quadpacket.QuadPacketInstance;
import application.bootstrap.geometrypipeline.quadpacket.QuadRecordUtility;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
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
     * A merged geometry batch of MEGA_CHUNK_SIZE² chunks, built straight from
     * its chunks' subchunk records so no chunk keeps a merged copy for it. A
     * chunk joining for the first time is appended; a chunk contributing again
     * forces a full rebuild from every member, which MegaMergeBranch runs only
     * while holding all their locks. Either way every bucket is reserved
     * before a record is copied, so it grows at most once. finalizeGeometry()
     * marks the packet ready once every chunk is present. The stand-ins for the trees rooted in its chunks live
     * in a packet of their own, so a tree that changes never re-merges the
     * ground.
     */

    // Internal
    private MegaDataSyncContainer megaDataSyncContainer;
    private MegaBatchHandle megaBatchStruct;
    private DynamicPacketInstance treePacketInstance;

    // State
    private int megaScale;
    private int megaX;
    private int megaZ;

    // Settings
    private int subChunkSpan;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.megaDataSyncContainer = create(MegaDataSyncContainer.class);
        this.megaBatchStruct = create(MegaBatchHandle.class);
        this.treePacketInstance = create(DynamicPacketInstance.class);

        // Settings
        this.subChunkSpan = EngineSetting.CHUNK_SIZE * EngineSetting.SUB_BLOCK_DIVISIONS;

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
                megaChunkCoordinate);

        this.megaX = Coordinate2Long.unpackX(megaChunkCoordinate);
        this.megaZ = Coordinate2Long.unpackY(megaChunkCoordinate);
        this.megaScale = megaScale;

        megaBatchStruct.constructor(megaChunkCoordinate, megaScale);
        treePacketInstance.constructor(vaoHandle);
        megaDataSyncContainer.resetData();
    }

    // Reset \\

    public void reset() {
        megaDataSyncContainer.resetData();
        getQuadPacket().clear();
        treePacketInstance.clear();
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

        QuadPacketInstance quadPacket = getQuadPacket();

        reserveChunk(quadPacket, chunkInstance);
        quadPacket.commitReserve();
        mergeChunk(quadPacket, chunkInstance);

        megaBatchStruct.recordMerged(chunkInstance.getCoordinate());
        megaBatchStruct.recordMergedVersion(chunkInstance.getCoordinate(), chunkInstance.getMergeVersion());
        return true;
    }

    public void batchAndMergeAll(ChunkInstance chunkInstance) {

        megaBatchStruct.updateChunk(chunkInstance.getCoordinate(), chunkInstance);
        megaBatchStruct.clearMerged();

        QuadPacketInstance quadPacket = getQuadPacket();
        ObjectArrayList<ChunkInstance> list = megaBatchStruct.getBatchedChunkList();
        Object[] elements = list.elements();
        int size = list.size();

        quadPacket.clear();

        for (int i = 0; i < size; i++)
            reserveChunk(quadPacket, (ChunkInstance) elements[i]);

        quadPacket.commitReserve();

        for (int i = 0; i < size; i++) {
            ChunkInstance batched = (ChunkInstance) elements[i];
            mergeChunk(quadPacket, batched);
            megaBatchStruct.recordMerged(batched.getCoordinate());
            megaBatchStruct.recordMergedVersion(batched.getCoordinate(), batched.getMergeVersion());
        }
    }

    private void reserveChunk(QuadPacketInstance quadPacket, ChunkInstance chunkInstance) {

        SubChunkInstance[] subChunks = chunkInstance.getSubChunks();

        for (int i = 0; i < subChunks.length; i++)
            quadPacket.reserve(subChunks[i].getQuadPacketInstance());
    }

    // Every subchunk's records shifted from its own origin to the mega's
    private void mergeChunk(QuadPacketInstance quadPacket, ChunkInstance chunkInstance) {

        long chunkCoordinate = chunkInstance.getCoordinate();
        int offsetX = (Coordinate2Long.unpackX(chunkCoordinate) - megaX) * subChunkSpan;
        int offsetZ = (Coordinate2Long.unpackY(chunkCoordinate) - megaZ) * subChunkSpan;
        SubChunkInstance[] subChunks = chunkInstance.getSubChunks();

        for (int i = 0; i < subChunks.length; i++)
            quadPacket.merge(
                    subChunks[i].getQuadPacketInstance(),
                    QuadRecordUtility.packPosition(offsetX, i * subChunkSpan, offsetZ));
    }

    public void finalizeGeometry() {
        getQuadPacket().finish();
    }

    // Accessible \\

    public MegaDataSyncContainer getMegaDataSyncContainer() {
        return megaDataSyncContainer;
    }

    public DynamicPacketInstance getTreePacketInstance() {
        return treePacketInstance;
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