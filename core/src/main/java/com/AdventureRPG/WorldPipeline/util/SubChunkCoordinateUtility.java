package com.AdventureRPG.WorldPipeline.util;

import com.AdventureRPG.core.engine.UtilityPackage;
import com.AdventureRPG.core.engine.settings.EngineSetting;

public final class SubChunkCoordinateUtility extends UtilityPackage {

    // Settings
    public static final int BIOME_SIZE;
    public static final int CHUNK_SIZE;

    public static final int BIOME_BOCK_COUNT;
    public static final int CHUNK_BLOCK_COUNT;

    // Bit sizes
    private static final int xBits, yBits, zBits;
    private static final int xMask, yMask, zMask;
    private static final int yShift, zShift;

    // Prepacked data
    private static final int[] packedBiomeCoordinates;
    private static final int[] packedBlockCoordinates;

    static {

        // Settings
        BIOME_SIZE = EngineSetting.BIOME_SIZE;
        CHUNK_SIZE = EngineSetting.CHUNK_SIZE;

        BIOME_BOCK_COUNT = (CHUNK_SIZE / BIOME_SIZE)
                * (CHUNK_SIZE / BIOME_SIZE)
                * (CHUNK_SIZE / BIOME_SIZE);

        CHUNK_BLOCK_COUNT = CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE;

        // Bit sizes
        xBits = bitsNeeded(CHUNK_SIZE * 2);
        yBits = bitsNeeded(CHUNK_SIZE * 2);
        zBits = bitsNeeded(CHUNK_SIZE * 2);

        xMask = (1 << xBits) - 1;
        yMask = (1 << yBits) - 1;
        zMask = (1 << zBits) - 1;

        yShift = xBits;
        zShift = xBits + yBits;

        // Prepacked data
        packedBiomeCoordinates = new int[BIOME_BOCK_COUNT];
        packedBlockCoordinates = new int[CHUNK_BLOCK_COUNT];

        prePackCoordinates();
    }

    private static int bitsNeeded(int max) {
        return 32 - Integer.numberOfLeadingZeros(max - 1);
    }

    private static void prePackCoordinates() {
        int i = 0;

        int biomeAxis = CHUNK_SIZE / BIOME_SIZE;

        for (int x = 0; x < biomeAxis; x++)
            for (int y = 0; y < biomeAxis; y++)
                for (int z = 0; z < biomeAxis; z++)
                    packedBiomeCoordinates[i++] = pack(x, y, z);

        i = 0;

        for (int x = 0; x < CHUNK_SIZE; x++)
            for (int y = 0; y < CHUNK_SIZE; y++)
                for (int z = 0; z < CHUNK_SIZE; z++)
                    packedBlockCoordinates[i++] = pack(x, y, z);
    }

    // Accessible \\

    // Pack / Unpack

    public static int pack(int x, int y, int z) {
        return (x & xMask)
                | ((y & yMask) << yShift)
                | ((z & zMask) << zShift);
    }

    public static int unpackX(int packed) {
        return packed & xMask;
    }

    public static int unpackY(int packed) {
        return (packed >> yShift) & yMask;
    }

    public static int unpackZ(int packed) {
        return (packed >> zShift) & zMask;
    }

    public static int getPackedBiomeCoordinate(int index) {
        return packedBiomeCoordinates[index];
    }

    public static int getPackedBlockCoordinate(int index) {
        return packedBlockCoordinates[index];
    }
}
