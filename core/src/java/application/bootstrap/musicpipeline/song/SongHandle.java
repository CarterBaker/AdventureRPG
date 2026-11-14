package application.bootstrap.musicpipeline.song;

import application.bootstrap.musicpipeline.util.NoteScale;
import engine.root.HandlePackage;

public class SongHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded song. A song from the project's assets
     * is registered and owned by SongManager for the engine lifetime and
     * shared by every clip that lays it and every player that plays it; a
     * player's song is kept by SongManager while something plays it.
     * Delegates all accessors through SongData.
     */

    // Internal
    private SongData songData;

    // Constructor \\

    public void constructor(SongData songData) {

        // Internal
        this.songData = songData;
    }

    // Accessible \\

    public SongData getSongData() {
        return songData;
    }

    public String getSongName() {
        return songData.getSongName();
    }

    public short getSongID() {
        return songData.getSongID();
    }

    public boolean isUserSong() {
        return songData.isUserSong();
    }

    public String getDisplayName() {
        return songData.getDisplayName();
    }

    public SongTempoStruct getTempo() {
        return songData.getTempo();
    }

    public int getBeatsPerBar() {
        return songData.getBeatsPerBar();
    }

    public int getBeatUnit() {
        return songData.getBeatUnit();
    }

    public int getTicksPerBar() {
        return songData.getTicksPerBar();
    }

    public int getKeyRoot() {
        return songData.getKeyRoot();
    }

    public NoteScale getKeyScale() {
        return songData.getKeyScale();
    }

    public int getLaneCount() {
        return songData.getLaneCount();
    }

    public SongLaneStruct getLane(int lane) {
        return songData.getLane(lane);
    }

    public boolean isLaneHeard(int lane) {
        return songData.isLaneHeard(lane);
    }

    public int getClipCount() {
        return songData.getClipCount();
    }

    public SongClipStruct getClip(int clip) {
        return songData.getClip(clip);
    }

    public int getLength() {
        return songData.getLength();
    }
}
