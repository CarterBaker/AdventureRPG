package application.kernel.profilerpipeline.profilermanager;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

import application.kernel.profilerpipeline.profiler.ProfilerGpuStruct;
import application.kernel.profilerpipeline.profiler.ProfilerPhase;
import application.kernel.profilerpipeline.profiler.ProfilerSampleStruct;
import application.kernel.profilerpipeline.profiler.ProfilerStatisticsUtility;
import application.kernel.profilerpipeline.profiler.ProfilerSystemStruct;
import application.kernel.profilerpipeline.profiler.ProfilerThreadStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectCollection;

class ProfilerReportBranch extends BranchPackage {

    /*
     * Writes a finished capture to the profiler directory as a sealed ARPG
     * report: frame time percentiles, the phase split, every group, the
     * heaviest systems with their phase breakdown, every GPU scope, every
     * thread pool, the counters and the heap. All figures are capture
     * averages, so a report reads the whole capture, not the last moment.
     * Decode a report with the ARPG tool to read it.
     */

    // Internal
    private ProfilerManager profilerManager;
    private DateTimeFormatter timestampFormatter;

    // Scratch
    private ObjectArrayList<ProfilerSystemStruct> systemScratch;
    private ObjectArrayList<ProfilerSampleStruct> sampleScratch;
    private ObjectArrayList<ProfilerGpuStruct> gpuScratch;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.timestampFormatter = DateTimeFormatter.ofPattern(EngineSetting.PROFILER_REPORT_TIMESTAMP_PATTERN);

