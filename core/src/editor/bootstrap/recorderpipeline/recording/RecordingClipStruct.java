package editor.bootstrap.recorderpipeline.recording;

import engine.root.StructPackage;

public class RecordingClipStruct extends StructPackage {

    /*
     * One node on a recording's timeline: a span of a take, from its in point
     * to its out point in the take's own seconds, placed on a lane from a
     * moment of the timeline. It plays whichever of the take's channels it is
     * given, so a take can be laid down for its camera alone, or for
     * everything but its weather. For each channel the clip on the highest
     * lane covering a moment plays it, unless it is muted. Its time ramp is
     * how long each jump of the take's clock is swept through, a cut at zero.
     * Each edge may hold a transition, named with how long it runs inside the
     * clip from that edge; the two never run past each other, so a clip
     * shortened under them plays them shorter.
     */

    // Source
    private final RecordingTakeStruct take;
    private float inPoint;
    private float outPoint;

    // Placement
    private int lane;
    private float start;

    // Playback
    private int channelMask;
    private boolean muted;
    private float timeRampSeconds;

    // Transitions — indexed by edge, a null name for none
    private final String[] transitionNames;
    private final float[] transitionSeconds;

    // Constructor \\

    public RecordingClipStruct(
            RecordingTakeStruct take,
            float inPoint,
            float outPoint,
            int lane,
            float start,
            int channelMask) {

        // Source
        this.take = take;
        this.inPoint = inPoint;
        this.outPoint = outPoint;

        // Placement
        this.lane = lane;
        this.start = start;

        // Playback
        this.channelMask = channelMask & take.getChannelMask();

        // Transitions
        this.transitionNames = new String[RecorderClipEdge.VALUES.length];
        this.transitionSeconds = new float[RecorderClipEdge.VALUES.length];
    }

    // Management \\

    // A copy of the clip placed elsewhere, playing the same span the same way
    public RecordingClipStruct copyTo(int lane, float start) {

        RecordingClipStruct copy = new RecordingClipStruct(take, inPoint, outPoint, lane, start, channelMask);
        copy.setMuted(muted);
        copy.setTimeRampSeconds(timeRampSeconds);

        for (int i = 0; i < RecorderClipEdge.VALUES.length; i++)
            if (transitionNames[i] != null)
                copy.setTransition(RecorderClipEdge.VALUES[i], transitionNames[i], transitionSeconds[i]);

        return copy;
    }

    // True when the clip speaks for the channel at the time
    public boolean covers(RecorderChannel channel, float time) {
        return !muted && channel.isIn(channelMask) && time >= start && time <= getEnd();
    }

    public boolean overlaps(float spanStart, float spanEnd) {
        return start < spanEnd && getEnd() > spanStart;
    }

    public boolean holds(RecorderChannel channel) {
        return channel.isIn(channelMask);
    }

    // True when the clip plays anything that is seen, rather than only heard
    public boolean showsPicture() {
        return !muted && (channelMask & RecorderChannel.PICTURE) != 0;
    }

    // True when the clip shows a picture at the time
    public boolean showsPictureAt(float time) {
        return showsPicture() && time >= start && time <= getEnd();
    }

    // The take's own time at a moment of the timeline
    public float toTakeTime(float time) {
        return inPoint + (time - start);
    }

    // The moment of the timeline a time of the take falls on
    public float toTimelineTime(float takeTime) {
        return start + (takeTime - inPoint);
    }

    // Accessible \\

    public RecordingTakeStruct getTake() {
        return take;
    }

    public float getInPoint() {
        return inPoint;
    }

    public float getOutPoint() {
        return outPoint;
    }

    // The span played, kept within the take and never shorter than nothing
    public void setSpan(float inPoint, float outPoint) {
        this.inPoint = Math.clamp(inPoint, 0f, take.getDuration());
        this.outPoint = Math.clamp(outPoint, this.inPoint, take.getDuration());
    }

    public int getLane() {
        return lane;
    }

    public void setLane(int lane) {
        this.lane = lane;
    }

    public float getStart() {
        return start;
    }

    public void setStart(float start) {
        this.start = start;
    }

    public float getEnd() {
        return start + getLength();
    }

    public float getLength() {
        return outPoint - inPoint;
    }

    public int getChannelMask() {
        return channelMask;
    }

    // Only channels the take recorded can be played
    public void setChannelMask(int channelMask) {
        this.channelMask = channelMask & take.getChannelMask();
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public float getTimeRampSeconds() {
        return timeRampSeconds;
    }

    public void setTimeRampSeconds(float timeRampSeconds) {
        this.timeRampSeconds = timeRampSeconds;
    }

    // Transitions \\

    public void setTransition(RecorderClipEdge edge, String transitionName, float seconds) {
        transitionNames[edge.ordinal()] = transitionName;
        transitionSeconds[edge.ordinal()] = seconds;
    }

    public void setTransitionSeconds(RecorderClipEdge edge, float seconds) {
        transitionSeconds[edge.ordinal()] = seconds;
    }

    public void clearTransition(RecorderClipEdge edge) {
        transitionNames[edge.ordinal()] = null;
        transitionSeconds[edge.ordinal()] = 0f;
    }

    public boolean hasTransition(RecorderClipEdge edge) {
        return transitionNames[edge.ordinal()] != null;
    }

    public boolean hasTransitions() {
        return hasTransition(RecorderClipEdge.START) || hasTransition(RecorderClipEdge.END);
    }

    public String getTransitionName(RecorderClipEdge edge) {
        return transitionNames[edge.ordinal()];
    }

    // How long the edge's transition was asked to run, however long the clip now is
    public float getTransitionSeconds(RecorderClipEdge edge) {
        return transitionSeconds[edge.ordinal()];
    }

    // How long the edge's transition runs: as asked, kept within the clip, the start's first and the end's in what
    // the start leaves
    public float resolveTransitionSeconds(RecorderClipEdge edge) {

        float opening = hasTransition(RecorderClipEdge.START)
                ? Math.min(getTransitionSeconds(RecorderClipEdge.START), getLength())
                : 0f;

        if (edge == RecorderClipEdge.START)
            return opening;

        return hasTransition(RecorderClipEdge.END)
                ? Math.min(getTransitionSeconds(RecorderClipEdge.END), getLength() - opening)
                : 0f;
    }

    // The moment of the timeline the edge's transition begins: the clip's start, or as long before its end as the
    // transition runs
    public float resolveTransitionStart(RecorderClipEdge edge) {
        return edge == RecorderClipEdge.START ? start : getEnd() - resolveTransitionSeconds(edge);
    }

    // True when the edge's transition runs across the moment
    public boolean isInTransition(RecorderClipEdge edge, float time) {

        float seconds = resolveTransitionSeconds(edge);
        float from = resolveTransitionStart(edge);

        return seconds > 0f && time >= from && time <= from + seconds;
    }
}
