package application.bootstrap.musicpipeline.instrument;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.musicpipeline.noteset.NoteSetHandle;
import engine.root.DataPackage;

public class InstrumentData extends DataPackage {

    /*
     * Immutable instrument definition loaded from ARPG — a thing that plays
     * notes. Holds the name it is shown under, the note set it plays from and
     * the lowest and highest pitch it plays, both inside what that note set
     * covers; how many notes it holds at once and how loud it plays; the
     * General MIDI program a MIDI file names it by, -1 for none, and whether
     * it plays MIDI channel 10's drums; and the animation for every rig that
     * can play it — a body whose rig it does not list cannot. Owned by
     * InstrumentHandle for the engine lifetime.
     */

    // Identity
    private final String instrumentName;
    private final short instrumentID;
    private final String displayName;

    // Notes
    private final NoteSetHandle noteSetHandle;
    private final int low;
    private final int high;
    private final int polyphony;
    private final float volume;

    // MIDI
    private final int midiProgram;
    private final boolean percussion;

    // Rigs
    private final InstrumentRigStruct[] rigs;

    // Constructor \\

    public InstrumentData(
            String instrumentName,
            short instrumentID,
            String displayName,
            NoteSetHandle noteSetHandle,
            int low,
            int high,
            int polyphony,
            float volume,
            int midiProgram,
            boolean percussion,
            InstrumentRigStruct[] rigs) {

        // Identity
        this.instrumentName = instrumentName;
        this.instrumentID = instrumentID;
        this.displayName = displayName;

        // Notes
        this.noteSetHandle = noteSetHandle;
        this.low = low;
        this.high = high;
        this.polyphony = polyphony;
        this.volume = volume;

        // MIDI
        this.midiProgram = midiProgram;
        this.percussion = percussion;

        // Rigs
        this.rigs = rigs;
    }

    // Accessible \\

    public String getInstrumentName() {
        return instrumentName;
    }

    public short getInstrumentID() {
        return instrumentID;
    }

    public String getDisplayName() {
        return displayName;
    }

    public NoteSetHandle getNoteSetHandle() {
        return noteSetHandle;
    }

    public short getNoteSetID() {
        return noteSetHandle.getNoteSetID();
    }

    public int getLow() {
        return low;
    }

    public int getHigh() {
        return high;
    }

    // Whether the instrument plays the pitch
    public boolean plays(int pitch) {
        return pitch >= low && pitch <= high;
    }

    public int getPolyphony() {
        return polyphony;
    }

    public float getVolume() {
        return volume;
    }

    public int getMidiProgram() {
        return midiProgram;
    }

    public boolean isPercussion() {
        return percussion;
    }

    // Rigs \\

    // The instrument's animation for a rig, null when it is not made for that rig
    public InstrumentRigStruct getRig(RigHandle rigHandle) {

        for (int i = 0; i < rigs.length; i++)
            if (rigs[i].getRigHandle() == rigHandle)
                return rigs[i];

        return null;
    }

    public boolean hasRig(RigHandle rigHandle) {
        return getRig(rigHandle) != null;
    }

    // The clip a body holds the instrument with on a rig, null when it has no animation for it
    public AnimationClipHandle getHoldClip(RigHandle rigHandle) {

        InstrumentRigStruct rig = getRig(rigHandle);

        return rig != null ? rig.getHoldClip() : null;
    }

    // The clip a body plays the instrument with on a rig, null when it has no animation for it
    public AnimationClipHandle getPlayClip(RigHandle rigHandle) {

        InstrumentRigStruct rig = getRig(rigHandle);

        return rig != null ? rig.getPlayClip() : null;
    }
}
