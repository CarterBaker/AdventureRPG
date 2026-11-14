package application.bootstrap.musicpipeline.noteset;

import engine.root.HandlePackage;

public class NoteSetHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded note set. Registered and owned by
     * NoteSetManager and shared by every instrument that plays from it.
     * Delegates all accessors through NoteSetData.
     */

    // Internal
    private NoteSetData noteSetData;

    // Constructor \\

    public void constructor(NoteSetData noteSetData) {

        // Internal
        this.noteSetData = noteSetData;
    }

    // Accessible \\

    public NoteSetData getNoteSetData() {
        return noteSetData;
    }

    public String getNoteSetName() {
        return noteSetData.getNoteSetName();
    }

    public short getNoteSetID() {
        return noteSetData.getNoteSetID();
    }

    public NoteRegionStruct findRegion(int pitch, int velocity, int roundRobinTurn) {
        return noteSetData.findRegion(pitch, velocity, roundRobinTurn);
    }

    public boolean covers(int fromPitch, int toPitch) {
        return noteSetData.covers(fromPitch, toPitch);
    }

    public int getRegionCount() {
        return noteSetData.getRegionCount();
    }

    public NoteRegionStruct getRegion(int index) {
        return noteSetData.getRegion(index);
    }

    public NoteEnvelopeStruct getEnvelope() {
        return noteSetData.getEnvelope();
    }

    public float getVelocityTracking() {
        return noteSetData.getVelocityTracking();
    }

    public int getLow() {
        return noteSetData.getLow();
    }

    public int getHigh() {
        return noteSetData.getHigh();
    }
}
