package editor.bootstrap.recorderpipeline.recording;

public enum RecorderDragKind {

    /*
     * What the Recorder tab is carrying under the cursor: nothing, a take
     * picked up from the bin to lay on the timeline, a sound picked up from
     * the sound library to lay as a take of its own, a clip already on it
     * being moved along its lanes, the start or the end of a clip being
     * dragged to trim it, a cut being placed through a clip, a node on a clip
     * being retimed or moved, the empty lanes being dragged to scroll the view
     * along, a key being retimed on its row of the Keys tab or moved in time
     * and value on its graph, one of a key's handles being bent, a press on
     * nothing on the Keys pane's rows or graph, which scrolls the view along
     * as it travels — on the graph its values too — or else moves the
     * playhead, or a box drawn with Shift or Ctrl across a curve's row or its
     * graph to select the keys within it, or take them out.
     */

    NONE,
    TAKE,
    SOUND,
    CLIP,
    TRIM_START,
    TRIM_END,
    CUT,
    NODE,
    PAN,
    KEY,
    GRAPH_KEY,
    HANDLE_IN,
    HANDLE_OUT,
    KEYS_PRESS,
    GRAPH_PRESS,
    KEYS_BOX,
    GRAPH_BOX;

    // Accessible \\

    // True for what carries a whole span to lay on the lanes
    public boolean isSpan() {
        return this == TAKE || this == SOUND || this == CLIP;
    }

    // True for what is let go at a single moment of the timeline
    public boolean isMoment() {
        return this == TRIM_START || this == TRIM_END || this == CUT || this == NODE;
    }

    // True for a key or a handle edited live on the Keys tab while it is dragged
    public boolean isKey() {
        return this == KEY || this == GRAPH_KEY || this == HANDLE_IN || this == HANDLE_OUT;
    }

    // True for a press on nothing on the Keys pane, which scrolls the view as it travels
    public boolean isPress() {
        return this == KEYS_PRESS || this == GRAPH_PRESS;
    }

    // True for a box drawn on the Keys pane to select keys by
    public boolean isBox() {
        return this == KEYS_BOX || this == GRAPH_BOX;
    }
}
