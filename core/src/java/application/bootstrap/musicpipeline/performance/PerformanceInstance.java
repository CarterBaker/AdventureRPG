package application.bootstrap.musicpipeline.performance;

import java.util.Arrays;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.entitypipeline.entity.CombatDirection;
import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityActionHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.musicpipeline.instrument.InstrumentHandle;
import application.bootstrap.musicpipeline.instrumentmanager.InstrumentManager;
import application.bootstrap.musicpipeline.notesetmanager.NoteSetManager;
import application.bootstrap.musicpipeline.samplermanager.SamplerManager;
import application.bootstrap.musicpipeline.song.SongPlaybackInstance;
import application.bootstrap.musicpipeline.util.NoteUtility;
import engine.root.EngineSetting;
import engine.root.InstancePackage;

public class PerformanceInstance extends InstancePackage {

    /*
     * An entity playing the instrument it holds, for the system that drives
     * it and calls update every frame. begin takes up an instrument the
     * entity's rig has clips for, asks for its samples and raises the PLAY
     * stance with the instrument's hold clip; while any note sounds the play
     * clip loops instead, staying a moment past the last so quick notes do
     * not flicker between the two. Keys play the thirteen notes from the
     * octave's C to the next, moved into the instrument's range by octaves,
     * as placed notes heard from where the entity stands; the octave and the
     * velocity step up and down. A song part plays through one reused
     * SongPlaybackInstance on the instrument, fitted to its range and placed
     * at the entity. A stance cut short by a blow is raised again once the
     * hands are free. end lets every note go, stops the song and lowers the
     * stance. Nothing allocates once begun.
     */

    // Internal
    private InstrumentManager instrumentManager;
    private NoteSetManager noteSetManager;
    private SamplerManager samplerManager;

    // Performer
    private EntityInstance entity;
    private InstrumentHandle instrumentHandle;
    private AnimationClipHandle holdClip;
    private AnimationClipHandle playClip;
    private AnimationClipHandle shownClip;
    private float sinceSounding;

    // Keys — the note each key holds, -1 for none
    private int[] keyNotes;
    private int baseOctavePitch;
    private int velocity;

    // Song
    private SongPlaybackInstance songPlayback;

    // Internal \\

    @Override
    protected void create() {

        // Keys
        this.keyNotes = new int[EngineSetting.PERFORMANCE_KEY_COUNT];
        Arrays.fill(keyNotes, EngineSetting.INDEX_NOT_FOUND);

        // Song
        this.songPlayback = create(SongPlaybackInstance.class);
    }

    @Override
    protected void get() {
        this.instrumentManager = get(InstrumentManager.class);
        this.noteSetManager = get(NoteSetManager.class);
        this.samplerManager = get(SamplerManager.class);
    }

    // Begin / End \\

    // Whether the instrument has clips for the entity's rig, so the entity can play it
    public boolean canPlay(EntityInstance entity, short instrumentID) {
        return instrumentManager.getInstrumentHandleFromInstrumentID(instrumentID).hasRig(resolveRig(entity));
    }

    // The entity takes up the instrument; false, doing nothing, when its rig has no clips for it
    public boolean begin(EntityInstance entity, short instrumentID) {

        if (!canPlay(entity, instrumentID))
            return false;

        end();

        InstrumentHandle instrument = instrumentManager.getInstrumentHandleFromInstrumentID(instrumentID);
        RigHandle rigHandle = resolveRig(entity);

        this.entity = entity;
        this.instrumentHandle = instrument;
        this.holdClip = instrument.getHoldClip(rigHandle);
        this.playClip = instrument.getPlayClip(rigHandle);
        this.shownClip = null;
        this.sinceSounding = EngineSetting.PERFORMANCE_PLAY_LINGER_SECONDS;
        this.baseOctavePitch = resolveCentreOctave(instrument);
        this.velocity = EngineSetting.PERFORMANCE_VELOCITY_DEFAULT;

        noteSetManager.prepare(instrument.getNoteSetID());
        songPlayback.setPlace(entity.getWorldHandle(), entity.getWorldPositionStruct());
        animate();
        return true;
    }

    public void end() {

        if (entity == null)
            return;

        releaseKeys();
        songPlayback.stop();
        songPlayback.setPlace(null, null);

        EntityActionHandle actionHandle = entity.getEntityActionHandle();

        if (actionHandle.getAction() == EntityAction.PLAY)
            actionHandle.release();

        this.entity = null;
        this.instrumentHandle = null;
        this.shownClip = null;
    }

    // Keys \\

