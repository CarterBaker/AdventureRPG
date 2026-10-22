package editor.bootstrap.recorderpipeline.recording;

public enum RecorderState {

    /*
     * Where the recorder's transport stands. Idle, the Dev window plays as it
     * always does; recording, a new take records what is played live over
     * the clips the timeline plays; playing and paused, the timeline drives
     * the Dev window from its playhead; exporting, the timeline renders frame
     * by frame into a video.
     */

    IDLE,
    RECORDING,
    PLAYING,
    PAUSED,
    EXPORTING;

    // Values
    public static final RecorderState[] VALUES = values();

    // Accessible \\

    // True while the recording drives a Dev window rather than leaving it to play
    public boolean isSession() {
        return this != IDLE;
    }
}
