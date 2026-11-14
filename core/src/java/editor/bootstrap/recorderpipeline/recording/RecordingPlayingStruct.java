package editor.bootstrap.recorderpipeline.recording;

import engine.root.StructPackage;

public class RecordingPlayingStruct extends StructPackage {

    /*
     * What plays a channel at a moment, found by walking down through every
     * nested recording on the way: the clip of a take that speaks for it at
     * whatever depth it lies, and the moment of that take it plays, the
     * moment mapped into each nested recording in turn. Each system that asks
     * keeps one and has it filled again each time, so finding what plays
     * allocates nothing.
     */

    // Playing
    private RecordingClipStruct clip;
    private float takeTime;

    // Management \\

    public void set(RecordingClipStruct clip, float takeTime) {
        this.clip = clip;
        this.takeTime = takeTime;
    }

    // Accessible \\

    public RecordingClipStruct getClip() {
        return clip;
    }

    public RecordingTakeStruct getTake() {
        return clip.getTake();
    }

    // The moment of the take the clip plays
    public float getTakeTime() {
        return takeTime;
    }

    public float getTimeRampSeconds() {
        return clip.getTimeRampSeconds();
    }
}
