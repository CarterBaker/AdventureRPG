package com.internal.bootstrap.worldpipeline.block;

import java.util.ArrayList;
import java.util.List;

import com.internal.bootstrap.worldpipeline.util.Coordinate3Short;
import com.internal.core.engine.HandlePackage;

public class BlockPaletteHandle extends HandlePackage {

    // Internal
    private int paletteSize;
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
        this.paletteSize = paletteSize;
        this.totalBlocks = paletteSize * paletteSize * paletteSize;
        this.maxPaletteSize = paletteThreshold;

        // Palette mode
        this.palette = new ArrayList<>();
        this.bitsPerEntry = 4;

        allocatePackedArray();
    }

    // Utility \\

    private int getBlockIndex(int x, int y, int z) {
        return x + y * paletteSize + z * paletteSize * paletteSize;
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

        // Check if value spans two longs
        if (bitOffset + bitsPerEntry <= 64) {
            return (int) ((packedData[longIndex] >>> bitOffset) & mask);
        } else {
            // Value spans two longs
            int bitsInFirstLong = 64 - bitOffset;
            int bitsInSecondLong = bitsPerEntry - bitsInFirstLong;

            long firstPart = (packedData[longIndex] >>> bitOffset);
            long secondPart = (packedData[longIndex + 1] & ((1L << bitsInSecondLong) - 1)) << bitsInFirstLong;

            return (int) ((firstPart | secondPart) & mask);
        }
    }

    private void writePackedValue(int index, int value) {
        int startBit = index * bitsPerEntry;
        int longIndex = startBit / 64;
        int bitOffset = startBit % 64;

        long mask = (1L << bitsPerEntry) - 1;

        // Check if value spans two longs
        if (bitOffset + bitsPerEntry <= 64) {
            packedData[longIndex] &= ~(mask << bitOffset);
            packedData[longIndex] |= ((long) value & mask) << bitOffset;
        } else {
            // Value spans two longs
            int bitsInFirstLong = 64 - bitOffset;
            int bitsInSecondLong = bitsPerEntry - bitsInFirstLong;

            long firstMask = ((1L << bitsInFirstLong) - 1) << bitOffset;
            long secondMask = (1L << bitsInSecondLong) - 1;

            packedData[longIndex] &= ~firstMask;
            packedData[longIndex] |= ((long) value & ((1L << bitsInFirstLong) - 1)) << bitOffset;

            packedData[longIndex + 1] &= ~secondMask;
            packedData[longIndex + 1] |= ((long) value >>> bitsInFirstLong) & secondMask;
        }
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

    public int getPaletteSize() {
        return paletteSize;
    }

    public short getBlock(int x, int y, int z) {

        int index = getBlockIndex(x, y, z);

        if (directData != null)
            return directData[index];

        int paletteIndex = readPackedValue(index);
        return palette.get(paletteIndex);
    }

    public short getBlock(short xyz) {
        int index = Coordinate3Short.getBlockIndex(xyz);

        if (directData != null)
            return directData[index];

        int paletteIndex = readPackedValue(index);
        return palette.get(paletteIndex);
    }

    public short getBlock(short xyz, int virtualPaletteSize) {

        // Calculate the scale factor between actual and virtual palette sizes
        int scale = paletteSize / virtualPaletteSize;

        // Unpack the coordinates
        int x = Coordinate3Short.unpackX(xyz);
        int y = Coordinate3Short.unpackY(xyz);
        int z = Coordinate3Short.unpackZ(xyz);

        // Scale down to virtual coordinates
        int virtualX = x / scale;
        int virtualY = y / scale;
        int virtualZ = z / scale;

        // Get the block at the virtual position
        return getBlock(virtualX, virtualY, virtualZ);
    }

    public void setBlock(int x, int y, int z, short blockId) {

        int index = getBlockIndex(x, y, z);

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
            if (neededBits > bitsPerEntry)
                expandBits(neededBits);
        }

        writePackedValue(index, paletteIndex);
    }
}