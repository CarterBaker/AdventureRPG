package application.kernel.profilerpipeline.profilermanager;

import java.io.File;

import application.kernel.profilerpipeline.profiler.ProfilerGpuStruct;
import application.kernel.profilerpipeline.profiler.ProfilerPhase;
import application.kernel.profilerpipeline.profiler.ProfilerSampleStruct;
import application.kernel.profilerpipeline.profiler.ProfilerSystemStruct;
import application.kernel.profilerpipeline.profiler.ProfilerThreadStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.root.ManagerPackage;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;

public class ProfilerManager extends ManagerPackage {

    /*
     * The engine profiler. Frame time and the split across the frame's phases
     * are always measured, since they cost two clock reads a phase. Per-system
     * CPU time, GPU timestamps, thread pool load, the heap and the counters
     * are only sampled while something holds sampling open, a profiler panel
     * or a running capture, so an unwatched engine pays nothing for them. A
     * capture measures a fixed number of frames and writes a report.
     */

    // Branches
    private SystemSampleBranch systemSampleBranch;
    private GpuTimerBranch gpuTimerBranch;
    private ThreadSampleBranch threadSampleBranch;
    private MemorySampleBranch memorySampleBranch;
    private ProfilerReportBranch profilerReportBranch;

    // Frame
    private ProfilerSampleStruct frameSample;
    private ProfilerSampleStruct[] phaseSamples;
    private long frameIndex;
    private long frameStartNanos;
    private long phaseStartNanos;

    // Palette
    private Object2ObjectLinkedOpenHashMap<String, ProfilerSampleStruct> counterName2Sample;

    // Sampling
    private int samplingHolds;

    // Capture
    private int captureFramesRemaining;
    private FloatArrayList captureFrameMillis;
    private File lastReportFile;

    // Settings
    private int historyFrames;
    private float nanosPerMilli;

    // Base \\

    @Override
    protected void create() {

        // Branches
        this.systemSampleBranch = create(SystemSampleBranch.class);
        this.gpuTimerBranch = create(GpuTimerBranch.class);
        this.threadSampleBranch = create(ThreadSampleBranch.class);
        this.memorySampleBranch = create(MemorySampleBranch.class);
        this.profilerReportBranch = create(ProfilerReportBranch.class);

        // Settings
        this.historyFrames = EngineSetting.PROFILER_HISTORY_FRAMES;
        this.nanosPerMilli = EngineSetting.NANOS_PER_MILLI;

        // Frame
        this.frameSample = new ProfilerSampleStruct(EngineSetting.PROFILER_FRAME_LABEL, historyFrames);
        this.phaseSamples = new ProfilerSampleStruct[ProfilerPhase.LENGTH];

        for (int i = 0; i < ProfilerPhase.LENGTH; i++)
            this.phaseSamples[i] = new ProfilerSampleStruct(ProfilerPhase.VALUES[i].displayName, historyFrames);

        this.frameStartNanos = EngineSetting.PROFILER_UNSTAMPED;

        // Palette
        this.counterName2Sample = new Object2ObjectLinkedOpenHashMap<>();

        // Capture
        this.captureFrameMillis = new FloatArrayList();

        EngineUtility.assignProfilerManager(this);
    }

    // Sampling \\

    public boolean isSampling() {
        return samplingHolds > 0 || captureFramesRemaining > 0;
    }

    public void acquireSampling() {
        samplingHolds++;
    }

    public void releaseSampling() {

        if (samplingHolds == 0)
            throwException("Profiler sampling was released more times than it was acquired.");

        samplingHolds--;
    }

    // Frame \\

    public void beginFrame() {

        long now = System.nanoTime();

        if (frameStartNanos != EngineSetting.PROFILER_UNSTAMPED) {
            phaseSamples[ProfilerPhase.PACING.ordinal()].accumulate((now - phaseStartNanos) / nanosPerMilli);
            frameSample.accumulate((now - frameStartNanos) / nanosPerMilli);
            commitFrame();
        }

        this.frameStartNanos = now;
        this.phaseStartNanos = now;
    }

    public void markPhase(ProfilerPhase phase) {

        long now = System.nanoTime();

        phaseSamples[phase.ordinal()].accumulate((now - phaseStartNanos) / nanosPerMilli);
        this.phaseStartNanos = now;
    }

    private void commitFrame() {

        boolean capturing = captureFramesRemaining > 0;

        if (capturing)
            captureFrameMillis.add(frameSample.getFrameValue());

        if (isSampling()) {

            threadSampleBranch.sample(frameSample.getFrameValue());
            memorySampleBranch.sample();

            systemSampleBranch.commit(frameIndex, capturing);
            gpuTimerBranch.commit(frameIndex, capturing);
            threadSampleBranch.commit(frameIndex, capturing);
            memorySampleBranch.commit(frameIndex, capturing);

            for (ProfilerSampleStruct counter : counterName2Sample.values())
                counter.commit(frameIndex, capturing);
        }

        frameSample.commit(frameIndex, capturing);

        for (int i = 0; i < phaseSamples.length; i++)
            phaseSamples[i].commit(frameIndex, capturing);

        frameIndex++;

        if (capturing && --captureFramesRemaining == 0)
            finishCapture();
    }

