package application.bootstrap.musicpipeline.noteset;

import engine.root.DataPackage;

public class NoteSetData extends DataPackage {

    /*
     * Immutable note set loaded from ARPG — the samples an instrument plays
     * from, as an SFZ instrument lays them out: its regions in order of their
     * lowest pitch, one amplitude envelope for every note, and how strongly
     * velocity sets loudness, 0 for not at all to 1 for the square of the
     * velocity's share of 127. A per-pitch index built once lists, for every
     * pitch, the regions covering it in velocity order, so finding the region
     * a note plays walks only those and allocates nothing; live and offline
     * playback both find it here. Owned by NoteSetHandle for the engine
     * lifetime.
     */

    // Identity
    private final String noteSetName;
    private final short noteSetID;

    // Regions — in order of their lowest pitch
    private final NoteRegionStruct[] regions;

    // Index — pitch2RegionStart[pitch] to pitch2RegionStart[pitch + 1] in regionIndices covers the pitch
    private final int[] pitch2RegionStart;
    private final int[] regionIndices;

    // Envelope
    private final NoteEnvelopeStruct envelope;
    private final float velocityTracking;

    // Coverage — the lowest and highest pitch any region covers
    private final int low;
    private final int high;

    // Constructor \\

    public NoteSetData(
            String noteSetName,
            short noteSetID,
            NoteRegionStruct[] regions,
            int[] pitch2RegionStart,
            int[] regionIndices,
            NoteEnvelopeStruct envelope,
            float velocityTracking,
            int low,
            int high) {

        // Identity
        this.noteSetName = noteSetName;
        this.noteSetID = noteSetID;

        // Regions
        this.regions = regions;

        // Index
        this.pitch2RegionStart = pitch2RegionStart;
        this.regionIndices = regionIndices;

        // Envelope
        this.envelope = envelope;
        this.velocityTracking = velocityTracking;

        // Coverage
        this.low = low;
        this.high = high;
    }

    // Lookup \\

    // The one lookup every playback shares: the region a pitch plays at a velocity on a turn of the round robin,
    // null when none covers it
    public NoteRegionStruct findRegion(int pitch, int velocity, int roundRobinTurn) {

        if (pitch < 0 || pitch >= pitch2RegionStart.length - 1)
            return null;

        for (int i = pitch2RegionStart[pitch]; i < pitch2RegionStart[pitch + 1]; i++) {

            NoteRegionStruct region = regions[regionIndices[i]];

            if (region.matches(velocity, roundRobinTurn))
                return region;
        }

        return null;
    }

    // Whether some region sounds for every pitch from low to high
    public boolean covers(int fromPitch, int toPitch) {

        if (fromPitch < 0 || toPitch >= pitch2RegionStart.length - 1 || fromPitch > toPitch)
            return false;

        for (int pitch = fromPitch; pitch <= toPitch; pitch++)
            if (pitch2RegionStart[pitch] == pitch2RegionStart[pitch + 1])
                return false;

        return true;
    }

    // Accessible \\

    public String getNoteSetName() {
        return noteSetName;
    }

    public short getNoteSetID() {
        return noteSetID;
    }

    public int getRegionCount() {
        return regions.length;
    }

    public NoteRegionStruct getRegion(int index) {
        return regions[index];
    }

    public NoteEnvelopeStruct getEnvelope() {
        return envelope;
    }

    public float getVelocityTracking() {
        return velocityTracking;
    }

    public int getLow() {
        return low;
    }

    public int getHigh() {
        return high;
    }
}
