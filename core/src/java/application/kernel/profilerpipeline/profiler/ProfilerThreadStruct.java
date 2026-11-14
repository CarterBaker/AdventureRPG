package application.kernel.profilerpipeline.profiler;

import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.root.StructPackage;

public class ProfilerThreadStruct extends StructPackage {

    /*
     * The load of one named thread pool, read each frame from the busy time
     * and completed task count its ThreadHandle keeps: milliseconds of work
     * finished per frame, how full its workers ran, and how many tasks it
     * completed and holds in flight.
     */

    // Internal
    private final ThreadHandle threadHandle;

    // Baseline
    private long lastBusyNanos;
    private long lastCompletedTasks;

    // Samples
    private final ProfilerSampleStruct busyMillis;
    private final ProfilerSampleStruct utilization;
    private final ProfilerSampleStruct completedTasks;
    private final ProfilerSampleStruct inFlight;

    // Constructor \\

    public ProfilerThreadStruct(ThreadHandle threadHandle, int historyFrames) {

        // Internal
        this.threadHandle = threadHandle;

        // Baseline
        this.lastBusyNanos = threadHandle.getBusyNanos();
        this.lastCompletedTasks = threadHandle.getCompletedTasks();

        // Samples
        String name = threadHandle.getThreadName();
        this.busyMillis = new ProfilerSampleStruct(name, historyFrames);
        this.utilization = new ProfilerSampleStruct(name, historyFrames);
        this.completedTasks = new ProfilerSampleStruct(name, historyFrames);
        this.inFlight = new ProfilerSampleStruct(name, historyFrames);
    }

    // Frame \\

    public void sample(float frameMillis, float nanosPerMilli) {

        long busyNanos = threadHandle.getBusyNanos();
        long completed = threadHandle.getCompletedTasks();
        float busy = (busyNanos - lastBusyNanos) / nanosPerMilli;
        float capacity = frameMillis * Math.max(1, threadHandle.getThreadSize());

        busyMillis.set(busy);
        utilization.set(capacity > 0f ? Math.min(1f, busy / capacity) : 0f);
        completedTasks.set(completed - lastCompletedTasks);
        inFlight.set(threadHandle.getInFlightCount());

        this.lastBusyNanos = busyNanos;
        this.lastCompletedTasks = completed;
    }

    public void commit(long frameIndex, boolean capturing) {
        busyMillis.commit(frameIndex, capturing);
        utilization.commit(frameIndex, capturing);
        completedTasks.commit(frameIndex, capturing);
        inFlight.commit(frameIndex, capturing);
    }

    public void resetCapture() {
        busyMillis.resetCapture();
        utilization.resetCapture();
        completedTasks.resetCapture();
        inFlight.resetCapture();
    }

    // Accessible \\

    public String getName() {
        return threadHandle.getThreadName();
    }

    public int getThreadSize() {
        return threadHandle.getThreadSize();
    }

    public ProfilerSampleStruct getBusyMillis() {
        return busyMillis;
    }

    public ProfilerSampleStruct getUtilization() {
        return utilization;
    }

    public ProfilerSampleStruct getCompletedTasks() {
        return completedTasks;
    }

    public ProfilerSampleStruct getInFlight() {
        return inFlight;
    }
}
