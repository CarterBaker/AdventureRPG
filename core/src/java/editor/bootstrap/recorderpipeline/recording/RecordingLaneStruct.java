package editor.bootstrap.recorderpipeline.recording;

import application.bootstrap.musicpipeline.song.SongLaneStruct;
import engine.root.EngineSetting;
import engine.root.StructPackage;

public class RecordingLaneStruct extends StructPackage {

    /*
     * One lane of a recording's timeline as a Music file mixes it, a DAW's
     * track strip: the name it is shown under, how loud everything on it
     * plays, 0 to 1, where it sits from left (-1) to right (1), whether it is
     * muted and whether it is soloed. A Video's lanes keep every default.
     * Read from and written to the song's own lane through the song codec.
     */

    // Identity
    private String name;

    // Mix
    private float volume;
    private float pan;
    private boolean muted;
    private boolean solo;

    // Constructor \\

    public RecordingLaneStruct() {
        this.name = EngineSetting.SONG_LANE_NAME_NONE;
        this.volume = EngineSetting.DEFAULT_SONG_LANE_VOLUME;
        this.pan = EngineSetting.DEFAULT_SONG_LANE_PAN;
        this.muted = EngineSetting.DEFAULT_SONG_LANE_MUTED;
        this.solo = EngineSetting.DEFAULT_SONG_LANE_SOLO;
    }

    // Song \\

    public void readFrom(SongLaneStruct lane) {
        this.name = lane.getName();
        this.volume = lane.getVolume();
        this.pan = lane.getPan();
        this.muted = lane.isMuted();
        this.solo = lane.isSolo();
    }

    public SongLaneStruct toSongLane() {
        return new SongLaneStruct(name, volume, pan, muted, solo);
    }

    // Accessible \\

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = Math.clamp(volume, 0f, 1f);
    }

    public float getPan() {
        return pan;
    }

    public void setPan(float pan) {
        this.pan = Math.clamp(pan, -1f, 1f);
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isSolo() {
        return solo;
    }

    public void setSolo(boolean solo) {
        this.solo = solo;
    }
}
