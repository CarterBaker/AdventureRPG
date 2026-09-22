package application.bootstrap.worldpipeline.structure;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class RoadPathStruct extends StructPackage {

    /*
     * A fully planned road — the world network's link between two points of
     * interest, a layout's street, or a dungeon corridor. Points are one
     * block apart along the centreline, each carrying its resolved road
     * height and whether it runs on the ground, over a bridge, or through a
     * tunnel. Coordinates are not wrapped: a road crossing the world seam
     * simply continues past it, and the chunk index below is keyed by the
     * wrapped chunk each point lands in so a lookup from either side of the
     * seam finds it. Every point is indexed into every chunk its road band
     * can reach, so stamping a chunk only ever walks the handful of points
     * that can affect it. Built once, then read concurrently.
     */

    private final RoadData roadData;

    private float[] x;
    private float[] z;
    private int[] y;
    private byte[] segmentType;
    private int count;

    private float minX = Float.MAX_VALUE;
    private float maxX = -Float.MAX_VALUE;
    private float minZ = Float.MAX_VALUE;
    private float maxZ = -Float.MAX_VALUE;

    private Long2ObjectOpenHashMap<IntArrayList> chunk2PointIndices;

    public RoadPathStruct(RoadData roadData, int capacity) {
        this.roadData = roadData;
        int initial = Math.max(capacity, 2);
        this.x = new float[initial];
        this.z = new float[initial];
        this.y = new int[initial];
        this.segmentType = new byte[initial];
    }

    // Build \\

    public void add(float pointX, float pointZ, int pointY, RoadSegmentType type) {

        if (count == x.length) {
            int grown = count * 2;
            x = java.util.Arrays.copyOf(x, grown);
            z = java.util.Arrays.copyOf(z, grown);
            y = java.util.Arrays.copyOf(y, grown);
            segmentType = java.util.Arrays.copyOf(segmentType, grown);
        }

        x[count] = pointX;
        z[count] = pointZ;
        y[count] = pointY;
        segmentType[count] = (byte) type.ordinal();
        count++;

        minX = Math.min(minX, pointX);
        maxX = Math.max(maxX, pointX);
        minZ = Math.min(minZ, pointZ);
        maxZ = Math.max(maxZ, pointZ);
    }

    public void finish(int worldChunksX, int worldChunksZ) {

        int chunkSize = EngineSetting.CHUNK_SIZE;
        float margin = getReachBlocks();

        chunk2PointIndices = new Long2ObjectOpenHashMap<>();

        for (int i = 0; i < count; i++) {

            int lowChunkX = Math.floorDiv((int) Math.floor(x[i] - margin), chunkSize);
            int highChunkX = Math.floorDiv((int) Math.floor(x[i] + margin), chunkSize);
            int lowChunkZ = Math.floorDiv((int) Math.floor(z[i] - margin), chunkSize);
            int highChunkZ = Math.floorDiv((int) Math.floor(z[i] + margin), chunkSize);

            for (int chunkX = lowChunkX; chunkX <= highChunkX; chunkX++) {
                for (int chunkZ = lowChunkZ; chunkZ <= highChunkZ; chunkZ++) {

                    long key = Coordinate2Long.pack(
                            Math.floorMod(chunkX, worldChunksX),
                            Math.floorMod(chunkZ, worldChunksZ));

                    IntArrayList indices = chunk2PointIndices.get(key);

                    if (indices == null) {
                        indices = new IntArrayList();
                        chunk2PointIndices.put(key, indices);
                    }

                    if (indices.isEmpty() || indices.getInt(indices.size() - 1) != i)
                        indices.add(i);
                }
            }
        }
    }

    // Accessible \\

    /*
     * Horizontal reach of the road band from its centreline — half the
     * width plus one block for tunnel walls or bridge rails, plus one more
     * so a column halfway between two points is never missed.
     */
    public float getReachBlocks() {
        return roadData.getHalfWidthBlocks() + 2f;
    }

    public IntArrayList getPointIndicesForChunk(long wrappedChunkCoordinate) {
        return chunk2PointIndices.get(wrappedChunkCoordinate);
    }

    public RoadData getRoadData() {
        return roadData;
    }

    public int getCount() {
        return count;
    }

    public float getX(int index) {
        return x[index];
    }

    public float getZ(int index) {
        return z[index];
    }

    public int getY(int index) {
        return y[index];
    }

    public RoadSegmentType getSegmentType(int index) {
        return RoadSegmentType.VALUES[segmentType[index]];
    }

    public float getMinX() {
        return minX;
    }

    public float getMaxX() {
        return maxX;
    }

    public float getMinZ() {
        return minZ;
    }

    public float getMaxZ() {
        return maxZ;
    }
}
