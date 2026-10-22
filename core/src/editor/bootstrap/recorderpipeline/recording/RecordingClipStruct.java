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
    }

    // Management \\

    // A copy of the clip placed elsewhere, playing the same span the same way
    public RecordingClipStruct copyTo(int lane, float start) {

        RecordingClipStruct copy = new RecordingClipStruct(take, inPoint, outPoint, lane, start, channelMask);
        copy.setMuted(muted);
        copy.setTimeRampSeconds(timeRampSeconds);

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
}
