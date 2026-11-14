package application.bootstrap.musicpipeline.noteset;

import engine.root.StructPackage;

public class NoteEnvelopeStruct extends StructPackage {

    /*
     * Immutable amplitude envelope of a note set: the seconds a note takes to
     * fade in, to hold at full, and to decay to its sustain level (0 to 1),
     * where it stays while held, and the seconds it takes to fade out once let
     * go. NoteEnvelopeUtility turns it into a gain for live and offline
     * playback alike.
     */

    // Envelope
    private final float attack;
    private final float hold;
    private final float decay;
    private final float sustain;
    private final float release;

    // Constructor \\

    public NoteEnvelopeStruct(float attack, float hold, float decay, float sustain, float release) {

        // Envelope
        this.attack = attack;
        this.hold = hold;
        this.decay = decay;
        this.sustain = sustain;
        this.release = release;
    }

    // Accessible \\

    public float getAttack() {
        return attack;
    }

    public float getHold() {
        return hold;
    }

    public float getDecay() {
        return decay;
    }

    public float getSustain() {
        return sustain;
    }

    public float getRelease() {
        return release;
    }
}
