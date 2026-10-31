package editor.bootstrap.recorderpipeline.recording;

import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecordingInstance extends InstancePackage {

    /*
     * One recording open in the recorder: its name, the world it was made in,
     * its bin of takes in the order they were recorded, the clips laid on its
     * timeline, and how it exports. It runs as long as its last clip reaches.
     * For each channel the clip on the highest lane covering a moment is the
     * one that plays it, the later laid of two on the same lane; a transition
     * also asks what plays with one clip set aside and its neighbour carried
     * on past its own span.
     */

    // Identity
    private String recordingName;
    private String worldName;

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

    public void constructor(String recordingName, String worldName, RecordingExportStruct exportStruct) {

        // Identity
        this.recordingName = recordingName;
        this.worldName = worldName;

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

    // The clip playing the channel at the time — the highest lane, then the later laid — null when none does
    public RecordingClipStruct findPlayingClip(RecorderChannel channel, float time) {

        RecordingClipStruct playing = null;

        for (int i = 0; i < clips.size(); i++) {

            RecordingClipStruct clip = clips.get(i);

            if (clip.covers(channel, time) && (playing == null || clip.getLane() >= playing.getLane()))
                playing = clip;
        }

        return playing;
    }

    // The clip playing the channel at the time as the timeline plays without one clip, with another carried on past
    // its own span as though it still covered the time; null when none does
    public RecordingClipStruct findPlayingClipWithout(
            RecorderChannel channel,
            float time,
            RecordingClipStruct without,
            RecordingClipStruct carried) {

        RecordingClipStruct playing = null;

        for (int i = 0; i < clips.size(); i++) {

            RecordingClipStruct clip = clips.get(i);
            boolean covering = clip == carried
                    ? !clip.isMuted() && clip.holds(channel)
                    : clip.covers(channel, time);

            if (clip != without && covering && (playing == null || clip.getLane() >= playing.getLane()))
                playing = clip;
        }

        return playing;
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

    // True when any clip but the one shows a picture at the time
    public boolean showsPictureWithout(float time, RecordingClipStruct without) {

        for (int i = 0; i < clips.size(); i++)
            if (clips.get(i) != without && clips.get(i).showsPictureAt(time))
                return true;

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

    // Accessible \\

    public String getRecordingName() {
        return recordingName;
    }

    public void setRecordingName(String recordingName) {
        this.recordingName = recordingName;
    }

    public String getWorldName() {
        return worldName;
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
