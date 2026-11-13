package application.bootstrap.musicpipeline.song;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class SongCursorStruct extends StructPackage {

    /*
     * One notes or sound clip as a song lays it out flat, nested songs
     * included: the clip, the lane of the played song it sounds on, the shift
     * from its source's ticks to the played song's, the window of the played
     * song's ticks it sounds within, and the transpose and gain every song
     * clip around it adds up to. Playback keeps how far through the clip's
     * notes it has walked, the instrument it plays them on and the sound and
     * voice a sound clip plays through; reused from one play to the next, so
     * playing allocates nothing once the cursors exist.
     */

    // Layout
    private SongClipStruct clip;
    private int lane;
    private int shift;
    private int windowStart;
    private int windowEnd;
    private int transpose;
    private float gain;

    // Playback
    private short instrumentID;
    private int soundID;
    private int nextNote;
    private int voice;

    // Management \\

    // The cursor takes up a clip as laid out, its playback state back at the clip's start
    public void begin(
            SongClipStruct clip,
            int lane,
            int shift,
            int windowStart,
            int windowEnd,
            int transpose,
            float gain) {

        // Layout
        this.clip = clip;
        this.lane = lane;
        this.shift = shift;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.transpose = transpose;
        this.gain = gain;

        // Playback
        this.instrumentID = clip.getInstrumentID();
        this.soundID = EngineSetting.INDEX_NOT_FOUND;
        this.nextNote = 0;
        this.voice = EngineSetting.INDEX_NOT_FOUND;
    }

    public void setInstrumentID(short instrumentID) {
        this.instrumentID = instrumentID;
    }

    public void setSoundID(int soundID) {
        this.soundID = soundID;
    }

    public void setNextNote(int nextNote) {
        this.nextNote = nextNote;
    }

    public void setVoice(int voice) {
        this.voice = voice;
    }

    // Notes \\

    // The played song's tick a tick of the clip's source falls at
    public int toSongTick(int sourceTick) {
        return sourceTick + shift;
    }

    // The played song's tick the clip's note begins at
    public int getNoteTick(int note) {
        return clip.getNoteStart(note) + shift;
    }

    // The played song's tick the clip's note ends at, cut where the clip's window ends
    public int getNoteEndTick(int note) {
        return Math.min(getNoteTick(note) + clip.getNoteLength(note), windowEnd);
    }

    // Whether the note begins inside the window, so it plays
    public boolean playsNote(int note) {

        int tick = getNoteTick(note);

        return tick >= windowStart && tick < windowEnd;
    }

    // The pitch the note sounds, transposed; outside 0 to 127 it does not sound
    public int getNotePitch(int note) {
        return clip.getNotePitch(note) + transpose;
    }

    // Accessible \\

    public SongClipStruct getClip() {
        return clip;
    }

    public SongClipKind getKind() {
        return clip.getKind();
    }

    public int getLane() {
        return lane;
    }

    public int getShift() {
        return shift;
    }

    public int getWindowStart() {
        return windowStart;
    }

    public int getWindowEnd() {
        return windowEnd;
    }

    public int getTranspose() {
        return transpose;
    }

    public float getGain() {
        return gain;
    }

    public short getInstrumentID() {
        return instrumentID;
    }

    public int getSoundID() {
        return soundID;
    }

    public int getNextNote() {
        return nextNote;
    }

    public int getVoice() {
        return voice;
    }
}
