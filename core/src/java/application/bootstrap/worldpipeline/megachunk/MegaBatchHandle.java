package application.bootstrap.worldpipeline.megachunk;

import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import engine.root.HandlePackage;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MegaBatchHandle extends HandlePackage {

    /*
     * Tracks a mega's registered and merged chunks and the content version of
     * each chunk's contribution, so needsMerge() only triggers a full re-merge
     * when a chunk's geometry actually changed. Owned for the pooled mega's
     * lifetime and reset on reuse.
     */

    // Internal
    private long megaChunkCoordinate;
    private int megaScale;
    private Long2ObjectOpenHashMap<ChunkInstance> batchedChunks;
    private ObjectArrayList<ChunkInstance> batchedChunkList;
    private LongOpenHashSet mergedCoordinates;
    private Long2LongOpenHashMap mergedChunkVersions;

    // Internal \\

    @Override
    protected void create() {
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