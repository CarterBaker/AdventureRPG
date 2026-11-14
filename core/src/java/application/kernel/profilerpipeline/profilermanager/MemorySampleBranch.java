package application.kernel.profilerpipeline.profilermanager;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

import application.kernel.profilerpipeline.profiler.ProfilerSampleStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class MemorySampleBranch extends BranchPackage {

    /*
     * Reads the Java heap and the garbage collectors once per frame: heap in
     * use and committed, and the collections and collection time that landed
     * inside the frame. Allocation churn shows up here long before it shows
     * up as a hitch.
     */

    // Internal
    private Runtime runtime;
    private List<GarbageCollectorMXBean> garbageCollectors;

    // Baseline
    private long lastCollections;
    private long lastCollectionMillis;

    // Samples
    private ProfilerSampleStruct heapUsed;
    private ProfilerSampleStruct heapCommitted;
    private ProfilerSampleStruct collections;
    private ProfilerSampleStruct collectionMillis;

    // Settings
    private float bytesPerMegabyte;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.runtime = Runtime.getRuntime();
        this.garbageCollectors = ManagementFactory.getGarbageCollectorMXBeans();

        // Samples
        int historyFrames = EngineSetting.PROFILER_HISTORY_FRAMES;
        this.heapUsed = new ProfilerSampleStruct(EngineSetting.PROFILER_MEMORY_HEAP_USED, historyFrames);
        this.heapCommitted = new ProfilerSampleStruct(EngineSetting.PROFILER_MEMORY_HEAP_COMMITTED, historyFrames);
        this.collections = new ProfilerSampleStruct(EngineSetting.PROFILER_MEMORY_GC_COLLECTIONS, historyFrames);
        this.collectionMillis = new ProfilerSampleStruct(EngineSetting.PROFILER_MEMORY_GC_MILLIS, historyFrames);

        // Settings
        this.bytesPerMegabyte = EngineSetting.BYTES_PER_MEGABYTE;

        // Baseline
        this.lastCollections = countCollections();
        this.lastCollectionMillis = sumCollectionMillis();
    }

    // Frame \\

    void sample() {

        long totalCollections = countCollections();
        long totalCollectionMillis = sumCollectionMillis();

        heapUsed.set((runtime.totalMemory() - runtime.freeMemory()) / bytesPerMegabyte);
        heapCommitted.set(runtime.totalMemory() / bytesPerMegabyte);
        collections.set(totalCollections - lastCollections);
        collectionMillis.set(totalCollectionMillis - lastCollectionMillis);

        this.lastCollections = totalCollections;
        this.lastCollectionMillis = totalCollectionMillis;
    }

    private long countCollections() {

        long total = 0L;

        for (int i = 0; i < garbageCollectors.size(); i++)
            total += Math.max(0L, garbageCollectors.get(i).getCollectionCount());

        return total;
    }

    private long sumCollectionMillis() {

        long total = 0L;

        for (int i = 0; i < garbageCollectors.size(); i++)
            total += Math.max(0L, garbageCollectors.get(i).getCollectionTime());

        return total;
    }

    void commit(long frameIndex, boolean capturing) {
        heapUsed.commit(frameIndex, capturing);
        heapCommitted.commit(frameIndex, capturing);
        collections.commit(frameIndex, capturing);
        collectionMillis.commit(frameIndex, capturing);
    }

    void resetCapture() {
        heapUsed.resetCapture();
        heapCommitted.resetCapture();
        collections.resetCapture();
        collectionMillis.resetCapture();
    }

    // Accessible \\

    ProfilerSampleStruct getHeapUsed() {
        return heapUsed;
    }

    ProfilerSampleStruct getHeapCommitted() {
        return heapCommitted;
    }

    ProfilerSampleStruct getCollections() {
        return collections;
    }

    ProfilerSampleStruct getCollectionMillis() {
        return collectionMillis;
    }
}
