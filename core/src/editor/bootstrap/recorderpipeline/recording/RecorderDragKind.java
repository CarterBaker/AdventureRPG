package editor.bootstrap.recorderpipeline.recording;

public enum RecorderDragKind {

    /*
     * What the Recorder tab is carrying under the cursor: nothing, a take
     * picked up from the bin to lay on the timeline, or a clip already on it
     * being moved along its lanes.
     */

    NONE,
    TAKE,
    CLIP;
}
