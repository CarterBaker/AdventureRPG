package application.bootstrap.musicpipeline.noteset;

public enum NoteLoopMode {

    /*
     * How a region's sample plays against the note that sounds it, as SFZ
     * names the modes. NONE plays the sample through and fades on release;
     * ONE_SHOT plays it through whatever the note does, as a drum is struck;
     * SUSTAIN repeats the sample's loop while the note is held and, let go,
     * plays on through its recorded tail as it fades; CONTINUOUS repeats the
     * loop until the release has faded it out. The two looping modes need a
     * sample with loop points. A note set names each by its lower-case name.
     */

    NONE(false, false, true),
    ONE_SHOT(false, false, false),
    SUSTAIN(true, false, true),
    CONTINUOUS(true, true, true);

    // Values
    public static final NoteLoopMode[] VALUES = values();

    // Internal
    private final boolean loopsHeld;
    private final boolean loopsReleased;
    private final boolean releases;

    // Constructor \\

    NoteLoopMode(boolean loopsHeld, boolean loopsReleased, boolean releases) {
        this.loopsHeld = loopsHeld;
        this.loopsReleased = loopsReleased;
        this.releases = releases;
    }

    // Accessible \\

    // Whether the sample repeats its loop while the note is held
    public boolean loopsHeld() {
        return loopsHeld;
    }

    // Whether the sample keeps repeating its loop once the note is let go
    public boolean loopsReleased() {
        return loopsReleased;
    }

    // Whether letting the note go fades it; a one-shot plays through
    public boolean releases() {
        return releases;
    }
}