    // The key's note sounds from the entity, the key's last note let go first
    public void press(int key) {

        if (entity == null)
            return;

        release(key);
        keyNotes[key] = samplerManager.noteOnAt(
                instrumentHandle.getInstrumentID(),
                resolveKeyPitch(key),
                velocity,
                entity.getWorldHandle(),
                entity.getWorldPositionStruct());
    }

    // Whether the key holds a note it sounded
    public boolean isPressed(int key) {
        return keyNotes[key] != EngineSetting.INDEX_NOT_FOUND;
    }

    public void release(int key) {

        if (keyNotes[key] == EngineSetting.INDEX_NOT_FOUND)
            return;

        samplerManager.noteOff(keyNotes[key]);
        keyNotes[key] = EngineSetting.INDEX_NOT_FOUND;
    }

    public void releaseKeys() {
        for (int key = 0; key < keyNotes.length; key++)
            release(key);
    }

    // The keys moved an octave either way, held to the octaves the instrument reaches; every held note let go
    public void stepOctave(int direction) {

        if (entity == null)
            return;

        int octave = EngineSetting.NOTE_SEMITONES_PER_OCTAVE;

        releaseKeys();
        this.baseOctavePitch = Math.clamp(
                baseOctavePitch + direction * octave,
                Math.floorDiv(instrumentHandle.getLow(), octave) * octave,
                Math.floorDiv(instrumentHandle.getHigh(), octave) * octave);
    }

    public void stepVelocity(int direction) {
        this.velocity = Math.clamp(
                velocity + direction * EngineSetting.PERFORMANCE_VELOCITY_STEP,
                EngineSetting.NOTE_VELOCITY_MIN,
                EngineSetting.NOTE_VELOCITY_MAX);
    }

    private int resolveKeyPitch(int key) {
        return NoteUtility.fitToRange(baseOctavePitch + key, instrumentHandle.getLow(), instrumentHandle.getHigh());
    }

    // The C whose octave of keys sits nearest the middle of the instrument's range
    private int resolveCentreOctave(InstrumentHandle instrument) {

        int octave = EngineSetting.NOTE_SEMITONES_PER_OCTAVE;
        int centre = (instrument.getLow() + instrument.getHigh()) / 2 - EngineSetting.PERFORMANCE_KEY_CENTRE;

        return Math.round((float) centre / octave) * octave;
    }

    // Song \\

    // One lane of a project song played on the instrument
    public void playSong(short songID, int lane) {

        if (entity != null)
            songPlayback.play(songID, instrumentHandle.getInstrumentID(), lane);
    }

    // One lane of a player's song played on the instrument; false when the song cannot be read
    public boolean playUserSong(String songName, int lane) {
        return entity != null && songPlayback.playUserSong(songName, instrumentHandle.getInstrumentID(), lane);
    }

    public void stopSong() {
        songPlayback.stop();
    }

    // Update \\

    // The song moved on and the stance kept, its clip following whether a note sounds
    public void update(float deltaTime) {

        if (entity == null)
            return;

        songPlayback.update(deltaTime);
        this.sinceSounding = isSounding() ? 0f : sinceSounding + deltaTime;
        animate();
    }

    // The stance raised with the clip that should show, once the hands are free of anything that cut it short
    private void animate() {

        EntityActionHandle actionHandle = entity.getEntityActionHandle();
        AnimationClipHandle clip = sinceSounding < EngineSetting.PERFORMANCE_PLAY_LINGER_SECONDS ? playClip : holdClip;
        boolean playing = actionHandle.getAction() == EntityAction.PLAY;

        if (playing && clip == shownClip || !playing && actionHandle.isActing())
            return;

        actionHandle.holdMove(EntityAction.PLAY, CombatDirection.NEUTRAL, EngineSetting.WEAPON_NONE, clip,
                clip.getDuration());
        this.shownClip = clip;
    }

    private boolean isSounding() {

        for (int key = 0; key < keyNotes.length; key++)
            if (keyNotes[key] != EngineSetting.INDEX_NOT_FOUND)
                return true;

        return songPlayback.getHeldCount() > 0;
    }

    // Utility \\

    private RigHandle resolveRig(EntityInstance entity) {
        return entity.getEntityData().getRaceData().getRigHandle();
    }

    // Accessible \\

    public boolean isPerforming() {
        return entity != null;
    }

    public InstrumentHandle getInstrumentHandle() {
        return instrumentHandle;
    }

    public boolean isSongPlaying() {
        return songPlayback.isPlaying();
    }

    public int getBaseOctavePitch() {
        return baseOctavePitch;
    }

    public int getVelocity() {
        return velocity;
    }
}
