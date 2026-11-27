package editor.bootstrap.recorderpipeline.recording;

import engine.root.StructPackage;

public class RecordingPlayingStruct extends StructPackage {

    /*
     * What plays a channel at a moment, found by walking down through every
     * nested recording on the way: the clip of a take that speaks for it at
     * whatever depth it lies, and the moment of that take it plays, the
     * moment mapped into each nested recording in turn, and the outermost
     * clip on the way down holding a photo filter with the moment of its own
     * timeline, whose filter is laid over what plays. Each system that asks
     * keeps one and has it filled again each time, so finding what plays
     * allocates nothing.
     */

    // Playing
    private RecordingClipStruct clip;
    private float takeTime;

    // Filter
    private RecordingClipStruct filterClip;
    private float filterTime;

    // Management \\

    public void set(RecordingClipStruct clip, float takeTime) {
        this.clip = clip;
        this.takeTime = takeTime;
        this.filterClip = null;
    }

    // A clip on the way down to what plays holds a filter; one further out takes the place of one within it
    public void filterBy(RecordingClipStruct filtered, float time) {
        this.filterClip = filtered;
        this.filterTime = time;
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

    // The outermost clip holding a filter on the way down, null when none does
    public RecordingClipStruct getFilterClip() {
        return filterClip;
    }

    // The moment of the timeline the filter's clip lies on
    public float getFilterTime() {
        return filterTime;
    }
}
