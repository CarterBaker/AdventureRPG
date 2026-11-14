package editor.bootstrap.recorderpipeline.recording;

import application.bootstrap.musicpipeline.song.SongTempoStruct;
import application.bootstrap.musicpipeline.util.NoteScale;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecordingInstance extends InstancePackage {

    /*
     * One recording open in the recorder, or laid inside another: its kind,
     * a Video or a Music file, its name, the world it was made in, its tempo
     * map and meter — which time its notes clips and the music grid of its
     * timeline — its key, which Scale Lock keeps notes on, its lanes as a
     * Music file mixes them, each heard at its volume, none while muted or
     * while another is soloed, its bin of takes in
     * the order they were recorded, the clips laid on its timeline, and how
     * it exports. It runs as long as its last clip reaches. For each channel
     * the clip on the highest lane covering a moment is the one that plays
     * it, the later laid of two on the same lane; a nested recording is
     * walked down into at the moment mapped into it, by the same rule at
     * every depth, and one that plays nothing there gives way to the clip
     * below it. A transition also asks what plays with one clip set aside and
     * its neighbour carried on past its own span, at whatever depth the two
     * lie.
     */

    // Identity
    private RecordingKind kind;
    private String recordingName;
    private String displayName;
    private String worldName;

    // Time
    private SongTempoStruct tempo;
    private int beatsPerBar;
    private int beatUnit;

    // Key
    private int keyRoot;
    private NoteScale keyScale;

    // Lanes
    private RecordingLaneStruct[] lanes;

    // Bin
    private ObjectArrayList<RecordingTakeStruct> takes;

    // Timeline
    private ObjectArrayList<RecordingClipStruct> clips;

    // Export
    private RecordingExportStruct exportStruct;

    // Internal \\

    @Override
    protected void create() {

        // Bin
        this.takes = new ObjectArrayList<>();

        // Timeline
        this.clips = new ObjectArrayList<>();
    }

    // Constructor \\

    public void constructor(
            RecordingKind kind,
            String recordingName,
            String worldName,
            SongTempoStruct tempo,
            int beatsPerBar,
            int beatUnit,
            int keyRoot,
            NoteScale keyScale,
            RecordingExportStruct exportStruct) {

        // Identity
        this.kind = kind;
        this.recordingName = recordingName;
        this.displayName = EngineSetting.SONG_DISPLAY_NAME_NONE;
        this.worldName = worldName;

        // Time
        this.tempo = tempo;
        this.beatsPerBar = beatsPerBar;
        this.beatUnit = beatUnit;

        // Key
        this.keyRoot = keyRoot;
        this.keyScale = keyScale;

        // Lanes
        this.lanes = new RecordingLaneStruct[kind.getLaneCount()];

        for (int i = 0; i < lanes.length; i++)
            lanes[i] = new RecordingLaneStruct();

        // Export
        this.exportStruct = exportStruct;
    }

    // Takes \\

    public void addTake(RecordingTakeStruct take) {
        takes.add(take);
    }

    // The take leaves the bin, and every clip of it the timeline
    public void removeTake(int index) {

        RecordingTakeStruct take = takes.remove(index);

        for (int i = clips.size() - 1; i >= 0; i--)
            if (clips.get(i).getTake() == take)
                clips.remove(i);
    }

    public boolean hasTakeName(String takeName) {

        for (int i = 0; i < takes.size(); i++)
            if (takes.get(i).getName().equals(takeName))
                return true;

        return false;
    }

    public int countClips(RecordingTakeStruct take) {

        int count = 0;

        for (int i = 0; i < clips.size(); i++)
            if (clips.get(i).getTake() == take)
                count++;

        return count;
    }

    // Clips \\

    public int addClip(RecordingClipStruct clip) {
        clips.add(clip);
        return clips.size() - 1;
    }

    public void removeClip(int index) {
        clips.remove(index);
    }

    // Every channel any clip plays, nested recordings counted as they play
    public int resolveChannelMask() {

        int channelMask = RecorderChannel.NONE;

        for (int i = 0; i < clips.size(); i++)
            if (!clips.get(i).isMuted())
                channelMask |= clips.get(i).getChannelMask();

        return channelMask;
    }

    // What plays the channel at the time, walked down through nested recordings, as the timeline plays without one
    // clip — none set aside when it is null — with another carried on past its own span as though it still covered
    // the time; the playing struct filled and handed back, or null when nothing does
    public RecordingPlayingStruct findPlaying(
            RecorderChannel channel,
            float time,
            RecordingClipStruct without,
            RecordingClipStruct carried,
            RecordingPlayingStruct playing) {

        int boundLane = Integer.MAX_VALUE;
        int boundIndex = Integer.MAX_VALUE;

        while (true) {

            int found = findCandidate(channel, time, without, carried, boundLane, boundIndex);

            if (found == EngineSetting.INDEX_NOT_FOUND)
                return null;

            RecordingClipStruct clip = clips.get(found);
            float childTime = clip.toChildTime(time);

            if (clip.getTake() != null) {
                playing.set(clip, childTime);
                return playing;
            }

            if (clip.getChild() != null
                    && clip.getChild().findPlaying(channel, childTime, without, carried, playing) != null)
                return playing;

            boundLane = clip.getLane();
            boundIndex = found;
        }
    }

    // The clip of the highest lane, then the later laid, that covers the channel at the time and ranks below the
    // bound given; -1 when none does
    private int findCandidate(
            RecorderChannel channel,
            float time,
            RecordingClipStruct without,
            RecordingClipStruct carried,
            int boundLane,
            int boundIndex) {

        int found = EngineSetting.INDEX_NOT_FOUND;
        int foundLane = Integer.MIN_VALUE;

        for (int i = 0; i < clips.size(); i++) {

            RecordingClipStruct clip = clips.get(i);
            int lane = clip.getLane();
            boolean covering = clip == carried
                    ? !clip.isMuted() && clip.holds(channel)
                    : clip.covers(channel, time);

            if (clip == without || !covering || lane > boundLane || lane == boundLane && i >= boundIndex)
                continue;

            if (lane >= foundLane) {
                found = i;
                foundLane = lane;
            }
        }

        return found;
    }

    // The clip on the same lane that ends just where the clip begins, or begins just where it ends, null for none
    public RecordingClipStruct findAdjoiningClip(RecordingClipStruct clip, boolean before) {

        float edge = before ? clip.getStart() : clip.getEnd();

        for (int i = 0; i < clips.size(); i++) {

            RecordingClipStruct other = clips.get(i);
            float otherEdge = before ? other.getEnd() : other.getStart();

            if (other != clip && other.getLane() == clip.getLane()
                    && Math.abs(otherEdge - edge) <= EditorSetting.RECORDER_ADJOIN_SECONDS)
                return other;
        }

        return null;
    }

    // True when any clip but the one shows a picture at the time, a nested recording only where it shows one itself
    public boolean showsPictureWithout(float time, RecordingClipStruct without) {

        for (int i = 0; i < clips.size(); i++) {

            RecordingClipStruct clip = clips.get(i);

            if (clip == without || !clip.showsPictureAt(time))
                continue;

            if (clip.getChild() == null || clip.getChild().showsPictureWithout(clip.toChildTime(time), without))
                return true;
        }

        return false;
    }

    // The clip drawn at the time on the lane — the later laid where two meet — -1 when none is
    public int findClipAt(float time, int lane) {

        for (int i = clips.size() - 1; i >= 0; i--) {

            RecordingClipStruct clip = clips.get(i);

            if (clip.getLane() == lane && time >= clip.getStart() && time <= clip.getEnd())
                return i;
        }

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Mix \\

    // How loud the lane is heard: its volume, nothing while it is muted or another lane is soloed and it is not
    public float resolveLaneGain(int lane) {

        RecordingLaneStruct mixed = lanes[Math.clamp(lane, 0, lanes.length - 1)];

        if (mixed.isMuted() || !mixed.isSolo() && isSoloed())
            return 0f;

        return mixed.getVolume();
    }

    private boolean isSoloed() {

        for (int i = 0; i < lanes.length; i++)
            if (lanes[i].isSolo())
                return true;

        return false;
    }

    // Grid \\

    // The ticks of a beat: a whole note's ticks shared by the beat unit
    public int getBeatTicks() {
        return EngineSetting.SONG_TICKS_PER_BEAT * EditorSetting.RECORDER_QUARTERS_PER_WHOLE / beatUnit;
    }

    public int getBarTicks() {
        return getBeatTicks() * beatsPerBar;
    }

    // The moment of the grid line nearest the time, the grid ruled every so many ticks of the tempo map
    public float snapToGrid(float time, int gridTicks) {

        double ticks = tempo.toTicks(Math.max(0f, time));

        return (float) tempo.toSeconds(Math.round(ticks / gridTicks) * (double) gridTicks);
    }

    // Accessible \\

    public RecordingKind getKind() {
        return kind;
    }

    public int getLaneCount() {
        return lanes.length;
    }

    public RecordingLaneStruct getLane(int lane) {
        return lanes[lane];
    }

    public void setTempo(SongTempoStruct tempo) {
        this.tempo = tempo;
    }

    public void setMeter(int beatsPerBar, int beatUnit) {
        this.beatsPerBar = beatsPerBar;
        this.beatUnit = beatUnit;
    }

    public int getKeyRoot() {
        return keyRoot;
    }

    public NoteScale getKeyScale() {
        return keyScale;
    }

    public void setKey(int keyRoot, NoteScale keyScale) {
        this.keyRoot = keyRoot;
        this.keyScale = keyScale;
    }

    // Accessible \\

    public String getRecordingName() {
        return recordingName;
    }

    public void setRecordingName(String recordingName) {
        this.recordingName = recordingName;
    }

    // The name a Music file's song is shown under in the game, none to show its file's
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getWorldName() {
        return worldName;
    }

    public SongTempoStruct getTempo() {
        return tempo;
    }

    public int getBeatsPerBar() {
        return beatsPerBar;
    }

    public int getBeatUnit() {
        return beatUnit;
    }

    public ObjectArrayList<RecordingTakeStruct> getTakes() {
        return takes;
    }

    public ObjectArrayList<RecordingClipStruct> getClips() {
        return clips;
    }

    public RecordingExportStruct getExportStruct() {
        return exportStruct;
    }

    public float getDuration() {

        float duration = 0f;

        for (int i = 0; i < clips.size(); i++)
            duration = Math.max(duration, clips.get(i).getEnd());

        return duration;
    }
}
