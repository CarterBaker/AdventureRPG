package editor.bootstrap.recorderpipeline.recording;

import engine.root.StructPackage;

public class RecordingNodeStruct extends StructPackage {

    /*
     * One node on a clip: a run of one character's samples in the clip's take
     * where it moves one way, or one action of its hands — the player's on
     * the timeline's lanes, and every character's in the Keys tab. It knows
     * the clip it lies on, whose it is, its kind, what it does and the words
     * for it, the samples of its run and the take's seconds they cover, the
     * moments of the timeline it is drawn across within the clip, and the
     * take's seconds its start may be dragged between. A movement node also
     * knows what the character did before it, which its run becomes when it
     * is removed. Nodes are refilled in place every time they are rebuilt.
     */

    // Node
    private int clipIndex;
    private RecordingBodyTrackStruct track;
    private RecorderNodeKind kind;
    private int value;
    private String label;

    // Run
    private int firstSample;
    private int endSample;
    private float takeStart;
    private float takeEnd;

    // Drawn
    private float start;
    private float end;

    // Editing
    private int previousValue;
    private float earliestStart;
    private float latestStart;
    private boolean movable;

    // Management \\

    public void set(
            int clipIndex,
            RecordingBodyTrackStruct track,
            RecorderNodeKind kind,
            int value,
            String label,
            int firstSample,
            int endSample,
            float takeStart,
            float takeEnd,
            RecordingClipStruct clip) {

        // Node
        this.clipIndex = clipIndex;
        this.track = track;
        this.kind = kind;
        this.value = value;
        this.label = label;

        // Run
        this.firstSample = firstSample;
        this.endSample = endSample;
        this.takeStart = takeStart;
        this.takeEnd = takeEnd;

        // Drawn
        this.start = Math.clamp(clip.toTimelineTime(takeStart), clip.getStart(), clip.getEnd());
        this.end = Math.clamp(clip.toTimelineTime(takeEnd), clip.getStart(), clip.getEnd());
    }

    public void setEditing(int previousValue, float earliestStart, float latestStart, boolean movable) {
        this.previousValue = previousValue;
        this.earliestStart = earliestStart;
        this.latestStart = latestStart;
        this.movable = movable;
    }

    public void setLatestStart(float latestStart) {
        this.latestStart = latestStart;
    }

    public boolean contains(float time) {
        return time >= start && time < end;
    }

    // Accessible \\

    public int getClipIndex() {
        return clipIndex;
    }

    public RecordingBodyTrackStruct getTrack() {
        return track;
    }

    public RecorderNodeKind getKind() {
        return kind;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public int getFirstSample() {
        return firstSample;
    }

    public int getEndSample() {
        return endSample;
    }

    public float getTakeStart() {
        return takeStart;
    }

    public float getTakeEnd() {
        return takeEnd;
    }

    public float getTakeLength() {
        return takeEnd - takeStart;
    }

    public float getStart() {
        return start;
    }

    public float getEnd() {
        return end;
    }

    public int getPreviousValue() {
        return previousValue;
    }

    public float getEarliestStart() {
        return earliestStart;
    }

    public float getLatestStart() {
        return latestStart;
    }

    // Only a node whose start lies inside its clip, with room to move, can be dragged
    public boolean isMovable() {
        return movable && earliestStart <= latestStart;
    }
}
