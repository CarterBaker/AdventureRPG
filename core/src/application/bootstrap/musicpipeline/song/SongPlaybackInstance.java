package application.bootstrap.musicpipeline.song;

import application.bootstrap.musicpipeline.instrument.InstrumentHandle;
import application.bootstrap.musicpipeline.instrumentmanager.InstrumentManager;
import application.bootstrap.musicpipeline.notesetmanager.NoteSetManager;
import application.bootstrap.musicpipeline.samplermanager.SamplerManager;
import application.bootstrap.musicpipeline.songmanager.SongManager;
import application.bootstrap.musicpipeline.util.NoteUtility;
import application.bootstrap.musicpipeline.util.SongFlattenUtility;
import application.bootstrap.soundpipeline.soundmanager.SoundManager;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SongPlaybackInstance extends InstancePackage {

    /*
     * Plays one song, as a sequencer plays an arrangement, for the system that
     * owns it and calls update every frame. play lays the song out flat once —
     * one lane of it or all, nested songs opened in place with their offset
     * and transpose — resolves every instrument once, an override standing in
     * for each when given, and asks for their samples; the clock starts once
     * they have all landed. Each update turns the seconds played into a tick
     * through the song's tempo map and walks every clip's packed notes from
     * the last tick to it, letting go the notes whose end has passed and
     * sounding those whose start has, through SamplerManager at the volume
     * their clips and lanes give them; a sound clip plays its sound through
     * SoundManager from as far in as the clip starts and stops where it ends.
     * A player's song is held by name while it plays and let go when it stops
     * or ends. A song can instead follow a clock its owner drives, as the
     * recorder's timeline does: follow lays it out the same way and followTo
     * walks it to the tick the owner's clock has reached, passing over every
     * note before the first tick it is given, so it plays from wherever the
     * clock stands; every note it sounds is scaled by the volume it follows
     * at. Played on an override instrument, the song is moved by the whole
     * octaves that centre its notes in the instrument's range and any note
     * still outside is moved into it by octaves, so every note sounds. Placed
     * at someone's position, its notes are heard from there and fade with
     * distance. Every cursor and held note is kept on the instance, so playing
     * allocates nothing once a song of its size has been played.
     */

    // Internal
    private SongManager songManager;
    private SamplerManager samplerManager;
    private SoundManager soundManager;
    private InstrumentManager instrumentManager;
    private NoteSetManager noteSetManager;

    // Song
    private SongHandle songHandle;
    private SongTempoStruct tempo;
    private int length;

    // Layout — the song's notes and sound clips laid out flat, reused from one play to the next
    private ObjectArrayList<SongCursorStruct> cursors;
    private int cursorCount;

    // Held — every note sounding, with the tick it is let go at
    private int[] heldNotes;
    private int[] heldEndTicks;
    private int heldCount;

    // Fit — the octaves the song moves by and the range it is moved into, when it plays on an override instrument
    private boolean fitting;
    private int fitShift;
    private int fitLow;
    private int fitHigh;

    // Place — where the notes are heard from, null for heard as they are
    private WorldHandle placeWorldHandle;
    private WorldPositionStruct place;

    // Clock
    private boolean playing;
    private boolean ready;
    private double secondsPlayed;
    private int lastTick;
    private float volume;

    // Internal \\

    @Override
    protected void create() {

        // Layout
        this.cursors = new ObjectArrayList<>();

        // Held
        this.heldNotes = new int[EngineSetting.SONG_PLAYBACK_HELD_NOTE_COUNT];
        this.heldEndTicks = new int[EngineSetting.SONG_PLAYBACK_HELD_NOTE_COUNT];

        // Clock
        this.lastTick = EngineSetting.SONG_PLAYBACK_START_TICK;
        this.volume = EngineSetting.SAMPLER_NOTE_FULL_VOLUME;
    }

    @Override
    protected void get() {
        this.songManager = get(SongManager.class);
        this.samplerManager = get(SamplerManager.class);
        this.soundManager = get(SoundManager.class);
        this.instrumentManager = get(InstrumentManager.class);
        this.noteSetManager = get(NoteSetManager.class);
    }

    // Play \\

    // One of the project's songs from its start, on one lane or SONG_LANE_ALL, every notes clip on the override
    // instrument unless it is INSTRUMENT_NONE
    public void play(short songID, short instrumentOverride, int lane) {
        start(songManager.getSongHandleFromSongID(songID), instrumentOverride, lane);
    }

    // A player's song from its start, read from their MIDI file by name; false, playing nothing, when it cannot be
    public boolean playUserSong(String songName, short instrumentOverride, int lane) {

        SongHandle userSongHandle = songManager.loadUserSong(songName);

        if (userSongHandle == null) {
            stop();
            return false;
        }

        start(userSongHandle, instrumentOverride, lane);
        return true;
    }

    // The one way a song begins from its start on its own clock
    private void start(SongHandle handle, short instrumentOverride, int lane) {
        lay(handle, handle.getSongData(), instrumentOverride, lane, EngineSetting.SAMPLER_NOTE_FULL_VOLUME);
    }

    // The one way a song is laid out to play: its instruments and sounds resolved, its samples asked for
    private void lay(SongHandle handle, SongData songData, short instrumentOverride, int lane, float volume) {

        stop();

        this.songHandle = handle;
        this.tempo = songData.getTempo();
        this.length = songData.getLength();
        this.cursorCount = SongFlattenUtility.flatten(songData, lane, cursors);

        for (int i = 0; i < cursorCount; i++)
            resolve(cursors.get(i), instrumentOverride);

        fit(instrumentOverride);
        this.playing = true;
        this.ready = false;
        this.secondsPlayed = 0.0;
        this.lastTick = EngineSetting.SONG_PLAYBACK_START_TICK;
        this.volume = volume;
    }

    // The octaves that centre the song's notes in the override instrument's range, worked out once as it is laid
    private void fit(short instrumentOverride) {

        this.fitting = instrumentOverride != EngineSetting.INSTRUMENT_NONE;

        if (!fitting)
            return;

        InstrumentHandle instrument = instrumentManager.getInstrumentHandleFromInstrumentID(instrumentOverride);
        int lowest = EngineSetting.NOTE_PITCH_MAX;
        int highest = EngineSetting.NOTE_PITCH_MIN;

        for (int i = 0; i < cursorCount; i++) {

            SongCursorStruct cursor = cursors.get(i);

            if (cursor.getKind() != SongClipKind.NOTES)
                continue;

            for (int note = 0; note < cursor.getClip().getNoteCount(); note++) {

                if (!cursor.playsNote(note))
                    continue;

                lowest = Math.min(lowest, cursor.getNotePitch(note));
                highest = Math.max(highest, cursor.getNotePitch(note));
            }
        }

        this.fitLow = instrument.getLow();
        this.fitHigh = instrument.getHigh();
        this.fitShift = lowest <= highest ? NoteUtility.resolveOctaveShift(lowest, highest, fitLow, fitHigh) : 0;
    }

    // Place \\

    // Every note sounded from now on is heard from the place, as it stands when each note begins; null for none
    public void setPlace(WorldHandle worldHandle, WorldPositionStruct place) {
        this.placeWorldHandle = worldHandle;
        this.place = place;
    }

    // Follow \\

    // The song laid out to follow a clock its owner drives, every note it sounds scaled by the volume
    public void follow(SongData songData, float volume) {
        lay(null, songData, EngineSetting.INSTRUMENT_NONE, EngineSetting.SONG_LANE_ALL, volume);
    }

    // How loud the notes it sounds from now on play, 0 to 1, as the clip it follows for fades
    public void setVolume(float volume) {
        this.volume = volume;
    }

    // The song walked to the tick its owner's clock has reached; the first tick it is given once its samples have
    // landed is where it starts, every note before it passed over
    public void followTo(double tick) {

        if (!playing || !isReady())
            return;

        int at = (int) tick;

        if (lastTick == EngineSetting.SONG_PLAYBACK_START_TICK)
            skipTo(at);

        letGoNotes(at);

        for (int i = 0; i < cursorCount; i++)
            walk(cursors.get(i), at);

        this.lastTick = at;
    }

    // Every cursor moved on to its first note at or after the tick, and the clock set just before it
    private void skipTo(int tick) {

        for (int i = 0; i < cursorCount; i++) {

            SongCursorStruct cursor = cursors.get(i);

            if (cursor.getKind() == SongClipKind.NOTES)
                cursor.setNextNote(findFirstNote(cursor, tick));
        }

        this.lastTick = tick - 1;
    }

    private void resolve(SongCursorStruct cursor, short instrumentOverride) {

        if (cursor.getKind() == SongClipKind.SOUND) {
            cursor.setSoundID(soundManager.findSoundID(cursor.getClip().getSoundName()));
            return;
        }

        if (instrumentOverride != EngineSetting.INSTRUMENT_NONE)
            cursor.setInstrumentID(instrumentOverride);

        noteSetManager.prepare(resolveNoteSetID(cursor));
        cursor.setNextNote(findFirstNote(cursor, cursor.getWindowStart()));
    }

    // The first of the clip's notes that begins inside its window at or after the tick
    private int findFirstNote(SongCursorStruct cursor, int tick) {

        int from = Math.max(tick, cursor.getWindowStart());
        int note = 0;

        while (note < cursor.getClip().getNoteCount() && cursor.getNoteTick(note) < from)
            note++;

        return note;
    }

    // Stop \\

    // Every held note let go, every sound stopped and a player's song let go of
    public void stop() {

        for (int i = 0; i < heldCount; i++)
            samplerManager.noteOff(heldNotes[i]);

        for (int i = 0; i < cursorCount; i++)
            stopSound(cursors.get(i));

        if (songHandle != null && songHandle.isUserSong())
            songManager.releaseUserSong(songHandle);

        this.heldCount = 0;
        this.cursorCount = 0;
        this.songHandle = null;
        this.playing = false;
    }

    // Update \\

    // The song moved on by the seconds given: ended notes let go, then every note and sound reached begun
    public void update(float deltaTime) {

        if (!playing || !isReady())
            return;

        if (lastTick != EngineSetting.SONG_PLAYBACK_START_TICK)
            this.secondsPlayed += deltaTime;

        int tick = (int) tempo.toTicks(secondsPlayed);

        letGoNotes(tick);

        for (int i = 0; i < cursorCount; i++)
            walk(cursors.get(i), tick);

        this.lastTick = tick;

        if (tick >= length && heldCount == 0)
            stop();
    }

    // Whether every instrument's samples have landed, so the clock may start
    private boolean isReady() {

        if (ready)
            return true;

        for (int i = 0; i < cursorCount; i++) {

            SongCursorStruct cursor = cursors.get(i);

            if (cursor.getKind() == SongClipKind.NOTES && !noteSetManager.isReady(resolveNoteSetID(cursor)))
                return false;
        }

        this.ready = true;
        return true;
    }

    // The clip walked from the last tick to this one
    private void walk(SongCursorStruct cursor, int tick) {

        if (cursor.getKind() == SongClipKind.SOUND) {
            walkSound(cursor, tick);
            return;
        }

        SongClipStruct clip = cursor.getClip();
        int note = cursor.getNextNote();

        for (; note < clip.getNoteCount() && cursor.getNoteTick(note) <= tick; note++)
            if (cursor.playsNote(note))
                playNote(cursor, note);

        cursor.setNextNote(note);
    }

    private void playNote(SongCursorStruct cursor, int note) {

        short instrumentID = cursor.getInstrumentID();
        int pitch = resolvePitch(cursor.getNotePitch(note));
        int velocity = cursor.getClip().getNoteVelocity(note);
        float gain = cursor.getGain() * volume;
        int noteHandle = place != null
                ? samplerManager.noteOnAt(instrumentID, pitch, velocity, gain, placeWorldHandle, place)
                : samplerManager.noteOn(instrumentID, pitch, velocity, gain);

        if (noteHandle != EngineSetting.INDEX_NOT_FOUND)
            hold(noteHandle, cursor.getNoteEndTick(note));
    }

    // A sound clip begun as the clock reaches its start, from as far into its sound as the clip starts, and
    // stopped as the clock reaches its end
    private void walkSound(SongCursorStruct cursor, int tick) {

        int windowStart = cursor.getWindowStart();

        if (cursor.getSoundID() != EngineSetting.INDEX_NOT_FOUND && windowStart > lastTick && windowStart <= tick)
            cursor.setVoice(soundManager.playSound(
                    cursor.getSoundID(),
                    cursor.getGain() * volume,
                    NoteUtility.toPitchRatio(cursor.getTranspose(), 0, 0f),
                    (float) (tempo.toSeconds(windowStart) - tempo.toSeconds(cursor.toSongTick(0)))));

        if (cursor.getWindowEnd() <= tick)
            stopSound(cursor);
    }

    private void stopSound(SongCursorStruct cursor) {

        if (cursor.getVoice() == EngineSetting.INDEX_NOT_FOUND)
            return;

        soundManager.stopVoice(cursor.getVoice());
        cursor.setVoice(EngineSetting.INDEX_NOT_FOUND);
    }

    // Held \\

    // A note kept until its end; with every place taken, the note ending soonest is let go for it
    private void hold(int noteHandle, int endTick) {

        if (heldCount == heldNotes.length)
            letGo(findSoonestEnd());

        heldNotes[heldCount] = noteHandle;
        heldEndTicks[heldCount] = endTick;
        heldCount++;
    }

    private void letGoNotes(int tick) {

        for (int i = heldCount - 1; i >= 0; i--)
            if (heldEndTicks[i] <= tick)
                letGo(i);
    }

    // The one way a held note is let go
    private void letGo(int held) {

        samplerManager.noteOff(heldNotes[held]);
        heldCount--;
        heldNotes[held] = heldNotes[heldCount];
        heldEndTicks[held] = heldEndTicks[heldCount];
    }

    private int findSoonestEnd() {

        int soonest = 0;

        for (int i = 1; i < heldCount; i++)
            if (heldEndTicks[i] < heldEndTicks[soonest])
                soonest = i;

        return soonest;
    }

    // Utility \\

    // The pitch a note sounds at, moved into the override instrument's range when the song is fitted to one
    private int resolvePitch(int pitch) {
        return fitting ? NoteUtility.fitToRange(pitch + fitShift, fitLow, fitHigh) : pitch;
    }

    private short resolveNoteSetID(SongCursorStruct cursor) {
        return instrumentManager.getInstrumentHandleFromInstrumentID(cursor.getInstrumentID()).getNoteSetID();
    }

    // Accessible \\

    public boolean isPlaying() {
        return playing;
    }

    // Whether the song is waiting for its instruments' samples before its clock starts
    public boolean isWaiting() {
        return playing && !ready;
    }

    public SongHandle getSongHandle() {
        return songHandle;
    }

    // The tick the song has reached, -1 before it starts
    public int getTick() {
        return lastTick;
    }

    public double getSecondsPlayed() {
        return secondsPlayed;
    }

    // How many notes are held now, each by the handle SamplerManager gave it
    public int getHeldCount() {
        return heldCount;
    }

    public int getHeldNote(int held) {
        return heldNotes[held];
    }
}
