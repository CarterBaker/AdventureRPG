package com.internal.bootstrap.worldpipeline.util;

import java.util.ArrayList;
import java.util.List;

import com.internal.core.engine.HandlePackage;
import com.internal.core.engine.settings.EngineSetting;

public class BlockPaletteHandle extends HandlePackage {

    // internal
    private int chunkSize;
    private int totalBlocks;
    private int maxPaletteSize;

    // Palette mode
    private List<Short> palette;
    private long[] packedData;
    private int bitsPerEntry;

    // Direct mode
    private short[] directData;

    // internal \\

    @Override
    protected void create() {

        // internal
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.totalBlocks = chunkSize * chunkSize * chunkSize;
        this.maxPaletteSize = EngineSetting.BLOCK_PALETTE_THRESHOLD;

        // Palette mode
        this.palette = new ArrayList<>();
        this.bitsPerEntry = 4; // Start with 4 bits per entry

        allocatePackedArray();
    }

    // Gets the block ID at the given position.
    public short getBlock(int x, int y, int z) {

        validateCoordinates(x, y, z);
        int index = getIndex(x, y, z);

        // Direct mode
        if (directData != null)
            return directData[index];

        // Palette mode
        int paletteIndex = readPackedValue(index);
        return palette.get(paletteIndex);
    }

    // Sets the block ID at the given position.
    public void setBlock(int x, int y, int z, short blockId) {
        validateCoordinates(x, y, z);
        int index = getIndex(x, y, z);

        // Direct mode - simple assignment
        if (directData != null) {
            directData[index] = blockId;
            return;
        }

        // Palette mode - find or add to palette
        int paletteIndex = palette.indexOf(blockId);

        if (paletteIndex == -1) {
            // New block type - add to palette
            if (palette.size() >= maxPaletteSize) {
                // Too many unique blocks, convert to direct storage
                convertToDirect();
                directData[index] = blockId;
                return;
            }

            palette.add(blockId);
            paletteIndex = palette.size() - 1;

            // Check if we need more bits per entry
            int neededBits = calculateBitsNeeded(palette.size());
            if (neededBits > bitsPerEntry) {
                expandBits(neededBits);
            }
        }

        writePackedValue(index, paletteIndex);
    }

    /**
     * Fills the entire chunk with a single block type.
     */
    public void fill(short blockId) {
        // Optimize: convert to palette mode with single entry
        palette = new ArrayList<>();
        palette.add(blockId);
        bitsPerEntry = 1;
        allocatePackedArray();
        directData = null;

        // All entries point to palette index 0
        // packedData is already zeroed, so we're done!
    }

    /**
     * Checks if all blocks in the chunk are the same.
     */
    public boolean isUniform() {
        if (directData != null) {
            short first = directData[0];
            for (int i = 1; i < totalBlocks; i++) {
                if (directData[i] != first) {
                    return false;
                }
            }
            return true;
        } else {
            // In palette mode, check if palette has only 1 entry
            // or if all indices point to same palette entry
            if (palette.size() == 1) {
                return true;
            }

            int first = readPackedValue(0);
            for (int i = 1; i < totalBlocks; i++) {
                if (readPackedValue(i) != first) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Gets the first block's ID.
     */
    public short getFirstBlock() {
        return getBlock(0, 0, 0);
    }

    // ==================== INTERNAL METHODS ====================

    private int getIndex(int x, int y, int z) {
        return (y * chunkSize + z) * chunkSize + x;
    }

    private void allocatePackedArray() {
        int longsNeeded = (int) Math.ceil((totalBlocks * (double) bitsPerEntry) / 64.0);
        this.packedData = new long[longsNeeded];
    }

    private int readPackedValue(int index) {
        int startBit = index * bitsPerEntry;
        int longIndex = startBit / 64;
        int bitOffset = startBit % 64;

        long mask = (1L << bitsPerEntry) - 1;
        return (int) ((packedData[longIndex] >>> bitOffset) & mask);
    }

    private void writePackedValue(int index, int value) {
        int startBit = index * bitsPerEntry;
        int longIndex = startBit / 64;
        int bitOffset = startBit % 64;

        long mask = (1L << bitsPerEntry) - 1;
        packedData[longIndex] &= ~(mask << bitOffset);
        packedData[longIndex] |= ((long) value) << bitOffset;
    }

    private int calculateBitsNeeded(int paletteSize) {
        if (paletteSize <= 1)
            return 1;
        return 32 - Integer.numberOfLeadingZeros(paletteSize - 1);
    }

    private void expandBits(int newBitsPerEntry) {
        long[] oldData = packedData;
        int oldBits = bitsPerEntry;

        bitsPerEntry = newBitsPerEntry;
        allocatePackedArray();

        // Copy all values with new bit width
        for (int i = 0; i < totalBlocks; i++) {
            int value = readOldPackedValue(i, oldData, oldBits);
            writePackedValue(i, value);
        }
    }

    private int readOldPackedValue(int index, long[] oldData, int oldBits) {
        int startBit = index * oldBits;
        int longIndex = startBit / 64;
        int bitOffset = startBit % 64;

        long mask = (1L << oldBits) - 1;
        return (int) ((oldData[longIndex] >>> bitOffset) & mask);
    }

    private void convertToDirect() {
        directData = new short[totalBlocks];

        // Copy all blocks to direct storage
        for (int i = 0; i < totalBlocks; i++) {
            int paletteIndex = readPackedValue(i);
            directData[i] = palette.get(paletteIndex);
        }

        // Free palette memory
        palette = null;
        packedData = null;
    }

    private void validateCoordinates(int x, int y, int z) {
        if (x < 0 || x >= chunkSize ||
                y < 0 || y >= chunkSize ||
                z < 0 || z >= chunkSize) {
            throw new IllegalArgumentException(
                    String.format("Coordinates (%d, %d, %d) out of bounds [0, %d)",
                            x, y, z, chunkSize));
        }
    }
}