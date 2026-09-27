package application.kernel.profilerpipeline.profilermanager;

import application.kernel.profilerpipeline.profiler.ProfilerPhase;
import application.kernel.profilerpipeline.profiler.ProfilerSampleStruct;
import application.kernel.profilerpipeline.profiler.ProfilerSystemStruct;
import engine.root.AssemblyPackage;
import engine.root.BranchPackage;
import engine.root.ContextPackage;
import engine.root.EngineSetting;
import engine.root.PipelinePackage;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;

class SystemSampleBranch extends BranchPackage {

    /*
     * Records each system's own lifecycle time, keyed by the system instance
     * so two contexts running the same class stay apart, and rolls it into the
     * group the system belongs to: its nearest pipeline or context, or failing
     * both the top-level manager it hangs from. A system that stops
     * reporting, its context closed, is pruned once it has been silent for a
     * whole history window.
     */

    // Palette
    private Reference2ObjectLinkedOpenHashMap<SystemPackage, ProfilerSystemStruct> system2ProfilerSystem;
    private Object2ObjectLinkedOpenHashMap<String, ProfilerSampleStruct> groupName2Sample;

    // Settings
    private int historyFrames;
    private float nanosPerMilli;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.system2ProfilerSystem = new Reference2ObjectLinkedOpenHashMap<>();
        this.groupName2Sample = new Object2ObjectLinkedOpenHashMap<>();

        // Settings
        this.historyFrames = EngineSetting.PROFILER_HISTORY_FRAMES;
        this.nanosPerMilli = EngineSetting.NANOS_PER_MILLI;
    }

    // Record \\

    void recordSystem(SystemPackage system, ProfilerPhase phase, long nanos) {

        ProfilerSystemStruct profilerSystem = system2ProfilerSystem.get(system);

        if (profilerSystem == null) {
            profilerSystem = new ProfilerSystemStruct(
                    system.getClass().getSimpleName(),
                    resolveGroupName(system),
                    historyFrames);
            system2ProfilerSystem.put(system, profilerSystem);
        }

        float millis = nanos / nanosPerMilli;

        profilerSystem.accumulate(phase, millis);
        resolveGroupSample(profilerSystem.getGroupName()).accumulate(millis);
    }

    private String resolveGroupName(SystemPackage system) {

        SystemPackage current = system;
        SystemPackage topmost = system;

        while (true) {

            if (current instanceof PipelinePackage || current instanceof ContextPackage)
                return current.getClass().getSimpleName();

            if (current instanceof AssemblyPackage || current.local == null || current.local == current)
                return topmost == current ? EngineSetting.PROFILER_GROUP_ENGINE : topmost.getClass().getSimpleName();

            topmost = current;
            current = current.local;
        }
    }

    private ProfilerSampleStruct resolveGroupSample(String groupName) {

        ProfilerSampleStruct sample = groupName2Sample.get(groupName);

        if (sample == null) {
            sample = new ProfilerSampleStruct(groupName, historyFrames);
            groupName2Sample.put(groupName, sample);
        }

        return sample;
    }

    // Frame \\

    void commit(long frameIndex, boolean capturing) {

        ObjectIterator<Reference2ObjectMap.Entry<SystemPackage, ProfilerSystemStruct>> iterator = system2ProfilerSystem
                .reference2ObjectEntrySet()
                .fastIterator();

        while (iterator.hasNext()) {

            ProfilerSystemStruct profilerSystem = iterator.next().getValue();
            profilerSystem.commit(frameIndex, capturing);

            if (frameIndex - profilerSystem.getTotal().getLastActiveFrame() > historyFrames)
                iterator.remove();
        }

        for (ProfilerSampleStruct sample : groupName2Sample.values())
            sample.commit(frameIndex, capturing);
    }

    void resetCapture() {

        for (ProfilerSystemStruct profilerSystem : system2ProfilerSystem.values())
            profilerSystem.resetCapture();

        for (ProfilerSampleStruct sample : groupName2Sample.values())
            sample.resetCapture();
    }

    // Accessible \\

    ObjectCollection<ProfilerSystemStruct> getSystems() {
        return system2ProfilerSystem.values();
    }

    ObjectCollection<ProfilerSampleStruct> getGroups() {
        return groupName2Sample.values();
    }
}
