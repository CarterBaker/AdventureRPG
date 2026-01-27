package com.internal.bootstrap.worldpipeline.block;

import java.util.ArrayList;
import java.util.List;

import com.internal.core.engine.HandlePackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate3Short;

public final class BlockPaletteHandle extends HandlePackage {

    private int chunkSize;
    private int blocksPerCell;
    private int paletteAxisSize;
    private int scaleBits;

    private int totalCells;
    private int maxPaletteSize;

    private List<Short> palette;
    private long[] packedData;
    private int bitsPerEntry;

    private short[] directData;

    // Construction \\

    public void constructor(int paletteAxisSize, int paletteThreshold, short airBlockId) {

        this.chunkSize = EngineSetting.CHUNK_SIZE;

        if (chunkSize % paletteAxisSize != 0)
            throwException("paletteAxisSize must evenly divide CHUNK_SIZE");

        this.blocksPerCell = chunkSize / paletteAxisSize;

        if ((blocksPerCell & (blocksPerCell - 1)) != 0)
            throwException("blocksPerCell must be power-of-two");

        this.paletteAxisSize = paletteAxisSize;
        this.scaleBits = Integer.numberOfTrailingZeros(blocksPerCell);
        this.totalCells = paletteAxisSize * paletteAxisSize * paletteAxisSize;
        this.maxPaletteSize = paletteThreshold;

        this.palette = new ArrayList<>();
        this.palette.add(airBlockId);

        this.bitsPerEntry = 1;
        allocatePackedArray();
    }

    // Internal \\

    private void allocatePackedArray() {
        int longsNeeded = (totalCells * bitsPerEntry + 63) >>> 6;
        this.packedData = new long[longsNeeded];
    }

    private int calculateBitsNeeded(int paletteSize) {
        return Math.max(1, 32 - Integer.numberOfLeadingZeros(paletteSize - 1));
    }

    private int readPackedValue(int index) {
        int startBit = index * bitsPerEntry;
        int longIndex = startBit >>> 6;
        int bitOffset = startBit & 63;
        long mask = (1L << bitsPerEntry) - 1L;

        if (bitOffset + bitsPerEntry <= 64)
            return (int) ((packedData[longIndex] >>> bitOffset) & mask);

        int lowBits = 64 - bitOffset;
        long low = packedData[longIndex] >>> bitOffset;
        long high = packedData[longIndex + 1] & ((1L << (bitsPerEntry - lowBits)) - 1L);

        return (int) ((high << lowBits) | low);
    }

    private void writePackedValue(int index, int value) {
        int startBit = index * bitsPerEntry;
        int longIndex = startBit >>> 6;
        int bitOffset = startBit & 63;
        long mask = (1L << bitsPerEntry) - 1L;

        if (bitOffset + bitsPerEntry <= 64) {
            packedData[longIndex] = (packedData[longIndex] & ~(mask << bitOffset)) | ((long) value << bitOffset);
            return;
        }

        int lowBits = 64 - bitOffset;
        long lowMask = (1L << lowBits) - 1L;
        long highMask = (1L << (bitsPerEntry - lowBits)) - 1L;

        packedData[longIndex] = (packedData[longIndex] & ~(lowMask << bitOffset))
                | (((long) value & lowMask) << bitOffset);
        packedData[longIndex + 1] = (packedData[longIndex + 1] & ~highMask) | ((long) value >>> lowBits);
    }

    private void expandBits(int newBits) {
        long[] oldData = packedData;
        int oldBits = bitsPerEntry;

        bitsPerEntry = newBits;
        allocatePackedArray();

        for (int i = 0; i < totalCells; i++) {
            int value = readPackedValueFrom(oldData, oldBits, i);
            writePackedValue(i, value);
        }
    }

    private static int readPackedValueFrom(long[] data, int bits, int index) {
        int startBit = index * bits;
        int longIndex = startBit >>> 6;
        int bitOffset = startBit & 63;
        long mask = (1L << bits) - 1L;

        if (bitOffset + bits <= 64)
            return (int) ((data[longIndex] >>> bitOffset) & mask);

        int lowBits = 64 - bitOffset;
        long low = data[longIndex] >>> bitOffset;
        long high = data[longIndex + 1] & ((1L << (bits - lowBits)) - 1L);

        return (int) ((high << lowBits) | low);
    }

    private int getCellIndex(short packedXYZ) {
        int x = Coordinate3Short.unpackX(packedXYZ) >> scaleBits;
        int y = Coordinate3Short.unpackY(packedXYZ) >> scaleBits;
        int z = Coordinate3Short.unpackZ(packedXYZ) >> scaleBits;
        return (y * paletteAxisSize + z) * paletteAxisSize + x;
    }

    private void convertToDirect() {
        directData = new short[totalCells];

        for (int i = 0; i < totalCells; i++)
            directData[i] = palette.get(readPackedValue(i));

        palette = null;
        packedData = null;
    }

    // Accessible \\

    public short getBlock(short packedXYZ) {
        int index = getCellIndex(packedXYZ);
        return directData != null ? directData[index] : palette.get(readPackedValue(index));
    }

    public void setBlock(short packedXYZ, short blockId) {
        int index = getCellIndex(packedXYZ);

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

    public short getBlock(int x, int y, int z) {
        return getBlock(Coordinate3Short.pack(x, y, z));
    }

    public void setBlock(int x, int y, int z, short blockId) {
        setBlock(Coordinate3Short.pack(x, y, z), blockId);
    }
}