package application.kernel.profilerpipeline.profiler;

import engine.root.StructPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class ProfilerGpuRingStruct extends StructPackage {

    /*
     * One GL context's timestamp queries, kept in a ring of frame slots so a
     * slot is only read back once the GPU has had several frames to finish
     * it. Each slot holds a start and end query per scope and the record the
     * pair resolves into. A slot grows its queries on demand; they belong to
     * the context that made them and are reused for the ring's lifetime.
     */

    // Slots
    private final int[][] startQueries;
    private final int[][] endQueries;
    private final ProfilerGpuStruct[][] records;
    private final int[] counts;
    private int slot;
    private long frameIndex;

    // Open Scopes — slot entries awaiting their end timestamp
    private final IntArrayList openEntries;

    // Constructor \\

    public ProfilerGpuRingStruct(int slotCount, long frameIndex) {

        // Slots
        this.startQueries = new int[slotCount][0];
        this.endQueries = new int[slotCount][0];
        this.records = new ProfilerGpuStruct[slotCount][0];
        this.counts = new int[slotCount];
        this.frameIndex = frameIndex;

        // Open Scopes
        this.openEntries = new IntArrayList();
    }

    // Slots \\

    public void advance(long frameIndex) {
        this.slot = (slot + 1) % counts.length;
        this.frameIndex = frameIndex;
    }

    public void clearSlot() {
        counts[slot] = 0;
        openEntries.clear();
    }

    public int reserveEntry(ProfilerGpuStruct record) {

        int entry = counts[slot];

        if (entry == records[slot].length)
            return -1;

        records[slot][entry] = record;
        counts[slot] = entry + 1;
        openEntries.add(entry);

        return entry;
    }

    public void grow(int[] newStartQueries, int[] newEndQueries) {

        int capacity = records[slot].length;
        int grownCapacity = capacity + newStartQueries.length;

        startQueries[slot] = append(startQueries[slot], newStartQueries, grownCapacity);
        endQueries[slot] = append(endQueries[slot], newEndQueries, grownCapacity);

        ProfilerGpuStruct[] grownRecords = new ProfilerGpuStruct[grownCapacity];
        System.arraycopy(records[slot], 0, grownRecords, 0, capacity);
        records[slot] = grownRecords;
    }

    private int[] append(int[] existing, int[] extra, int grownCapacity) {

        int[] grown = new int[grownCapacity];
        System.arraycopy(existing, 0, grown, 0, existing.length);
        System.arraycopy(extra, 0, grown, existing.length, extra.length);

        return grown;
    }

    public int closeEntry() {
        return openEntries.isEmpty() ? -1 : openEntries.popInt();
    }

    public int peekEntry() {
        return openEntries.topInt();
    }

    // Accessible \\

    public int getSlot() {
        return slot;
    }

    public int getSlotCount() {
        return counts.length;
    }

    public long getFrameIndex() {
        return frameIndex;
    }

    public int getCount() {
        return counts[slot];
    }

    public int getCapacity() {
        return records[slot].length;
    }

    public int[] getStartQueries() {
        return startQueries[slot];
    }

    public int[] getEndQueries() {
        return endQueries[slot];
    }

    public int[] getStartQueries(int slotIndex) {
        return startQueries[slotIndex];
    }

    public int[] getEndQueries(int slotIndex) {
        return endQueries[slotIndex];
    }

    public ProfilerGpuStruct getRecord(int entry) {
        return records[slot][entry];
    }

    public boolean hasOpenEntries() {
        return !openEntries.isEmpty();
    }
}
