package application.bootstrap.musicpipeline.song;

import application.bootstrap.musicpipeline.util.NoteScale;
import engine.root.DataPackage;
import engine.root.EngineSetting;

public class SongData extends DataPackage {

    /*
     * Immutable song, built from ARPG or read from a MIDI file — an
     * arrangement as a DAW lays one out: the name it is shown under, its tempo
     * map, the beats to a bar and the note value of a beat, the key it is
     * written in, its lanes and the clips laid on them. Its length is where
     * its last clip ends. A song from the project's assets holds the ID
     * SongManager assigned it; a player's song, read from their MIDI files,
     * holds none and is kept apart from the palette. Owned by SongHandle.
     */

    // Identity
    private final String songName;
    private final short songID;
    private final String displayName;

    // Time
    private final SongTempoStruct tempo;
    private final int beatsPerBar;
    private final int beatUnit;

    // Key
    private final int keyRoot;
    private final NoteScale keyScale;

    // Arrangement
    private final SongLaneStruct[] lanes;
    private final SongClipStruct[] clips;
    private final boolean soloed;
    private final int length;

    // Constructor \\

    public SongData(
            String songName,
            short songID,
            String displayName,
            SongTempoStruct tempo,
            int beatsPerBar,
            int beatUnit,
            int keyRoot,
            NoteScale keyScale,
            SongLaneStruct[] lanes,
            SongClipStruct[] clips) {

        // Identity
        this.songName = songName;
        this.songID = songID;
        this.displayName = displayName;

        // Time
        this.tempo = tempo;
        this.beatsPerBar = beatsPerBar;
        this.beatUnit = beatUnit;

        // Key
        this.keyRoot = keyRoot;
        this.keyScale = keyScale;

        // Arrangement
        this.lanes = lanes;
        this.clips = clips;
        this.soloed = findSolo(lanes);
        this.length = findLength(clips);
    }

    private static boolean findSolo(SongLaneStruct[] lanes) {

        for (int i = 0; i < lanes.length; i++)
            if (lanes[i].isSolo())
                return true;

        return false;
    }

    private static int findLength(SongClipStruct[] clips) {

        int end = 0;

        for (int i = 0; i < clips.length; i++)
            end = Math.max(end, clips[i].getEnd());

        return end;
    }

    // Lanes \\

    // Whether what is laid on the lane plays: unmuted, and soloed while any lane is
    public boolean isLaneHeard(int lane) {

        SongLaneStruct laneStruct = lanes[lane];

        return !laneStruct.isMuted() && (!soloed || laneStruct.isSolo());
    }

    // Accessible \\

    public String getSongName() {
        return songName;
    }

    public short getSongID() {
        return songID;
    }

    // Whether the song is a player's, read from their MIDI files and kept apart from the palette
    public boolean isUserSong() {
        return songID == EngineSetting.SONG_NONE;
    }

    public String getDisplayName() {
        return displayName;
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

    // The ticks a bar spans: its beats, each the note value of the beat unit
    public int getTicksPerBar() {
        return beatsPerBar * EngineSetting.SONG_TICKS_PER_BEAT * EngineSetting.SONG_QUARTER_NOTE_UNIT / beatUnit;
    }

    public int getKeyRoot() {
        return keyRoot;
    }

    public NoteScale getKeyScale() {
        return keyScale;
    }

    public int getLaneCount() {
        return lanes.length;
    }

    public SongLaneStruct getLane(int lane) {
        return lanes[lane];
    }

    public int getClipCount() {
        return clips.length;
    }

    public SongClipStruct getClip(int clip) {
        return clips[clip];
    }

    // The tick the song's last clip ends at
    public int getLength() {
        return length;
    }
}
