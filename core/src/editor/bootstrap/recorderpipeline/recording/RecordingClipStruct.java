package editor.bootstrap.recorderpipeline.recording;

import application.bootstrap.musicpipeline.song.SongClipStruct;
import application.bootstrap.musicpipeline.song.SongData;
import engine.root.StructPackage;

public class RecordingClipStruct extends StructPackage {

    /*
     * One node on a recording's timeline: a span of its source, from its in
     * point to its out point in the source's own seconds, placed on a lane
     * from a moment of the timeline. Its source is a take of the bin, another
     * recording laid inside this one by name, a song by name, or a notes clip
     * of its own, timed by the recording's tempo map where it lies; a
     * recording or a song is bound to what it names once it is resolved, and
     * a notes clip to the song its notes play as. What a source holds is read
     * through one method each — the channels it can play, how long it runs
     * and the take, nested recording or song it is looked up in — and nothing
     * else asks which kind it is. The span played is kept as asked and held
     * within the source's current length only when it is read, so a nested
     * recording that grows or shrinks is played as it now stands. It plays
     * whichever of its source's channels it is given, so a take can be laid
     * down for its camera alone, or for everything but its weather. For each
     * channel the clip on the highest lane covering a moment plays it, unless
     * it is muted. Its time ramp is how long each jump of a take's clock is
     * swept through, a cut at zero. Each edge may hold a transition, named
     * with how long it runs inside the clip from that edge; the two never run
     * past each other, so a clip shortened under them plays them shorter.
     */

    // Source
    private final RecordingSourceKind sourceKind;
    private final RecordingTakeStruct take;
    private final String sourceName;
    private final SongClipStruct notes;
    private RecordingInstance child;
    private SongData song;

    // Span
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

    private RecordingClipStruct(
            RecordingSourceKind sourceKind,
            RecordingTakeStruct take,
            String sourceName,
            SongClipStruct notes,
            float inPoint,
            float outPoint,
            int lane,
            float start,
            int channelMask) {

        // Source
        this.sourceKind = sourceKind;
        this.take = take;
        this.sourceName = sourceName;
        this.notes = notes;

        // Span
        setSpan(inPoint, outPoint);

        // Placement
        this.lane = lane;
        this.start = start;

        // Playback
        this.channelMask = channelMask;

        // Transitions
        this.transitionNames = new String[RecorderClipEdge.VALUES.length];
        this.transitionSeconds = new float[RecorderClipEdge.VALUES.length];
    }

    // A span of a take of the recording's own bin
    public static RecordingClipStruct ofTake(
            RecordingTakeStruct take,
            float inPoint,
            float outPoint,
            int lane,
            float start,
            int channelMask) {
        return new RecordingClipStruct(
                RecordingSourceKind.TAKE, take, take.getName(), null, inPoint, outPoint, lane, start, channelMask);
    }

    // A span of another recording, named, laid inside this one
    public static RecordingClipStruct ofRecording(
            String recordingName,
            float inPoint,
            float outPoint,
            int lane,
            float start,
            int channelMask) {
        return new RecordingClipStruct(
                RecordingSourceKind.RECORDING, null, recordingName, null, inPoint, outPoint, lane, start, channelMask);
    }

    // A span of a project song, named, heard through the sampler
    public static RecordingClipStruct ofSong(String songName, float inPoint, float outPoint, int lane, float start) {
        return new RecordingClipStruct(RecordingSourceKind.SONG, null, songName, null, inPoint, outPoint, lane,
                start, RecorderChannel.SOUND.getBit());
    }

    // A span of the clip's own notes on an instrument, timed by the recording's tempo map
    public static RecordingClipStruct ofNotes(
            SongClipStruct notes,
            float inPoint,
            float outPoint,
            int lane,
            float start) {
        return new RecordingClipStruct(RecordingSourceKind.NOTES, null, notes.getInstrumentName(), notes, inPoint,
                outPoint, lane, start, RecorderChannel.SOUND.getBit());
    }

    // Management \\

    // A copy of the clip placed elsewhere, playing the same span of the same source the same way
    public RecordingClipStruct copyTo(int lane, float start) {

        RecordingClipStruct copy = new RecordingClipStruct(
                sourceKind, take, sourceName, notes, inPoint, outPoint, lane, start, channelMask);

        copy.bindChild(child);
        copy.bindSong(song);
        copy.setMuted(muted);
        copy.setTimeRampSeconds(timeRampSeconds);

        for (int i = 0; i < RecorderClipEdge.VALUES.length; i++)
            if (transitionNames[i] != null)
                copy.setTransition(RecorderClipEdge.VALUES[i], transitionNames[i], transitionSeconds[i]);

        return copy;
    }

    // True when the clip speaks for the channel at the time
    public boolean covers(RecorderChannel channel, float time) {
        return !muted && holds(channel) && time >= start && time <= getEnd();
    }

    public boolean overlaps(float spanStart, float spanEnd) {
        return start < spanEnd && getEnd() > spanStart;
    }

    public boolean holds(RecorderChannel channel) {
        return channel.isIn(getChannelMask());
    }

    // True when the clip plays anything that is seen, rather than only heard
    public boolean showsPicture() {
        return !muted && (getChannelMask() & RecorderChannel.PICTURE) != 0;
    }

