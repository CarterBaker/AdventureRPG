package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.Util.Coordinate3Int;
import com.AdventureRPG.Util.Direction3Int;
import com.AdventureRPG.SettingsSystem.Settings;

public class ChunkCoordinates {

    private final int CHUNK_SIZE;
    private final int WORLD_HEIGHT;
    private final int CHUNK_MASK;

    public final int chunkSize;
    public final int subChunkSize;

    private final int[] baseCoordinates;
    private final int[] subCoordinates;

    // Base \\

    public ChunkCoordinates(Settings settings) {

        this.CHUNK_SIZE = settings.CHUNK_SIZE;
        this.WORLD_HEIGHT = settings.WORLD_HEIGHT;
        this.CHUNK_MASK = CHUNK_SIZE - 1;

        this.chunkSize = CHUNK_SIZE * (CHUNK_SIZE * WORLD_HEIGHT) * CHUNK_SIZE;
        this.subChunkSize = CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE;

        this.baseCoordinates = new int[chunkSize];
        this.subCoordinates = new int[subChunkSize];

        allocateBlockCoordinates();
    }

    private void allocateBlockCoordinates() {

        int index = 0;

        for (int y = 0; y < (CHUNK_SIZE * WORLD_HEIGHT); y++)
            for (int z = 0; z < CHUNK_SIZE; z++)
                for (int x = 0; x < CHUNK_SIZE; x++)
                    baseCoordinates[index++] = pack(x, y, z);

        index = 0;

        for (int y = 0; y < CHUNK_SIZE; y++)
            for (int z = 0; z < CHUNK_SIZE; z++)
                for (int x = 0; x < CHUNK_SIZE; x++)
                    subCoordinates[index++] = pack(x, y, z);
    }

    // Coordinates \\

    public int getCoordinates(int index) {
        return baseCoordinates[index];
    }

    public int getSubCoordinates(int index) {
        return subCoordinates[index];
    }

    public int getSubCoordinates(int index, int subChunk) {

        int xyz = getSubCoordinates(index);

        return multiply(xyz, 1, subChunk * CHUNK_SIZE, 1);
    }

    // Pack coordinates into int
    public int pack(int x, int y, int z) {

        int bits = log2(CHUNK_SIZE);

        return (y << (2 * bits)) | (z << bits) | x;
    }

    private int packAndWrap(int x, int y, int z) {

        x &= CHUNK_MASK;
        z &= CHUNK_MASK;

        int bits = log2(CHUNK_SIZE);

        return (y << (2 * bits)) | (z << bits) | x;
    }

    public long convertToCoordinate3Int(int coordinate) {

        int x = getX(coordinate);
        int y = getY(coordinate);
        int z = getZ(coordinate);

        return Coordinate3Int.pack(x, y, z);
    }

    public int getSubChunk(int y) {
        return Math.min(Math.max(y / CHUNK_SIZE, 0), WORLD_HEIGHT - 1);
    }

    // Extract x
    public int getX(int packed) {
        return packed & CHUNK_MASK;
    }

    // Extract z
    public int getZ(int packed) {

        int bits = log2(CHUNK_SIZE);

        return (packed >> bits) & CHUNK_MASK;
    }

    // Extract y
    public int getY(int packed) {

        int bits = log2(CHUNK_SIZE);

        return (packed >> (2 * bits));
    }

    // Add x, y, z without wrapping
    public int add(int packed, int xi, int yi, int zi) {

        int x = getX(packed) + xi;
        int y = getY(packed) + yi;
        int z = getZ(packed) + zi;

        return pack(x, y, z);
    }

    // Add x y and z and wrap
    public int addAndWrap(int packed, int xi, int yi, int zi) {

        int x = (getX(packed) + xi);
        int y = (getY(packed) + yi);
        int z = (getZ(packed) + zi);

        return packAndWrap(x, y, z);
    }

    // Add direction and wrap
    public int addAndWrap(int packed, Direction3Int dir) {

        int x = (getX(packed) + dir.x);
        int y = (getY(packed) + dir.y);
        int z = (getZ(packed) + dir.z);

        return packAndWrap(x, y, z);
    }

    // Add direction and wrap
    public int addAndWrap(int x, int y, int z, Direction3Int dir) {

        x += dir.x;
        y += dir.y;
        z += dir.z;

        return packAndWrap(x, y, z);
    }

    // Multiply packed coordinates by given factors (no wrap)
    public int multiply(int packed, int mx, int my, int mz) {

        int x = getX(packed) * mx;
        int y = getY(packed) * my;
        int z = getZ(packed) * mz;

        return pack(x, y, z);
    }

    public boolean isAtEdge(int x, int y, int z, Direction3Int dir) {

        if (dir.x != 0)
            return (x == 0 && dir.x < 0) || (x == CHUNK_MASK && dir.x > 0);

        if (dir.y != 0) {
            int maxY = (CHUNK_SIZE * WORLD_HEIGHT) - 1;
            return (y == 0 && dir.y < 0) || (y == maxY && dir.y > 0);
        }

        if (dir.z != 0)
            return (z == 0 && dir.z < 0) || (z == CHUNK_MASK && dir.z > 0);

        return false;
    }

    // Helper to compute log2
    private int log2(int n) {
        return 31 - Integer.numberOfLeadingZeros(n);
    }
}
