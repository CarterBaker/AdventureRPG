package application.bootstrap.geometrypipeline.quadpacket;

import java.util.Arrays;

import application.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketState;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class QuadPacketInstance extends InstancePackage {

    /*
     * Packed quad records for one subchunk, chunk or mega, one bucket per
     * material, each holding fixed-stride integer records its shader pulls by
     * index. Builders write straight into the buckets under the owner's lock;
     * a merge reserves every source first so each bucket grows at most once,
     * then appends the records shifted by one add on their position word.
     * clear() empties the buckets for a rebuild and keeps their storage, and
     * release() hands the storage back once nothing will read it again. The
     * state is status only, published to the render thread.
     */

    // Internal
    private volatile DynamicPacketState state;

    // Buckets
    private int bucketCount;
    private int[] bucketMaterialIDs;
    private int[] bucketStrides;
    private int[] bucketReserved;
    private IntArrayList[] bucketRecords;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.state = DynamicPacketState.EMPTY;

        // Buckets
        this.bucketMaterialIDs = new int[EngineSetting.QUAD_PACKET_INITIAL_BUCKETS];
        this.bucketStrides = new int[EngineSetting.QUAD_PACKET_INITIAL_BUCKETS];
        this.bucketReserved = new int[EngineSetting.QUAD_PACKET_INITIAL_BUCKETS];
        this.bucketRecords = new IntArrayList[EngineSetting.QUAD_PACKET_INITIAL_BUCKETS];
    }

    // State Management \\

    public void beginGenerating() {
        state = DynamicPacketState.GENERATING;
    }

    public void setReady() {
        state = DynamicPacketState.READY;
    }

    public void unlock() {
        state = DynamicPacketState.EMPTY;
    }

    // Ready when anything was written, empty otherwise
    public void finish() {
        state = hasRecords() ? DynamicPacketState.READY : DynamicPacketState.EMPTY;
    }

    // Records \\

    // The bucket a material's records are written to, opened on its first use
    public IntArrayList getRecords(int materialID, int stride) {
        return bucketRecords[resolveBucket(materialID, stride)];
    }

    private int resolveBucket(int materialID, int stride) {

        for (int i = 0; i < bucketCount; i++) {

            if (bucketMaterialIDs[i] != materialID)
                continue;

            if (bucketStrides[i] != stride)
                throwException("Material " + materialID + " was written with a record stride of " + stride
                        + " after " + bucketStrides[i] + " — every record of one material shares its layout.");

            return i;
        }

        if (bucketCount == bucketRecords.length)
            growBuckets();

        int bucket = bucketCount++;

        bucketMaterialIDs[bucket] = materialID;
        bucketStrides[bucket] = stride;
        bucketReserved[bucket] = 0;

        if (bucketRecords[bucket] == null)
            bucketRecords[bucket] = new IntArrayList();

        return bucket;
    }

    private void growBuckets() {

        int capacity = bucketRecords.length * 2;

        bucketMaterialIDs = Arrays.copyOf(bucketMaterialIDs, capacity);
        bucketStrides = Arrays.copyOf(bucketStrides, capacity);
        bucketReserved = Arrays.copyOf(bucketReserved, capacity);
        bucketRecords = Arrays.copyOf(bucketRecords, capacity);
    }

    // Merge \\

    // A source's records counted against this packet's buckets, so commitReserve() grows each at most once
    public void reserve(QuadPacketInstance source) {

        for (int i = 0; i < source.bucketCount; i++) {

            int size = source.bucketRecords[i].size();

            if (size != 0)
                bucketReserved[resolveBucket(source.bucketMaterialIDs[i], source.bucketStrides[i])] += size;
        }
    }

    public void commitReserve() {

        for (int i = 0; i < bucketCount; i++) {

            if (bucketReserved[i] == 0)
                continue;

            IntArrayList records = bucketRecords[i];
            records.ensureCapacity(records.size() + bucketReserved[i]);
            bucketReserved[i] = 0;
        }
    }

    // Every record of the source appended, its position word shifted by an offset packed like a position
    public void merge(QuadPacketInstance source, int positionOffset) {

        for (int i = 0; i < source.bucketCount; i++) {

            IntArrayList from = source.bucketRecords[i];
            int size = from.size();

            if (size == 0)
                continue;

            int stride = source.bucketStrides[i];
            IntArrayList to = bucketRecords[resolveBucket(source.bucketMaterialIDs[i], stride)];
            int start = to.size();

            to.addElements(start, from.elements(), 0, size);

            if (positionOffset == 0)
                continue;

            int[] elements = to.elements();
            int end = start + size;

            for (int record = start; record < end; record += stride)
                elements[record] += positionOffset;
        }
    }

    // Reset \\

    public void clear() {

        for (int i = 0; i < bucketCount; i++)
            bucketRecords[i].clear();

        unlock();
    }

    public void release() {

        for (int i = 0; i < bucketCount; i++) {
            bucketRecords[i].clear();
            bucketRecords[i].trim();
        }

        unlock();
    }

    // Accessible \\

    public DynamicPacketState getState() {
        return state;
    }

    public boolean hasRecords() {

        for (int i = 0; i < bucketCount; i++)
            if (!bucketRecords[i].isEmpty())
                return true;

        return false;
    }

    public int getBucketCount() {
        return bucketCount;
    }

    public int getBucketMaterialID(int bucket) {
        return bucketMaterialIDs[bucket];
    }

    public int getBucketStride(int bucket) {
        return bucketStrides[bucket];
    }

    public IntArrayList getBucketRecords(int bucket) {
        return bucketRecords[bucket];
    }
}