    // True when the clip shows a picture at the time
    public boolean showsPictureAt(float time) {
        return showsPicture() && time >= start && time <= getEnd();
    }

    // The source's own time at a moment of the timeline: a take's, a nested recording's or a song's
    public float toChildTime(float time) {
        return inPoint + (time - start);
    }

    // The moment of the timeline a time of the source falls on
    public float toTimelineTime(float childTime) {
        return start + (childTime - inPoint);
    }

    // The seconds of the tempo map the song the clip sounds is timed by that a time of its source falls on: a song's
    // own map from its start, or for a notes clip the recording's from the timeline's start
    public double toSongSeconds(float childTime) {
        return resolveSongOrigin() + childTime;
    }

    // The tick of that tempo map the song's own first tick falls on: none for a song, where a notes clip's notes
    // begin on the timeline for a notes clip
    public double resolveSongTickOffset() {
        return song.getTempo().toTicks(resolveSongOrigin());
    }

    // The tick of the song the clip sounds that a time of its source falls on
    public double toSongTick(float childTime) {
        return song.getTempo().toTicks(toSongSeconds(childTime)) - resolveSongTickOffset();
    }

    // The seconds of the tempo map the source's own start falls on: a notes clip's notes are timed by the recording's
    // tempo where they lie, every other source from its own start
    private float resolveSongOrigin() {
        return sourceKind == RecordingSourceKind.NOTES ? start - inPoint : 0f;
    }

    // Source \\

    // The channels the source can play: what a take recorded, what a nested recording's clips play, or sound alone
    // for a song or notes; nothing while a nested recording or a song is not yet bound
    public int resolveSourceChannelMask() {
        return switch (sourceKind) {
            case TAKE -> take.getChannelMask();
            case RECORDING -> child != null ? child.resolveChannelMask() : RecorderChannel.NONE;
            case SONG, NOTES -> song != null ? RecorderChannel.SOUND.getBit() : RecorderChannel.NONE;
        };
    }

    // How long the source runs in its own seconds, as it stands now: a take's length, a nested recording's, a
    // song's at its own tempo, or a notes clip's at the recording's tempo where it lies; while a source is unbound
    // the span stands as asked, so a clip whose source is missing is still drawn where it lies
    public float resolveSourceDuration() {
        return switch (sourceKind) {
            case TAKE -> take.getDuration();
            case RECORDING -> child != null ? child.getDuration() : outPoint;
            case SONG, NOTES -> song != null ? resolveSongDuration() : outPoint;
        };
    }

    private float resolveSongDuration() {
        return (float) (song.getTempo().toSeconds(resolveSongTickOffset() + song.getLength()) - resolveSongOrigin());
    }

    // The take the clip is looked up in, null for any other source
    public RecordingTakeStruct getTake() {
        return take;
    }

    // The nested recording the clip is looked up in, null for any other source or while unbound
    public RecordingInstance getChild() {
        return child;
    }

    // The song the clip sounds — a project song, or a notes clip's notes as one — null for any other source or
    // while unbound
    public SongData getSong() {
        return song;
    }

    public void bindChild(RecordingInstance child) {
        this.child = child;
    }

    public void bindSong(SongData song) {
        this.song = song;
    }

    public RecordingSourceKind getSourceKind() {
        return sourceKind;
    }

    // What the source is named: a take's name, a nested recording's or song's, or a notes clip's instrument
    public String getSourceName() {
        return take != null ? take.getName() : sourceName;
    }

    // The name of the recording the clip nests, null for any other source
    public String getNestedName() {
        return sourceKind == RecordingSourceKind.RECORDING ? sourceName : null;
    }

    // The notes of a notes clip, as the codec writes them; null for any other source
    public SongClipStruct getNotes() {
        return notes;
    }

    // Span \\

    // The span played, kept as asked: no earlier than the source's start and never shorter than nothing
    public void setSpan(float inPoint, float outPoint) {
        this.inPoint = Math.max(0f, inPoint);
        this.outPoint = Math.max(this.inPoint, outPoint);
    }

    public void setInPoint(float inPoint) {
        setSpan(inPoint, outPoint);
    }

    public void setOutPoint(float outPoint) {
        setSpan(inPoint, outPoint);
    }

    // Where the span starts, held within the source as it stands now
    public float getInPoint() {
        return Math.min(inPoint, resolveSourceDuration());
    }

    // Where the span ends, held within the source as it stands now
    public float getOutPoint() {
        return Math.clamp(outPoint, getInPoint(), Math.max(getInPoint(), resolveSourceDuration()));
    }

    // The span's start as it was asked for, whatever the source now runs
    public float getStoredInPoint() {
        return inPoint;
    }

    // The span's end as it was asked for, whatever the source now runs
    public float getStoredOutPoint() {
        return outPoint;
    }

    // Accessible \\

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
        return getOutPoint() - getInPoint();
    }

    // The channels the clip plays: those it is given that its source can play
    public int getChannelMask() {
        return channelMask & resolveSourceChannelMask();
    }

    // The channels the clip was given, whatever its source can play now
    public int getStoredChannelMask() {
        return channelMask;
    }

    public void setChannelMask(int channelMask) {
        this.channelMask = channelMask;
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
