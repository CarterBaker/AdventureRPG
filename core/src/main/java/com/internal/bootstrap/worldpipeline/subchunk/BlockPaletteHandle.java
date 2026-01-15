package com.internal.bootstrap.worldpipeline.subchunk;

import java.util.ArrayList;
import java.util.List;

import com.internal.core.engine.HandlePackage;

public class BlockPaletteHandle extends HandlePackage {

    // Internal
    private int chunkSize;
    private int totalBlocks;
    private int maxPaletteSize;

    // Palette mode
    private List<Short> palette;
    private long[] packedData;
    private int bitsPerEntry;

    private short[] directData;

    // Internal \\

    public void constructor(
            int paletteSize,
            int paletteThreshold) {

        // Internal
        this.chunkSize = paletteSize;
        this.totalBlocks = chunkSize * chunkSize * chunkSize;
        this.maxPaletteSize = paletteThreshold;

        // Palette mode
        this.palette = new ArrayList<>();
        this.bitsPerEntry = 4;

        allocatePackedArray();
    }

    // Utility \\

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

        for (int i = 0; i < totalBlocks; i++) {
            int startBit = i * oldBits;
            int longIndex = startBit / 64;
            int bitOffset = startBit % 64;
            long mask = (1L << oldBits) - 1;
            int value = (int) ((oldData[longIndex] >>> bitOffset) & mask);

            writePackedValue(i, value);
        }
    }

    private void convertToDirect() {
        directData = new short[totalBlocks];

        for (int i = 0; i < totalBlocks; i++) {
            int paletteIndex = readPackedValue(i);
            directData[i] = palette.get(paletteIndex);
        }

        palette = null;
        packedData = null;
    }

    // Accessible \\

    public short getBlock(short xyz) {

        int x = (xyz >> 8) & 0xF;
        int y = (xyz >> 4) & 0xF;
        int z = xyz & 0xF;

        int index = (y * chunkSize + z) * chunkSize + x;

        if (directData != null)
            return directData[index];

        int paletteIndex = readPackedValue(index);
        return palette.get(paletteIndex);
    }

    public short getBlock(int x, int y, int z) {

        int index = (y * chunkSize + z) * chunkSize + x;

        if (directData != null)
            return directData[index];

        int paletteIndex = readPackedValue(index);
        return palette.get(paletteIndex);
    }

    public void setBlock(int x, int y, int z, short blockId) {

        int index = (y * chunkSize + z) * chunkSize + x;

        if (directData != null) {
            directData[index] = blockId;
            return;
        }

        int paletteIndex = palette.indexOf(blockId);

        if (paletteIndex == -1) {
            if (palette.size() >= maxPaletteSize) {
                convertToDirect();
                directData[index] = blockId;
                return;
            }

            palette.add(blockId);
            paletteIndex = palette.size() - 1;

            int neededBits = calculateBitsNeeded(palette.size());
            if (neededBits > bitsPerEntry) {
                expandBits(neededBits);
            }
        }

        writePackedValue(index, paletteIndex);
    }
}