    // Systems \\

    public void recordSystem(SystemPackage system, ProfilerPhase phase, long nanos) {
        systemSampleBranch.recordSystem(system, phase, nanos);
    }

    // GPU \\

    public void beginGpuFrame(WindowInstance window) {
        if (isSampling())
            gpuTimerBranch.beginScope(resolveContextID(window), frameIndex, EngineSetting.PROFILER_GPU_FRAME_LABEL);
    }

    public void beginGpuPass(WindowInstance window, String passName) {
        if (isSampling())
            gpuTimerBranch.beginScope(resolveContextID(window), frameIndex, gpuTimerBranch.resolvePassLabel(passName));
    }

    public void beginGpuBatch(WindowInstance window, String shaderName) {
        if (isSampling())
            gpuTimerBranch.beginScope(resolveContextID(window), frameIndex, shaderName);
    }

    public void endGpuScope(WindowInstance window) {
        if (isSampling())
            gpuTimerBranch.endScope(resolveContextID(window));
    }

    public void recordDraw(WindowInstance window, int triangles) {

        if (!isSampling())
            return;

        gpuTimerBranch.recordDraw(resolveContextID(window), triangles);
        recordCount(EngineSetting.PROFILER_COUNTER_DRAW_CALLS, 1f);
        recordCount(EngineSetting.PROFILER_COUNTER_TRIANGLES, triangles);
    }

    public void removeGpuWindow(WindowInstance window) {
        gpuTimerBranch.removeWindow(resolveContextID(window));
    }

    private int resolveContextID(WindowInstance window) {
        return window.getGLWindow().getWindowID();
    }

    // Counters \\

    public void recordCount(String counterName, float value) {

        if (!isSampling())
            return;

        ProfilerSampleStruct counter = counterName2Sample.get(counterName);

        if (counter == null) {
            counter = new ProfilerSampleStruct(counterName, historyFrames);
            counterName2Sample.put(counterName, counter);
        }

        counter.accumulate(value);
    }

    // Capture \\

    public void beginCapture() {

        if (captureFramesRemaining > 0)
            return;

        frameSample.resetCapture();

        for (int i = 0; i < phaseSamples.length; i++)
            phaseSamples[i].resetCapture();

        systemSampleBranch.resetCapture();
        gpuTimerBranch.resetCapture();
        threadSampleBranch.resetCapture();
        memorySampleBranch.resetCapture();

        for (ProfilerSampleStruct counter : counterName2Sample.values())
            counter.resetCapture();

        captureFrameMillis.clear();
        this.captureFramesRemaining = EngineSetting.PROFILER_CAPTURE_FRAMES;
    }

    private void finishCapture() {

        this.lastReportFile = profilerReportBranch.writeReport(captureFrameMillis);

        if (lastReportFile != null)
            log("Profiler report written to " + lastReportFile.getAbsolutePath());
    }

    // Accessible \\

    public boolean isCapturing() {
        return captureFramesRemaining > 0;
    }

    public int getCaptureFramesRemaining() {
        return captureFramesRemaining;
    }

    public File getLastReportFile() {
        return lastReportFile;
    }

    public long getFrameIndex() {
        return frameIndex;
    }

    public ProfilerSampleStruct getFrameSample() {
        return frameSample;
    }

    public ProfilerSampleStruct getPhaseSample(ProfilerPhase phase) {
        return phaseSamples[phase.ordinal()];
    }

    public ObjectCollection<ProfilerSystemStruct> getSystems() {
        return systemSampleBranch.getSystems();
    }

    public ObjectCollection<ProfilerSampleStruct> getGroups() {
        return systemSampleBranch.getGroups();
    }

    public ObjectCollection<ProfilerGpuStruct> getGpuRecords() {
        return gpuTimerBranch.getRecords();
    }

    public ObjectCollection<ProfilerThreadStruct> getThreads() {
        return threadSampleBranch.getThreads();
    }

    public ObjectCollection<ProfilerSampleStruct> getCounters() {
        return counterName2Sample.values();
    }

    public ProfilerSampleStruct getHeapUsed() {
        return memorySampleBranch.getHeapUsed();
    }

    public ProfilerSampleStruct getHeapCommitted() {
        return memorySampleBranch.getHeapCommitted();
    }

    public ProfilerSampleStruct getCollections() {
        return memorySampleBranch.getCollections();
    }

    public ProfilerSampleStruct getCollectionMillis() {
        return memorySampleBranch.getCollectionMillis();
    }
}