        // Scratch
        this.systemScratch = new ObjectArrayList<>();
        this.sampleScratch = new ObjectArrayList<>();
        this.gpuScratch = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.profilerManager = get(ProfilerManager.class);
    }

    // Report \\

    File writeReport(FloatArrayList captureFrameMillis) {

        ArpgObjectStruct reportArpg = new ArpgObjectStruct();

        reportArpg.addProperty("version", EngineSetting.VERSION);
        reportArpg.add("frame", buildFrame(captureFrameMillis));
        reportArpg.add("phases", buildPhases());
        reportArpg.add("groups", buildSamples(profilerManager.getGroups()));
        reportArpg.add("systems", buildSystems());
        reportArpg.add("gpu", buildGpu());
        reportArpg.add("threads", buildThreads());
        reportArpg.add("counters", buildSamples(profilerManager.getCounters()));
        reportArpg.add("memory", buildMemory());

        File directory = new File(internal.path, EngineSetting.PROFILER_DIRECTORY);
        directory.mkdirs();

        File file = ArpgUtility.resolveFile(
                directory,
                EngineSetting.PROFILER_REPORT_PREFIX + LocalDateTime.now().format(timestampFormatter));

        return ArpgUtility.tryWriteObject(file, reportArpg) ? file : null;
    }

    // Sections \\

    private ArpgObjectStruct buildFrame(FloatArrayList captureFrameMillis) {

        ProfilerSampleStruct frame = profilerManager.getFrameSample();
        float[] frameMillis = captureFrameMillis.toFloatArray();
        ArpgObjectStruct frameArpg = new ArpgObjectStruct();

        frameArpg.addProperty("frames", frame.getCaptureFrames());
        frameArpg.addProperty("averageMs", frame.getCaptureAverage());
        frameArpg.addProperty("averageFps", frame.getCaptureAverage() > 0f
                ? EngineSetting.MILLIS_PER_SECOND_FLOAT / frame.getCaptureAverage()
                : 0f);
        frameArpg.addProperty("peakMs", frame.getCapturePeak());

        ArpgArrayStruct percentilesArpg = new ArpgArrayStruct();

        for (float fraction : EngineSetting.PROFILER_PERCENTILES) {
            ArpgObjectStruct percentileArpg = new ArpgObjectStruct();
            percentileArpg.addProperty("percentile", fraction);
            percentileArpg.addProperty("ms", ProfilerStatisticsUtility.percentile(
                    frameMillis, frameMillis.length, fraction));
            percentilesArpg.add(percentileArpg);
        }

        frameArpg.add("percentiles", percentilesArpg);

        return frameArpg;
    }

    private ArpgArrayStruct buildPhases() {

        ArpgArrayStruct phasesArpg = new ArpgArrayStruct();

        for (ProfilerPhase phase : ProfilerPhase.VALUES)
            phasesArpg.add(toSampleArpg(profilerManager.getPhaseSample(phase)));

        return phasesArpg;
    }

    private ArpgArrayStruct buildSystems() {

        systemScratch.clear();
        systemScratch.addAll(profilerManager.getSystems());
        systemScratch.sort(Comparator.comparingDouble(
                (ProfilerSystemStruct system) -> system.getTotal().getCaptureAverage()).reversed());

        ArpgArrayStruct systemsArpg = new ArpgArrayStruct();
        int count = Math.min(systemScratch.size(), EngineSetting.PROFILER_REPORT_TOP_ENTRIES);

        for (int i = 0; i < count; i++) {

            ProfilerSystemStruct system = systemScratch.get(i);

            if (system.getTotal().getCaptureAverage() <= 0f)
                break;

            ArpgObjectStruct systemArpg = toSampleArpg(system.getTotal());
            systemArpg.addProperty("group", system.getGroupName());

            ArpgObjectStruct phasesArpg = new ArpgObjectStruct();

            for (ProfilerPhase phase : ProfilerPhase.VALUES) {

                float average = system.getPhase(phase).getCaptureAverage();

                if (average > 0f)
                    phasesArpg.addProperty(phase.displayName, average);
            }

            systemArpg.add("phases", phasesArpg);
            systemsArpg.add(systemArpg);
        }

        return systemsArpg;
    }

    private ArpgArrayStruct buildGpu() {

        gpuScratch.clear();
        gpuScratch.addAll(profilerManager.getGpuRecords());
        gpuScratch.sort(Comparator.comparingDouble(
                (ProfilerGpuStruct record) -> record.getMillis().getCaptureAverage()).reversed());

        ArpgArrayStruct gpuArpg = new ArpgArrayStruct();

        for (int i = 0; i < gpuScratch.size(); i++) {

            ProfilerGpuStruct record = gpuScratch.get(i);
            ArpgObjectStruct recordArpg = toSampleArpg(record.getMillis());

            recordArpg.addProperty("drawCalls", record.getDrawCalls().getCaptureAverage());
            recordArpg.addProperty("triangles", record.getTriangles().getCaptureAverage());
            gpuArpg.add(recordArpg);
        }

        return gpuArpg;
    }

    private ArpgArrayStruct buildThreads() {

        ArpgArrayStruct threadsArpg = new ArpgArrayStruct();

        for (ProfilerThreadStruct thread : profilerManager.getThreads()) {

            ArpgObjectStruct threadArpg = new ArpgObjectStruct();

            threadArpg.addProperty("name", thread.getName());
            threadArpg.addProperty("threads", thread.getThreadSize());
            threadArpg.addProperty("busyMsPerFrame", thread.getBusyMillis().getCaptureAverage());
            threadArpg.addProperty("utilization", thread.getUtilization().getCaptureAverage());
            threadArpg.addProperty("tasksPerFrame", thread.getCompletedTasks().getCaptureAverage());
            threadArpg.addProperty("inFlight", thread.getInFlight().getCaptureAverage());
            threadsArpg.add(threadArpg);
        }

        return threadsArpg;
    }

    private ArpgArrayStruct buildMemory() {

        ArpgArrayStruct memoryArpg = new ArpgArrayStruct();

        memoryArpg.add(toSampleArpg(profilerManager.getHeapUsed()));
        memoryArpg.add(toSampleArpg(profilerManager.getHeapCommitted()));
        memoryArpg.add(toSampleArpg(profilerManager.getCollections()));
        memoryArpg.add(toSampleArpg(profilerManager.getCollectionMillis()));

        return memoryArpg;
    }

    private ArpgArrayStruct buildSamples(ObjectCollection<ProfilerSampleStruct> samples) {

        sampleScratch.clear();
        sampleScratch.addAll(samples);
        sampleScratch.sort(Comparator.comparingDouble(ProfilerSampleStruct::getCaptureAverage).reversed());

        ArpgArrayStruct samplesArpg = new ArpgArrayStruct();

        for (int i = 0; i < sampleScratch.size(); i++)
            samplesArpg.add(toSampleArpg(sampleScratch.get(i)));

        return samplesArpg;
    }

    // Utility \\

    private ArpgObjectStruct toSampleArpg(ProfilerSampleStruct sample) {

        ArpgObjectStruct sampleArpg = new ArpgObjectStruct();

        sampleArpg.addProperty("name", sample.getName());
        sampleArpg.addProperty("average", sample.getCaptureAverage());
        sampleArpg.addProperty("peak", sample.getCapturePeak());
        sampleArpg.addProperty("perFrameCalls", sample.getCaptureFrames() == 0L
                ? 0f
                : (float) sample.getCaptureCalls() / sample.getCaptureFrames());

        return sampleArpg;
    }
}
