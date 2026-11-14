package application.bootstrap.musicpipeline.song;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class SongClipStruct extends StructPackage {

    /*
     * One clip of a song, laid on a lane from its start for its length in
     * ticks, playing its source from the offset ticks into it, transposed by
     * whole semitones and at a volume of 0 to 1 unless muted. A notes clip
     * plays its own notes on an instrument, packed four ints to a note — its
     * start and length in ticks from the clip's start, its pitch and its
     * velocity — in order of their start; a song clip plays another song; a
     * sound clip plays a sound. What a clip names is kept by name, so it is
     * written back as it was read, and the instrument or song resolved once
     * when the song is built.
     */

    // Placement
    private final SongClipKind kind;
    private final int lane;
    private final int start;
    private final int length;
    private final int offset;

    // Mix
    private final boolean muted;
    private final float volume;
    private final int transpose;

    // Notes
    private final String instrumentName;
    private short instrumentID;
    private final int[] notes;

    // Song
    private final String songName;
    private SongHandle songHandle;

    // Sound
    private final String soundName;

    // Constructor \\

    public SongClipStruct(
            SongClipKind kind,
            int lane,
            int start,
            int length,
            int offset,
            boolean muted,
            float volume,
            int transpose,
            String instrumentName,
            int[] notes,
            String songName,
            String soundName) {

        // Placement
        this.kind = kind;
        this.lane = lane;
        this.start = start;
        this.length = length;
        this.offset = offset;

        // Mix
        this.muted = muted;
        this.volume = volume;
        this.transpose = transpose;

        // Notes
        this.instrumentName = instrumentName;
        this.instrumentID = EngineSetting.INSTRUMENT_NONE;
        this.notes = notes;

        // Song
        this.songName = songName;

        // Sound
        this.soundName = soundName;
    }

    // Resolve \\

    // The instrument a notes clip plays, resolved once when its song is built
    public void resolveInstrument(short instrumentID) {
        this.instrumentID = instrumentID;
    }

    // The song a song clip plays, resolved once when its song is built
    public void resolveSong(SongHandle songHandle) {
        this.songHandle = songHandle;
    }

    // Notes \\

    public int getNoteCount() {
        return notes.length / EngineSetting.SONG_NOTE_STRIDE;
    }

    public int getNoteStart(int note) {
        return notes[note * EngineSetting.SONG_NOTE_STRIDE + EngineSetting.SONG_NOTE_START];
    }

    public int getNoteLength(int note) {
        return notes[note * EngineSetting.SONG_NOTE_STRIDE + EngineSetting.SONG_NOTE_LENGTH];
    }

    public int getNotePitch(int note) {
        return notes[note * EngineSetting.SONG_NOTE_STRIDE + EngineSetting.SONG_NOTE_PITCH];
    }

    public int getNoteVelocity(int note) {
        return notes[note * EngineSetting.SONG_NOTE_STRIDE + EngineSetting.SONG_NOTE_VELOCITY];
    }

    // Accessible \\

    public SongClipKind getKind() {
        return kind;
    }

    public int getLane() {
        return lane;
    }

    public int getStart() {
        return start;
    }

    public int getLength() {
        return length;
    }

    // The tick the clip ends at, where the next may start
    public int getEnd() {
        return start + length;
    }

    public int getOffset() {
        return offset;
    }

    public boolean isMuted() {
        return muted;
    }

    public float getVolume() {
        return volume;
    }

    public int getTranspose() {
        return transpose;
    }

    public String getInstrumentName() {
        return instrumentName;
    }

    public short getInstrumentID() {
        return instrumentID;
    }

    // The packed notes as they are kept, for the codec alone to read
    public int[] getNotes() {
        return notes;
    }

    public String getSongName() {
        return songName;
    }

    public SongHandle getSongHandle() {
        return songHandle;
    }

    public String getSoundName() {
        return soundName;
    }
}
