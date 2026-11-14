package application.bootstrap.musicpipeline.sampler;

import application.bootstrap.musicpipeline.instrument.InstrumentHandle;
import application.bootstrap.musicpipeline.noteset.NoteRegionStruct;
import application.bootstrap.musicpipeline.noteset.NoteSetHandle;
import engine.root.EngineSetting;
import engine.root.StructPackage;

public class SamplerNoteStruct extends StructPackage {

    /*
     * One slot of the sampler's fixed array of live notes: the instrument
     * playing it, the region and pitch it sounds, the voice it sounds through,
     * the gain its region, velocity and instrument give it before its envelope,
     * how long it has sounded and, once let go, how long since and from what
     * level its release falls. A note is held until let go, and fades once let
     * go unless its region plays through. A handle packs the slot in its low
     * bits and the slot's generation above, so a handle kept after its note
     * ended never reaches the note that took its slot.
     */

    // Slot
    private final int slot;
    private int generation;
    private int handle;

    // State
    private boolean active;
    private boolean held;
    private boolean fading;
    private long serial;

    // Note
    private InstrumentHandle instrumentHandle;
    private NoteSetHandle noteSetHandle;
    private NoteRegionStruct region;
    private int pitch;
    private int voice;

    // Gain
    private float baseGain;
    private float gain;
    private float levelAtRelease;

    // Time — seconds since the note began, and since it was let go
    private float secondsSinceOn;
    private float secondsSinceRelease;

    // Constructor \\

    public SamplerNoteStruct(int slot) {

        // Slot
        this.slot = slot;
        this.handle = EngineSetting.INDEX_NOT_FOUND;

        // Note
        this.voice = EngineSetting.INDEX_NOT_FOUND;
    }

    // Management \\

    // The slot takes up a note under a new handle, the generation of its slot moved on
    public int begin(
            InstrumentHandle instrumentHandle,
            NoteSetHandle noteSetHandle,
            NoteRegionStruct region,
            int pitch,
            int voice,
            float baseGain,
            float gain,
            long serial) {

        this.generation = (generation + 1) & EngineSetting.SAMPLER_NOTE_GENERATION_MASK;

        if (generation == 0)
            this.generation = 1;

        this.handle = (generation << EngineSetting.SAMPLER_NOTE_SLOT_BITS) | slot;
        this.active = true;
        this.held = true;
        this.fading = false;
        this.serial = serial;
        this.instrumentHandle = instrumentHandle;
        this.noteSetHandle = noteSetHandle;
        this.region = region;
        this.pitch = pitch;
        this.voice = voice;
        this.baseGain = baseGain;
        this.gain = gain;
        this.levelAtRelease = 0f;
        this.secondsSinceOn = 0f;
        this.secondsSinceRelease = 0f;
        return handle;
    }

    // Let go: its release falls from the envelope's level now when it fades, else it plays on through
    public void letGo(boolean fade, float levelAtRelease) {
        this.held = false;
        this.fading = fade;
        this.levelAtRelease = levelAtRelease;
        this.secondsSinceRelease = 0f;
    }

    public void advance(float seconds) {

        this.secondsSinceOn += seconds;

        if (fading)
            this.secondsSinceRelease += seconds;
    }

    public void end() {
        this.active = false;
        this.held = false;
        this.fading = false;
        this.instrumentHandle = null;
        this.noteSetHandle = null;
        this.region = null;
        this.voice = EngineSetting.INDEX_NOT_FOUND;
    }

    public boolean answers(int handle) {
        return active && this.handle == handle;
    }

    // Accessible \\

    public int getSlot() {
        return slot;
    }

    public int getHandle() {
        return handle;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isHeld() {
        return held;
    }

    public boolean isFading() {
        return fading;
    }

    public long getSerial() {
        return serial;
    }

    public InstrumentHandle getInstrumentHandle() {
        return instrumentHandle;
    }

    public NoteSetHandle getNoteSetHandle() {
        return noteSetHandle;
    }

    public NoteRegionStruct getRegion() {
        return region;
    }

    public int getPitch() {
        return pitch;
    }

    public int getVoice() {
        return voice;
    }

    public float getBaseGain() {
        return baseGain;
    }

    public float getGain() {
        return gain;
    }

    public void setGain(float gain) {
        this.gain = gain;
    }

    public float getLevelAtRelease() {
        return levelAtRelease;
    }

    public float getSecondsSinceOn() {
        return secondsSinceOn;
    }

    public float getSecondsSinceRelease() {
        return secondsSinceRelease;
    }
}
