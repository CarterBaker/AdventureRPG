package application.kernel.profilerpipeline.profilermanager;

import application.kernel.profilerpipeline.profiler.ProfilerThreadStruct;
import application.kernel.threadpipeline.thread.ThreadHandle;
import application.kernel.threadpipeline.threadmanager.ThreadManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;

class ThreadSampleBranch extends BranchPackage {

    /*
     * Reads every thread pool's load once per frame. Pools load on demand, so
     * a pool first seen here starts its record from its current counters and
     * only work finished after that point is measured.
     */

    // Internal
    private ThreadManager threadManager;

    // Palette
    private Reference2ObjectLinkedOpenHashMap<ThreadHandle, ProfilerThreadStruct> threadHandle2ProfilerThread;

    // Settings
    private int historyFrames;
    private float nanosPerMilli;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.threadHandle2ProfilerThread = new Reference2ObjectLinkedOpenHashMap<>();

        // Settings
        this.historyFrames = EngineSetting.PROFILER_HISTORY_FRAMES;
        this.nanosPerMilli = EngineSetting.NANOS_PER_MILLI;
    }

    @Override
    protected void get() {
        this.threadManager = get(ThreadManager.class);
    }

    // Frame \\

    void sample(float frameMillis) {

        for (ThreadHandle threadHandle : threadManager.getThreadHandles()) {

            ProfilerThreadStruct profilerThread = threadHandle2ProfilerThread.get(threadHandle);

            if (profilerThread == null) {
                profilerThread = new ProfilerThreadStruct(threadHandle, historyFrames);
                threadHandle2ProfilerThread.put(threadHandle, profilerThread);
            }

            profilerThread.sample(frameMillis, nanosPerMilli);
        }
    }

    void commit(long frameIndex, boolean capturing) {
        for (ProfilerThreadStruct profilerThread : threadHandle2ProfilerThread.values())
            profilerThread.commit(frameIndex, capturing);
    }

    void resetCapture() {
        for (ProfilerThreadStruct profilerThread : threadHandle2ProfilerThread.values())
            profilerThread.resetCapture();
    }

    // Accessible \\

    ObjectCollection<ProfilerThreadStruct> getThreads() {
        return threadHandle2ProfilerThread.values();
    }
}
