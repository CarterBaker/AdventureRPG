package application.kernel.profilerpipeline.profiler;

public enum ProfilerPhase {

    /*
     * The stretches of one engine frame the profiler times. UPDATE through
     * RENDER are the system lifecycle phases, timed per system as well; DRAW
     * covers GPU submission and the buffer swap; PACING is the time the frame
     * rate cap sleeps off before the next frame begins.
     */

    UPDATE("Update"),
    FIXED_UPDATE("Fixed Update"),
    LATE_UPDATE("Late Update"),
    RENDER("Render"),
    DRAW("Draw"),
    PACING("Frame Pacing");

    public final String displayName;

    public static final ProfilerPhase[] VALUES = values();
    public static final int LENGTH = VALUES.length;

    ProfilerPhase(String displayName) {
        this.displayName = displayName;
    }
}
