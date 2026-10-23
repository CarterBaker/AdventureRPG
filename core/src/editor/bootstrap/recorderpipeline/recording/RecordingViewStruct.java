package editor.bootstrap.recorderpipeline.recording;

import editor.runtime.EditorSetting;
import engine.root.StructPackage;

public class RecordingViewStruct extends StructPackage {

    /*
     * The stretch of a recording's timeline the Recorder tab shows: from a
     * moment, for a number of seconds. A fitted view shows the whole timeline
     * and follows it as it grows; zooming in frees it, and zooming back out
     * to the whole timeline fits it again. Every change is kept within the
     * timeline and reports whether it moved the view, so the tab redraws only
     * when it did.
     */

    // View
    private float start;
    private float span;
    private boolean fitted;

    // Constructor \\

    public RecordingViewStruct() {
        this.span = EditorSetting.RECORDER_MIN_TIMELINE_SECONDS;
        this.fitted = true;
    }

    // Management \\

    public void fit() {
        this.fitted = true;
    }

    // Kept within the timeline, and fitted again once it shows all of it
    public void resolve(float timelineSpan) {

        if (fitted || span >= timelineSpan) {
            this.fitted = true;
            this.start = 0f;
            this.span = timelineSpan;
            return;
        }

        this.start = Math.clamp(start, 0f, timelineSpan - span);
    }

    // Zoomed by the factor about the moment, which stays where it is drawn
    public boolean zoom(float time, float factor, float timelineSpan) {

        float previousStart = start;
        float previousSpan = span;
        float zoomed = Math.clamp(
                span * factor,
                Math.min(EditorSetting.RECORDER_MIN_VIEW_SECONDS, timelineSpan),
                timelineSpan);

        this.start = time - (time - start) * zoomed / span;
        this.span = zoomed;
        this.fitted = false;
        resolve(timelineSpan);

        return start != previousStart || span != previousSpan;
    }

    // A fitted view already shows everything, so only a zoomed view moves along
    public boolean scrollTo(float start, float timelineSpan) {

        if (fitted)
            return false;

        float previousStart = this.start;

        this.start = start;
        resolve(timelineSpan);

        return this.start != previousStart;
    }

    // Brings a moment outside the view in, a little way from the left edge
    public boolean reveal(float time, float timelineSpan) {

        if (fitted || contains(time))
            return false;

        return scrollTo(time - span * EditorSetting.RECORDER_REVEAL_SHARE, timelineSpan);
    }

    public boolean contains(float time) {
        return time >= start && time <= getEnd();
    }

    // The moment at a share of the way across the view
    public float toTime(float fraction) {
        return start + fraction * span;
    }

    // How far across the view a moment lies, below 0 before it and above 1 after it
    public float toFraction(float time) {
        return (time - start) / span;
    }

    // Accessible \\

    public float getStart() {
        return start;
    }

    public float getSpan() {
        return span;
    }

    public float getEnd() {
        return start + span;
    }

    public float getMiddle() {
        return start + span / 2f;
    }

    public boolean isFitted() {
        return fitted;
    }
}
