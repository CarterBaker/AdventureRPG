package program.bootstrap.worldpipeline.megachunk;

import program.bootstrap.worldpipeline.chunk.ChunkInstance;
import program.core.engine.StructPackage;
import program.core.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MegaBatchStruct extends StructPackage {

    /*
     * Tracks which chunks have been registered and which have been successfully
     * merged into the mega's geometry. mergedCoordinates drives readiness —
     * when its size reaches megaScale all contributions are present and the
     * mega is ready for GPU upload. On re-merge, mergedCoordinates is cleared
     * and rebuilt so the count stays accurate after a full geometry rebuild.
     * batchedChunkList mirrors batchedChunks for zero-allocation hot-path
     * iteration — both are always kept in sync.
     */

    // Internal
    private long megaChunkCoordinate;
    private int megaScale;
    private final Long2ObjectOpenHashMap<ChunkInstance> batchedChunks;
    private final ObjectArrayList<ChunkInstance> batchedChunkList;
    private final LongOpenHashSet mergedCoordinates;

    // Constructor \\

    public MegaBatchStruct() {
        this.batchedChunks = new Long2ObjectOpenHashMap<>();
        this.batchedChunkList = new ObjectArrayList<>();
        this.mergedCoordinates = new LongOpenHashSet();
    }

    public void constructor(long megaChunkCoordinate, int megaScale) {
        this.megaChunkCoordinate = megaChunkCoordinate;
        this.megaScale = megaScale;
        this.batchedChunks.clear();
        this.batchedChunkList.clear();
        this.mergedCoordinates.clear();
    }

    // Reset \\

    public void reset() {
        batchedChunks.clear();
        batchedChunkList.clear();
        mergedCoordinates.clear();
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