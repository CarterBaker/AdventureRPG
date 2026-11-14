package editor.bootstrap.recorderpipeline.pianorollmanager;

import application.bootstrap.musicpipeline.instrumentmanager.InstrumentManager;
import application.bootstrap.musicpipeline.notesetmanager.NoteSetManager;
import application.bootstrap.musicpipeline.samplermanager.SamplerManager;
import application.bootstrap.musicpipeline.util.NoteUtility;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderState;
import editor.bootstrap.shortcutpipeline.shortcutmanager.ShortcutManager;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class PianoRollTypingBranch extends BranchPackage {

    /*
     * Musical Typing, the computer keyboard played as a piano the way DAWs
     * lay it out: thirteen held shortcuts from C to the next C, an octave
     * that Z and X move and a velocity that C and V move. While it is on the
     * Piano Roll's window claims every one of those keys, so no other
     * shortcut on them runs there. A note key pressed sounds its pitch
     * through the sampler on the clip's instrument, kept to the key with
     * Scale Lock on, and is let go when its held shortcut lets go; while the
     * transport records, the note is written into the clip from the
     * playhead's tick where it was pressed, its start and length on the Snap
     * grid. Everything is read from fixed arrays, so playing allocates
     * nothing; a note written is an edit like any other.
     */

    // Internal
    private PianoRollManager pianoRollManager;
    private RecorderManager recorderManager;
    private ShortcutManager shortcutManager;
    private SamplerManager samplerManager;
    private InstrumentManager instrumentManager;
    private NoteSetManager noteSetManager;

    // Shortcuts — resolved the first time Musical Typing is turned on
    private int[] noteShortcutIDs;
    private int[] claimedShortcutIDs;

    // Typing
    private boolean on;
    private int baseOctavePitch;
    private int velocity;

    // Held — per note key, the sampler's note it sounds, its pitch, and the clip's tick it was pressed at, -1 when
    // it is not being recorded
    private boolean[] held;
    private int[] heldHandles;
    private int[] heldPitches;
    private int[] heldTicks;

    // Base \\

    @Override
    protected void create() {

        int count = EditorSetting.PIANO_ROLL_SHORTCUT_NOTES.length;

        // Typing
        this.baseOctavePitch = EditorSetting.PIANO_ROLL_TYPING_BASE_DEFAULT;
        this.velocity = EditorSetting.PIANO_ROLL_TYPING_VELOCITY_DEFAULT;

        // Held
        this.held = new boolean[count];
        this.heldHandles = new int[count];
        this.heldPitches = new int[count];
        this.heldTicks = new int[count];
    }

    @Override
    protected void get() {
        this.pianoRollManager = get(PianoRollManager.class);
        this.recorderManager = get(RecorderManager.class);
        this.shortcutManager = get(ShortcutManager.class);
        this.samplerManager = get(SamplerManager.class);
        this.instrumentManager = get(InstrumentManager.class);
        this.noteSetManager = get(NoteSetManager.class);
    }

    // Toggle \\

    // Turned on, the window claims the note, octave and velocity keys and the instrument's samples are asked for;
    // turned off, the keys go back and every held note is let go
    void toggle(WindowInstance window) {

        resolveShortcuts();
        this.on = !on;

        if (on) {
            shortcutManager.claimKeys(window, claimedShortcutIDs);
            noteSetManager.prepare(instrumentManager.getInstrumentHandleFromInstrumentID(
                    pianoRollManager.resolveInstrumentID()).getNoteSetID());
        } else {
            shortcutManager.releaseKeys(window);
            releaseAll();
        }

        pianoRollManager.notifyChanged();
    }

    private void resolveShortcuts() {

        if (noteShortcutIDs != null)
            return;

        int notes = EditorSetting.PIANO_ROLL_SHORTCUT_NOTES.length;
        int typing = EditorSetting.PIANO_ROLL_SHORTCUT_TYPING.length;

        this.noteShortcutIDs = new int[notes];
        this.claimedShortcutIDs = new int[notes + typing];

        for (int i = 0; i < notes; i++) {
            noteShortcutIDs[i] = shortcutManager.getShortcutIDFromShortcutName(
                    EditorSetting.PIANO_ROLL_SHORTCUT_NOTES[i]);
            claimedShortcutIDs[i] = noteShortcutIDs[i];
        }

        for (int i = 0; i < typing; i++)
            claimedShortcutIDs[notes + i] = shortcutManager.getShortcutIDFromShortcutName(
                    EditorSetting.PIANO_ROLL_SHORTCUT_TYPING[i]);
    }

    // Update \\

    // Each note key pressed sounds its note and each let go releases it, while the window is the active one; once it
    // is not, every held note is let go
    void update(WindowInstance window, boolean active) {

        if (!on)
            return;

        if (!active) {
            releaseAll();
            return;
        }

        for (int i = 0; i < noteShortcutIDs.length; i++) {

            if (!held[i] && shortcutManager.isPressed(noteShortcutIDs[i], window))
                press(i);
            else if (held[i] && !shortcutManager.isHeld(noteShortcutIDs[i], window))
                release(i);
        }
    }

    private void press(int key) {

        int pitch = pianoRollManager.lockPitch(baseOctavePitch + key, 0);

        held[key] = true;
        heldPitches[key] = pitch;
        heldHandles[key] = samplerManager.noteOn(pianoRollManager.resolveInstrumentID(), pitch, velocity);
        heldTicks[key] = isRecording()
                ? (int) Math.round(pianoRollManager.getPlayheadTick())
                : EngineSetting.INDEX_NOT_FOUND;
    }

    // The note let go, and written into the clip when it was pressed while recording
    private void release(int key) {

        held[key] = false;

        if (heldHandles[key] != EngineSetting.INDEX_NOT_FOUND)
            samplerManager.noteOff(heldHandles[key]);

        if (heldTicks[key] == EngineSetting.INDEX_NOT_FOUND || !isRecording())
            return;

        int snap = pianoRollManager.getSnapTicks();
        int start = pianoRollManager.snapTick(heldTicks[key]);
        int played = (int) Math.round(pianoRollManager.getPlayheadTick()) - heldTicks[key];

        pianoRollManager.addTypedNote(start, Math.max(snap, Math.round(played / (float) snap) * snap),
                heldPitches[key], velocity);
    }

    private void releaseAll() {
        for (int i = 0; i < held.length; i++)
            if (held[i])
                release(i);
    }

    private boolean isRecording() {
        return recorderManager.getState() == RecorderState.RECORDING && pianoRollManager.getClip() != null;
    }

    // Octave and Velocity \\

    void stepOctave(int direction) {

        if (!on)
            return;

        releaseAll();
        this.baseOctavePitch = Math.clamp(baseOctavePitch + direction * EngineSetting.NOTE_SEMITONES_PER_OCTAVE,
                EditorSetting.PIANO_ROLL_TYPING_BASE_MIN, EditorSetting.PIANO_ROLL_TYPING_BASE_MAX);
        pianoRollManager.notifyChanged();
    }

    void stepVelocity(int direction) {

        if (!on)
            return;

        this.velocity = Math.clamp(velocity + direction * EditorSetting.PIANO_ROLL_TYPING_VELOCITY_STEP,
                EngineSetting.NOTE_VELOCITY_MIN, EngineSetting.NOTE_VELOCITY_MAX);
        pianoRollManager.notifyChanged();
    }

    // Accessible \\

    boolean isOn() {
        return on;
    }

    // What Musical Typing plays, for the Piano Roll's header
    String describe() {
        return on
                ? String.format(EditorSetting.PIANO_ROLL_TYPING_FORMAT, NoteUtility.toNoteName(baseOctavePitch),
                        NoteUtility.toNoteName(baseOctavePitch + EngineSetting.NOTE_SEMITONES_PER_OCTAVE), velocity)
                : EditorSetting.PIANO_ROLL_TYPING_OFF;
    }
}
