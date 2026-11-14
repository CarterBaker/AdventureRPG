package application.kernel.profilerpipeline.profilermanager;

import application.kernel.profilerpipeline.profiler.ProfilerGpuRingStruct;
import application.kernel.profilerpipeline.profiler.ProfilerGpuStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

class GpuTimerBranch extends BranchPackage {

    /*
     * Times GPU work with timestamp queries, one ring per GL context since
     * query objects never cross contexts. The first scope a context opens in
     * a frame advances its ring and reads back the slot written a full ring
     * ago, so the CPU never waits on the GPU; a slot the GPU has still not
     * finished is dropped rather than stalled on. Scopes nest freely, and a
     * draw is counted against the innermost open scope.
     */

    // Palette
    private Int2ObjectOpenHashMap<ProfilerGpuRingStruct> windowID2GpuRing;
    private Object2ObjectLinkedOpenHashMap<String, ProfilerGpuStruct> label2GpuRecord;
    private Object2ObjectOpenHashMap<String, String> passName2Label;

    // Settings
    private int slotCount;
    private int queryGrowth;
    private int historyFrames;
    private double nanosPerMilli;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.windowID2GpuRing = new Int2ObjectOpenHashMap<>();
        this.label2GpuRecord = new Object2ObjectLinkedOpenHashMap<>();
        this.passName2Label = new Object2ObjectOpenHashMap<>();

        // Settings
        this.slotCount = EngineSetting.PROFILER_GPU_SLOT_COUNT;
        this.queryGrowth = EngineSetting.PROFILER_GPU_QUERY_GROWTH;
        this.historyFrames = EngineSetting.PROFILER_HISTORY_FRAMES;
        this.nanosPerMilli = EngineSetting.NANOS_PER_MILLI;
    }

    // Scopes \\

    void beginScope(int windowID, long frameIndex, String label) {

        ProfilerGpuRingStruct ring = resolveRing(windowID, frameIndex);
        ProfilerGpuStruct record = resolveRecord(label);
        int entry = ring.reserveEntry(record);

        if (entry < 0) {
            ring.grow(
                    ProfilerGLSLUtility.generateQueries(Math.max(queryGrowth, ring.getCapacity())),
                    ProfilerGLSLUtility.generateQueries(Math.max(queryGrowth, ring.getCapacity())));
            entry = ring.reserveEntry(record);
        }

        ProfilerGLSLUtility.stampTimestamp(ring.getStartQueries()[entry]);
    }

    void endScope(int windowID) {

        ProfilerGpuRingStruct ring = windowID2GpuRing.get(windowID);

        if (ring == null)
            return;

        int entry = ring.closeEntry();

        if (entry >= 0)
            ProfilerGLSLUtility.stampTimestamp(ring.getEndQueries()[entry]);
    }

    void recordDraw(int windowID, int triangles) {

        ProfilerGpuRingStruct ring = windowID2GpuRing.get(windowID);

        if (ring == null || !ring.hasOpenEntries())
            return;

        ProfilerGpuStruct record = ring.getRecord(ring.peekEntry());
        record.getDrawCalls().accumulate(1f);
        record.getTriangles().accumulate(triangles);
    }

    String resolvePassLabel(String passName) {

        String label = passName2Label.get(passName);

        if (label == null) {
            label = EngineSetting.PROFILER_GPU_PASS_PREFIX + passName;
            passName2Label.put(passName, label);
        }

        return label;
    }

    // Ring \\

    private ProfilerGpuRingStruct resolveRing(int windowID, long frameIndex) {

        ProfilerGpuRingStruct ring = windowID2GpuRing.get(windowID);

        if (ring == null) {
            ring = new ProfilerGpuRingStruct(slotCount, frameIndex);
            windowID2GpuRing.put(windowID, ring);
            return ring;
        }

        if (ring.getFrameIndex() != frameIndex) {
            ring.advance(frameIndex);
            resolveSlot(ring);
            ring.clearSlot();
        }

        return ring;
    }

    private void resolveSlot(ProfilerGpuRingStruct ring) {

        int count = ring.getCount();

        if (count == 0)
            return;

        int[] startQueries = ring.getStartQueries();
        int[] endQueries = ring.getEndQueries();

        if (!ProfilerGLSLUtility.isResultAvailable(endQueries[count - 1]))
            return;

        for (int i = 0; i < count; i++) {

            long elapsed = ProfilerGLSLUtility.readTimestamp(endQueries[i])
                    - ProfilerGLSLUtility.readTimestamp(startQueries[i]);

            if (elapsed > 0L)
                ring.getRecord(i).getMillis().accumulate((float) (elapsed / nanosPerMilli));
        }
    }

    private ProfilerGpuStruct resolveRecord(String label) {

        ProfilerGpuStruct record = label2GpuRecord.get(label);

        if (record == null) {
            record = new ProfilerGpuStruct(label, historyFrames);
            label2GpuRecord.put(label, record);
        }

        return record;
    }

    // Window \\

    void removeWindow(int windowID) {

        ProfilerGpuRingStruct ring = windowID2GpuRing.remove(windowID);

        if (ring == null)
            return;

        for (int i = 0; i < ring.getSlotCount(); i++) {
            ProfilerGLSLUtility.deleteQueries(ring.getStartQueries(i));
            ProfilerGLSLUtility.deleteQueries(ring.getEndQueries(i));
        }
    }

    // Frame \\

    void commit(long frameIndex, boolean capturing) {

        ObjectIterator<Object2ObjectMap.Entry<String, ProfilerGpuStruct>> iterator = label2GpuRecord
                .object2ObjectEntrySet()
                .fastIterator();

        while (iterator.hasNext()) {

            ProfilerGpuStruct record = iterator.next().getValue();
            record.commit(frameIndex, capturing);

            if (frameIndex - record.getMillis().getLastActiveFrame() > historyFrames
                    && frameIndex - record.getDrawCalls().getLastActiveFrame() > historyFrames)
                iterator.remove();
        }
    }

    void resetCapture() {
        for (ProfilerGpuStruct record : label2GpuRecord.values())
            record.resetCapture();
    }

    // Accessible \\

    ObjectCollection<ProfilerGpuStruct> getRecords() {
        return label2GpuRecord.values();
    }
}
