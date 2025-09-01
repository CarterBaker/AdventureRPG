package com.AdventureRPG.WorldSystem;

import com.AdventureRPG.Util.Coordinate3Int;
import com.AdventureRPG.Util.Direction3Int;

public class PackedCoordinate3Int {

    // Settings
    private final int BIOME_SIZE;
    private final int CHUNK_SIZE;
    private final int WORLD_HEIGHT;

    public final int biomeSize;
    public final int chunkSize;

    // Bit sizes
    private final int xBits, yBits, zBits;
    private final int xMask, yMask, zMask;
    private final int yShift, zShift;

    // Utility
    private final int[] packedBiomeCoordinates;
    private final int[] packedBlockCoordinates;

    // Base \\

    public PackedCoordinate3Int(WorldSystem worldSystem) {

        // Settings
        this.BIOME_SIZE = worldSystem.settings.BIOME_SIZE;
        this.CHUNK_SIZE = worldSystem.settings.CHUNK_SIZE;
        this.WORLD_HEIGHT = worldSystem.settings.WORLD_HEIGHT;

        this.biomeSize = (CHUNK_SIZE / BIOME_SIZE) * (CHUNK_SIZE / BIOME_SIZE) * (CHUNK_SIZE / BIOME_SIZE);
        this.chunkSize = CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE;

        // Bit sizes
        this.xBits = bitsNeeded(CHUNK_SIZE);
        this.zBits = bitsNeeded(CHUNK_SIZE);
        this.yBits = bitsNeeded(CHUNK_SIZE * WORLD_HEIGHT);

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

    // Accessible \\

    public boolean isOverEdge(int x, int y, int z, Direction3Int dir) {

        if (dir.x != 0)
            return (x == 0 && dir.x < 0) || (x == xMask && dir.x > 0);

        if (dir.y != 0)
            return (y == 0 && dir.y < 0) || (y == yMask && dir.y > 0);

        if (dir.z != 0)
            return (z == 0 && dir.z < 0) || (z == zMask && dir.z > 0);

        return false;
    }

    public int addAndWrapAxis(int axisA, int axisB) {
        return (axisA + axisB) & (CHUNK_SIZE - 1);
    }

    public long addCoordinate3Int(int x, int y, int z, long coordinate3Int) {

        int aX = Coordinate3Int.unpackX(coordinate3Int);
        int aY = Coordinate3Int.unpackY(coordinate3Int);
        int aZ = Coordinate3Int.unpackZ(coordinate3Int);

        int bX = aX + x;
        int bY = aY + y;
        int bZ = aZ + z;

        return Coordinate3Int.pack(bX, bY, bZ);
    }
}
