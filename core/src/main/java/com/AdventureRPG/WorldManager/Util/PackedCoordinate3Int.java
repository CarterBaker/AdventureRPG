package com.AdventureRPG.WorldManager.Util;

import com.AdventureRPG.Core.Bootstrap.SystemFrame;
import com.AdventureRPG.Core.RenderPipeline.Util.GlobalConstant;

public class PackedCoordinate3Int extends SystemFrame {

    // Settings
    private int BIOME_SIZE;
    private int CHUNK_SIZE;

    public int biomeSize;
    public int chunkSize;

    // Bit sizes
    private int xBits, yBits, zBits;
    private int xMask, yMask, zMask;
    private int yShift, zShift;

    // Utility
    private int[] packedBiomeCoordinates;
    private int[] packedBlockCoordinates;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.BIOME_SIZE = GlobalConstant.BIOME_SIZE;
        this.CHUNK_SIZE = GlobalConstant.CHUNK_SIZE;

        this.biomeSize = (CHUNK_SIZE / BIOME_SIZE) *
                (CHUNK_SIZE / BIOME_SIZE) *
                (CHUNK_SIZE / BIOME_SIZE);
        this.chunkSize = CHUNK_SIZE *
                CHUNK_SIZE *
                CHUNK_SIZE;

        // Bit sizes
        this.xBits = bitsNeeded(CHUNK_SIZE * 2);
        this.yBits = bitsNeeded(CHUNK_SIZE * 2);
        this.zBits = bitsNeeded(CHUNK_SIZE * 2);

        this.xMask = (1 << xBits) - 1;
        this.yMask = (1 << yBits) - 1;
        this.zMask = (1 << zBits) - 1;

        this.yShift = xBits;
        this.zShift = xBits + yBits;

        // Utility
        this.packedBiomeCoordinates = new int[biomeSize];
        this.packedBlockCoordinates = new int[chunkSize];

        prePackCoordinates();
    }

    private int bitsNeeded(int max) {
        return 32 - Integer.numberOfLeadingZeros(max - 1);
    }

    private void prePackCoordinates() {

        int i = 0;

        for (int x = 0; x < (CHUNK_SIZE / BIOME_SIZE); x++)

            for (int y = 0; y < (CHUNK_SIZE / BIOME_SIZE); y++)

                for (int z = 0; z < (CHUNK_SIZE / BIOME_SIZE); z++) {

                    packedBiomeCoordinates[i] = pack(x, y, z);
                    i++;
                }

        i = 0;

        for (int x = 0; x < CHUNK_SIZE; x++)

            for (int y = 0; y < CHUNK_SIZE; y++)

                for (int z = 0; z < CHUNK_SIZE; z++) {

                    packedBlockCoordinates[i] = pack(x, y, z);
                    i++;
                }
    }

    // Pack \\

    public int pack(int x, int y, int z) {
        return (x & xMask) | ((y & yMask) << yShift) | ((z & zMask) << zShift);
    }

    // Unpack \\

    public int unpackX(int packed) {
        return packed & xMask;
    }

    public int unpackY(int packed) {
        return (packed >> yShift) & yMask;
    }

    public int unpackZ(int packed) {
        return (packed >> zShift) & zMask;
    }

    // Utility \\

    public int getPackedBiomeCoordinates(int index) {
        return packedBiomeCoordinates[index];
    }

    public int getPackedBlockCoordinate(int index) {
        return packedBlockCoordinates[index];
    }
}
