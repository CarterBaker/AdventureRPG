package editor.bootstrap.recorderpipeline.recording;

import editor.bootstrap.transitionpipeline.transition.TransitionHandle;
import engine.root.StructPackage;

public class RecordingTransitionStruct extends StructPackage {

    /*
     * The transition the timeline's picture is passing through at the
     * playhead, read again each frame: the clip whose edge holds it, which
     * edge, the clip beside it on its lane carried on past its own span to
     * meet it, the transition itself, how far the picture has gone over —
     * eased, zero showing the side it comes from and one the side it goes to
     * — and whether the timeline shows any picture at all with the clip set
     * aside; the side played this frame is set by whoever renders the two.
     * Into a clip, the timeline without it gives way to it; out of a clip,
     * it gives way to the timeline without it.
     */

    // Transition
    private boolean active;
    private RecordingClipStruct clip;
    private RecorderClipEdge edge;
    private RecordingClipStruct carried;
    private TransitionHandle transitionHandle;
    private float mix;
    private boolean withoutShown;

    // Side
    private boolean withoutPlayed;

    // Management \\

    public void set(
            RecordingClipStruct clip,
            RecorderClipEdge edge,
            RecordingClipStruct carried,
            TransitionHandle transitionHandle,
            float mix,
            boolean withoutShown) {

        // Transition
        this.active = true;
        this.clip = clip;
        this.edge = edge;
        this.carried = carried;
        this.transitionHandle = transitionHandle;
        this.mix = mix;
        this.withoutShown = withoutShown;
    }

    public void clear() {
        this.active = false;
        this.clip = null;
        this.edge = null;
        this.carried = null;
        this.transitionHandle = null;
        this.mix = 0f;
        this.withoutShown = false;
    }

    // Accessible \\

    public boolean isActive() {
        return active;
    }

    public RecordingClipStruct getClip() {
        return clip;
    }

    public RecorderClipEdge getEdge() {
        return edge;
    }

    public RecordingClipStruct getCarried() {
        return carried;
    }

    public TransitionHandle getTransitionHandle() {
        return transitionHandle;
    }

    public float getMix() {
        return mix;
    }

    // True when the timeline shows a picture with the clip set aside, so both sides must be rendered
    public boolean isWithoutShown() {
        return withoutShown;
    }

    // True when the side the picture comes from is the timeline without the clip
    public boolean isFromWithout() {
        return edge == RecorderClipEdge.START;
    }

    // True when this frame plays the timeline without the clip; the side asked for is kept from frame to frame,
    // and only ever played while a transition shows a picture there
    public boolean isWithoutPlayed() {
        return active && withoutShown && withoutPlayed;
    }

    public void setWithoutPlayed(boolean withoutPlayed) {
        this.withoutPlayed = withoutPlayed;
    }
}
