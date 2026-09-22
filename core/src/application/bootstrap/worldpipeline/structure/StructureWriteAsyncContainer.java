package application.bootstrap.worldpipeline.structure;

import engine.root.AsyncContainerPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.Int2ShortOpenHashMap;

public class StructureWriteAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local set of block overrides every structure and road reaching
     * one chunk column wants written, filled once per chunk by
     * StructureManager.resolveChunk() right after the column's terrain is
     * computed and read by every generateSubChunk() call that follows on
     * the same worker. Sparse — a road through a chunk writes a few hundred
     * cells out of the column's quarter million — and keyed by the packed
     * column-local position. A per-subchunk write count lets generation keep
     * every fast path (known-empty sky, uniform stone) for the subchunks no
     * structure touches and only walk the ones that need it. Later writes
     * overwrite earlier ones, which is what lets a template stamped after a
     * street win at their shared edge.
     */

    private static final int WORLD_HEIGHT_BLOCKS = EngineSetting.WORLD_HEIGHT * EngineSetting.CHUNK_SIZE;

    private Int2ShortOpenHashMap writes;
    private int[] subChunkWriteCounts;

    private long chunkCoordinate;
    private boolean resolved;

    @Override
    protected void create() {
        this.writes = new Int2ShortOpenHashMap();
        this.subChunkWriteCounts = new int[EngineSetting.WORLD_HEIGHT];
        this.resolved = false;
    }

    // Build \\

    public void begin(long chunkCoordinate) {

        writes.clear();
        java.util.Arrays.fill(subChunkWriteCounts, 0);

        this.chunkCoordinate = chunkCoordinate;
        this.resolved = true;
    }

    public void write(int localX, int worldY, int localZ, short blockID) {

        if (localX < 0 || localX >= EngineSetting.CHUNK_SIZE
                || localZ < 0 || localZ >= EngineSetting.CHUNK_SIZE
                || worldY < EngineSetting.STRUCTURE_WORLD_MIN_Y_BLOCKS || worldY >= WORLD_HEIGHT_BLOCKS)
            return;

        int key = pack(localX, worldY, localZ);

        if (!writes.containsKey(key))
            subChunkWriteCounts[worldY / EngineSetting.CHUNK_SIZE]++;

        writes.put(key, blockID);
    }

    // Accessible \\

    public static int pack(int localX, int worldY, int localZ) {
        return (worldY << 8) | (localZ << 4) | localX;
    }

    public static int unpackX(int key) {
        return key & 0xF;
    }

    public static int unpackZ(int key) {
        return (key >> 4) & 0xF;
    }

    public static int unpackY(int key) {
        return key >>> 8;
    }

    public boolean isResolvedFor(long chunkCoordinate) {
        return resolved && this.chunkCoordinate == chunkCoordinate;
    }

    public boolean hasWritesInSubChunk(int subChunkIndex) {
        return subChunkWriteCounts[subChunkIndex] > 0;
    }

    public boolean hasWrites() {
        return !writes.isEmpty();
    }

    public short getWrite(int localX, int worldY, int localZ) {
        return writes.getOrDefault(pack(localX, worldY, localZ), StructureTemplateData.BLOCK_SKIP);
    }
}
