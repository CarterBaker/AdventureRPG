package application.bootstrap.worldpipeline.megachunk;

import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import engine.root.StructPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MegaBatchStruct extends StructPackage {

    /*
     * Tracks which chunks are registered, which have been merged into the
     * mega's geometry, and — via mergedChunkVersions — the exact content
     * version of each chunk's contribution currently reflected in that
     * geometry. The version map is what lets needsMerge() tell a genuine
     * content change apart from a chunk being redispatched for reasons
     * unrelated to its own geometry (its mega still waiting on GPU upload,
     * for instance), so the expensive full mega remerge only ever runs when
     * something actually changed. mergedCoordinates still drives readiness —
     * when its size reaches megaScale all contributions are present — and is
     * cleared and rebuilt alongside the version map on any full re-merge so
     * both stay in lockstep.
     */

    // Internal
    private long megaChunkCoordinate;
    private int megaScale;
    private final Long2ObjectOpenHashMap<ChunkInstance> batchedChunks;
    private final ObjectArrayList<ChunkInstance> batchedChunkList;
    private final LongOpenHashSet mergedCoordinates;
    private final Long2LongOpenHashMap mergedChunkVersions;

    // Constructor \\

    public MegaBatchStruct() {
        this.batchedChunks = new Long2ObjectOpenHashMap<>();
        this.batchedChunkList = new ObjectArrayList<>();
        this.mergedCoordinates = new LongOpenHashSet();
        this.mergedChunkVersions = new Long2LongOpenHashMap();
        this.mergedChunkVersions.defaultReturnValue(-1L);
    }

    public void constructor(long megaChunkCoordinate, int megaScale) {
        this.megaChunkCoordinate = megaChunkCoordinate;
        this.megaScale = megaScale;
        this.batchedChunks.clear();
        this.batchedChunkList.clear();
        this.mergedCoordinates.clear();
        this.mergedChunkVersions.clear();
    }

    // Reset \\

    public void reset() {
        batchedChunks.clear();
        batchedChunkList.clear();
        mergedCoordinates.clear();
        mergedChunkVersions.clear();
    }

    // Management \\

    public boolean registerChunk(ChunkInstance chunkInstance) {

        if (Coordinate2Long.toMegaChunkCoordinate(chunkInstance.getCoordinate()) != megaChunkCoordinate)
            return false;

        long coord = chunkInstance.getCoordinate();
        batchedChunks.put(coord, chunkInstance);
        batchedChunkList.add(chunkInstance);
        return true;
    }

    public void updateChunk(long coord, ChunkInstance chunkInstance) {
        batchedChunks.put(coord, chunkInstance);
        for (int i = 0; i < batchedChunkList.size(); i++) {
            if (batchedChunkList.get(i).getCoordinate() == coord) {
                batchedChunkList.set(i, chunkInstance);
                return;
            }
        }
    }

    public void recordMerged(long coordinate) {
        mergedCoordinates.add(coordinate);
    }

    public void clearMerged() {
        mergedCoordinates.clear();
    }

    // Version Tracking \\

    public boolean needsMerge(long coordinate, long contentVersion) {
        return !batchedChunks.containsKey(coordinate) || mergedChunkVersions.get(coordinate) != contentVersion;
    }

    public void recordMergedVersion(long coordinate, long contentVersion) {
        mergedChunkVersions.put(coordinate, contentVersion);
    }

    // Accessible \\

    public boolean isReadyToRender() {
        return mergedCoordinates.size() == megaScale;
    }

    public boolean isEmpty() {
        return batchedChunks.isEmpty();
    }

    public long getMegaChunkCoordinate() {
        return megaChunkCoordinate;
    }

    public Long2ObjectOpenHashMap<ChunkInstance> getBatchedChunks() {
        return batchedChunks;
    }

    public ObjectArrayList<ChunkInstance> getBatchedChunkList() {
        return batchedChunkList;
    }

    public ChunkInstance getBatchedChunk(long chunkCoordinate) {
        return batchedChunks.get(chunkCoordinate);
    }
}