package com.AdventureRPG.WorldSystem.Blocks;

import com.AdventureRPG.Util.Direction3Int;

public class BlockCoordinateEncryptor {

    private final int CHUNK_SIZE;
    private final int CHUNK_MASK;

    private final int chunkSize;
    private final int subChunkSize;

    public final int[] baseCoordinates;
    public final int[] subCoordinates;

    public BlockCoordinateEncryptor(Settings settings) {

        this.CHUNK_SIZE = CHUNK_SIZE;
        this.CHUNK_MASK = CHUNK_SIZE - 1;

        this.chunkSize = CHUNK_SIZE * (CHUNK_SIZE) * CHUNK_SIZE;
        this.subChunkSize = CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE;

        this.baseCoordinates = new int[];
        this.subCoordinates = new int[subChunkSize];

        allocateBlockCoordinates();
    }

    private void allocateBlockCoordinates() {

        int index = 0;

        for (int y = 0; y < CHUNK_SIZE; y++)
            for (int z = 0; z < CHUNK_SIZE; z++)
                for (int x = 0; x < CHUNK_SIZE; x++)
                    subCoordinates[index++] = pack(x, y, z);
    }

    // Pack coordinates into int
    public int pack(int x, int y, int z) {

        int bits = log2(CHUNK_SIZE);

        return (y << (2 * bits)) | (z << bits) | x;
    }

    private int packAndWrap(int x, int y, int z) {

        x &= CHUNK_MASK;
        y &= CHUNK_MASK;
        z &= CHUNK_MASK;

        int bits = log2(CHUNK_SIZE);

        return (y << (2 * bits)) | (z << bits) | x;
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

        return (packed >> (2 * bits)) & CHUNK_MASK;
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

        int x = (getX(packed) + xi) & CHUNK_MASK;
        int y = (getY(packed) + yi) & CHUNK_MASK;
        int z = (getZ(packed) + zi) & CHUNK_MASK;

        return packAndWrap(x, y, z);
    }

    // Add direction and wrap
    public int addAndWrap(int packed, Direction3Int dir) {

        int x = (getX(packed) + dir.x) & CHUNK_MASK;
        int y = (getY(packed) + dir.y) & CHUNK_MASK;
        int z = (getZ(packed) + dir.z) & CHUNK_MASK;

        return packAndWrap(x, y, z);
    }

    // Multiply packed coordinates by given factors (no wrap)
    public int multiply(int packed, int mx, int my, int mz) {

        int x = getX(packed) * mx;
        int y = getY(packed) * my;
        int z = getZ(packed) * mz;

        return pack(x, y, z);
    }

    // Helper to compute log2
    private int log2(int n) {
        return 31 - Integer.numberOfLeadingZeros(n);
    }
}
