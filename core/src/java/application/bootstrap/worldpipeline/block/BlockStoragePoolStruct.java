package application.bootstrap.worldpipeline.block;

import java.util.Arrays;
import java.util.BitSet;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.Short2IntOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class BlockStoragePoolStruct extends StructPackage {

    /*
     * The free list every block palette takes its storage from and gives it
     * back to. A subchunk releases its palettes whenever it collapses to a
     * uniform fill, unloads or generates again, so its arrays return here
     * instead of to the collector and the next subchunk to generate takes
     * them as they were when new: zeroed, cleared and empty. Arrays are kept
     * by length, at most BLOCK_STORAGE_POOL_CAPACITY of each kind and length,
     * and every call is synchronized, since generation, ticking and unloading
     * release from different threads.
     */

    // Arrays
    private final Int2ObjectOpenHashMap<ObjectArrayList<long[]>> length2Words;
    private final Int2ObjectOpenHashMap<ObjectArrayList<short[]>> length2Shorts;
    private final Int2ObjectOpenHashMap<ObjectArrayList<byte[]>> length2Bytes;

    // Collections
    private final ObjectArrayList<BitSet> bitSets;
    private final ObjectArrayList<ShortArrayList> paletteLists;
    private final ObjectArrayList<Short2IntOpenHashMap> paletteIndexes;

    // Constructor \\

    public BlockStoragePoolStruct() {

        // Arrays
        this.length2Words = new Int2ObjectOpenHashMap<>();
        this.length2Shorts = new Int2ObjectOpenHashMap<>();
        this.length2Bytes = new Int2ObjectOpenHashMap<>();

        // Collections
        this.bitSets = new ObjectArrayList<>();
        this.paletteLists = new ObjectArrayList<>();
        this.paletteIndexes = new ObjectArrayList<>();
    }

    // Words \\

    synchronized long[] takeWords(int length) {

        ObjectArrayList<long[]> free = length2Words.get(length);

        return free == null || free.isEmpty() ? new long[length] : free.pop();
    }

    synchronized void giveWords(long[] words) {

        if (words == null)
            return;

        Arrays.fill(words, 0L);
        keep(freeList(length2Words, words.length), words);
    }

    // Shorts \\

    synchronized short[] takeShorts(int length) {

        ObjectArrayList<short[]> free = length2Shorts.get(length);

        return free == null || free.isEmpty() ? new short[length] : free.pop();
    }

    synchronized void giveShorts(short[] shorts) {

        if (shorts == null)
            return;

        Arrays.fill(shorts, (short) 0);
        keep(freeList(length2Shorts, shorts.length), shorts);
    }

    // Bytes \\

    synchronized byte[] takeBytes(int length) {

        ObjectArrayList<byte[]> free = length2Bytes.get(length);

        return free == null || free.isEmpty() ? new byte[length] : free.pop();
    }

    synchronized void giveBytes(byte[] bytes) {

        if (bytes == null)
            return;

        Arrays.fill(bytes, (byte) 0);
        keep(freeList(length2Bytes, bytes.length), bytes);
    }

    // Bit Sets \\

    synchronized BitSet takeBits(int totalCells) {
        return bitSets.isEmpty() ? new BitSet(totalCells) : bitSets.pop();
    }

    synchronized void giveBits(BitSet bits) {

        if (bits == null)
            return;

        bits.clear();
        keep(bitSets, bits);
    }

    // Palette Lists \\

    synchronized ShortArrayList takePaletteList() {
        return paletteLists.isEmpty() ? new ShortArrayList() : paletteLists.pop();
    }

    synchronized void givePaletteList(ShortArrayList paletteList) {

        if (paletteList == null)
            return;

        paletteList.clear();
        keep(paletteLists, paletteList);
    }

    // The palette sets the index's miss value itself on every take
    synchronized Short2IntOpenHashMap takePaletteIndex() {
        return paletteIndexes.isEmpty() ? new Short2IntOpenHashMap() : paletteIndexes.pop();
    }

    synchronized void givePaletteIndex(Short2IntOpenHashMap paletteIndex) {

        if (paletteIndex == null)
            return;

        paletteIndex.clear();
        keep(paletteIndexes, paletteIndex);
    }

    // Utility \\

    private static <T> ObjectArrayList<T> freeList(Int2ObjectOpenHashMap<ObjectArrayList<T>> length2Free, int length) {

        ObjectArrayList<T> free = length2Free.get(length);

        if (free == null) {
            free = new ObjectArrayList<>();
            length2Free.put(length, free);
        }

        return free;
    }

    private static <T> void keep(ObjectArrayList<T> free, T storage) {
        if (free.size() < EngineSetting.BLOCK_STORAGE_POOL_CAPACITY)
            free.push(storage);
    }
}
