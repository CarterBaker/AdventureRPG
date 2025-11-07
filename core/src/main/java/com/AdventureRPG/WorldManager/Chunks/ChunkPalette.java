package com.AdventureRPG.WorldManager.Chunks;

import java.util.ArrayList;
import java.util.List;

public final class ChunkPalette {

    private final int size; // number of entries
    private final int maxPaletteSize; // e.g. 512
    private List<Short> palette; // dynamic palette
    private long[] packed; // bit-packed array
    private short[] direct; // fallback: direct shorts
    private int bitsPerEntry;

    public ChunkPalette(int size, int maxPaletteSize) {
        this.size = size;
        this.maxPaletteSize = maxPaletteSize;
        this.palette = new ArrayList<>();
        this.bitsPerEntry = 4; // start small
        allocatePacked();
    }

    private void allocatePacked() {
        int longsNeeded = (int) Math.ceil((size * (double) bitsPerEntry) / 64.0);
        this.packed = new long[longsNeeded];
        this.direct = null;
    }

    private int getIndex(int x, int y, int z, int dim) {
        return (y * dim + z) * dim + x;
    }

    // ---- Public API ----
    public short get(int x, int y, int z, int dim) {
        int index = getIndex(x, y, z, dim);
        if (direct != null) {
            return direct[index];
        }
        int startBit = index * bitsPerEntry;
        int longIndex = startBit >>> 6;
        int bitOffset = startBit & 63;
        long val = (packed[longIndex] >>> bitOffset) & ((1L << bitsPerEntry) - 1);
        return palette.get((int) val);
    }

    public void set(int x, int y, int z, short value, int dim) {
        int index = getIndex(x, y, z, dim);

        if (direct != null) {
            direct[index] = value;
            return;
        }

        int paletteIndex = palette.indexOf(value);
        if (paletteIndex == -1) {
            if (palette.size() >= maxPaletteSize) {
                convertToDirect();
                direct[index] = value;
                return;
            }
            palette.add(value);
            paletteIndex = palette.size() - 1;
            int neededBits = Math.max(1, Integer.SIZE - Integer.numberOfLeadingZeros(palette.size() - 1));
            if (neededBits > bitsPerEntry) {
                bitsPerEntry = neededBits;
                repack();
            }
        }

        int startBit = index * bitsPerEntry;
        int longIndex = startBit >>> 6;
        int bitOffset = startBit & 63;
        packed[longIndex] &= ~(((1L << bitsPerEntry) - 1) << bitOffset);
        packed[longIndex] |= ((long) paletteIndex) << bitOffset;
    }

    private void repack() {
        long[] old = packed;
        allocatePacked();
        for (int i = 0; i < size; i++) {
            short value = palette.get(getRawIndex(i, old));
            setRawIndex(i, palette.indexOf(value));
        }
    }

    private int getRawIndex(int i, long[] arr) {
        int startBit = i * bitsPerEntry;
        int longIndex = startBit >>> 6;
        int bitOffset = startBit & 63;
        return (int) ((arr[longIndex] >>> bitOffset) & ((1L << bitsPerEntry) - 1));
    }

    private void setRawIndex(int i, int val) {
        int startBit = i * bitsPerEntry;
        int longIndex = startBit >>> 6;
        int bitOffset = startBit & 63;
        packed[longIndex] &= ~(((1L << bitsPerEntry) - 1) << bitOffset);
        packed[longIndex] |= ((long) val) << bitOffset;
    }

    private void convertToDirect() {
        direct = new short[size];
        for (int i = 0; i < size; i++) {
            direct[i] = palette.get(getRawIndex(i, packed));
        }
        packed = null;
        palette = null;
    }
}
