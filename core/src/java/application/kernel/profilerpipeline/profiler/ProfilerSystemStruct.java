package application.kernel.profilerpipeline.profiler;

import engine.root.StructPackage;

public class ProfilerSystemStruct extends StructPackage {

    /*
     * The CPU record of one system instance: its own time across every
     * lifecycle phase, the share each phase took, and the group it rolls up
     * into, the nearest pipeline or context above it. Times are self time, a
     * manager's children being recorded against their own entries.
     */

    // Identity
    private final String name;
    private final String groupName;

    // Samples
    private final ProfilerSampleStruct total;
    private final ProfilerSampleStruct[] phases;

    // Constructor \\

    public ProfilerSystemStruct(String name, String groupName, int historyFrames) {

        // Identity
        this.name = name;
        this.groupName = groupName;

        // Samples
        this.total = new ProfilerSampleStruct(name, historyFrames);
        this.phases = new ProfilerSampleStruct[ProfilerPhase.LENGTH];

        for (int i = 0; i < ProfilerPhase.LENGTH; i++)
            this.phases[i] = new ProfilerSampleStruct(ProfilerPhase.VALUES[i].displayName, historyFrames);
    }

    // Frame \\

    public void accumulate(ProfilerPhase phase, float millis) {
        total.accumulate(millis);
        phases[phase.ordinal()].accumulate(millis);
    }

    public void commit(long frameIndex, boolean capturing) {

        total.commit(frameIndex, capturing);

        for (int i = 0; i < phases.length; i++)
            phases[i].commit(frameIndex, capturing);
    }

    public void resetCapture() {

        total.resetCapture();

        for (int i = 0; i < phases.length; i++)
            phases[i].resetCapture();
    }

    // Accessible \\

    public String getName() {
        return name;
    }

    public String getGroupName() {
        return groupName;
    }

    public ProfilerSampleStruct getTotal() {
        return total;
    }

    public ProfilerSampleStruct getPhase(ProfilerPhase phase) {
        return phases[phase.ordinal()];
    }
}
