package application.bootstrap.musicpipeline.instrument;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.musicpipeline.noteset.NoteSetHandle;
import engine.root.HandlePackage;

public class InstrumentHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded instrument. Registered and owned by
     * InstrumentManager and shared by every item, song and player that names
     * it. Delegates all accessors through InstrumentData.
     */

    // Internal
    private InstrumentData instrumentData;

    // Constructor \\

    public void constructor(InstrumentData instrumentData) {

        // Internal
        this.instrumentData = instrumentData;
    }

    // Accessible \\

    public InstrumentData getInstrumentData() {
        return instrumentData;
    }

    public String getInstrumentName() {
        return instrumentData.getInstrumentName();
    }

    public short getInstrumentID() {
        return instrumentData.getInstrumentID();
    }

    public String getDisplayName() {
        return instrumentData.getDisplayName();
    }

    public NoteSetHandle getNoteSetHandle() {
        return instrumentData.getNoteSetHandle();
    }

    public short getNoteSetID() {
        return instrumentData.getNoteSetID();
    }

    public int getLow() {
        return instrumentData.getLow();
    }

    public int getHigh() {
        return instrumentData.getHigh();
    }

    public boolean plays(int pitch) {
        return instrumentData.plays(pitch);
    }

    public int getPolyphony() {
        return instrumentData.getPolyphony();
    }

    public float getVolume() {
        return instrumentData.getVolume();
    }

    public int getMidiProgram() {
        return instrumentData.getMidiProgram();
    }

    public boolean isPercussion() {
        return instrumentData.isPercussion();
    }

    public boolean hasRig(RigHandle rigHandle) {
        return instrumentData.hasRig(rigHandle);
    }

    public AnimationClipHandle getHoldClip(RigHandle rigHandle) {
        return instrumentData.getHoldClip(rigHandle);
    }

    public AnimationClipHandle getPlayClip(RigHandle rigHandle) {
        return instrumentData.getPlayClip(rigHandle);
    }
}
