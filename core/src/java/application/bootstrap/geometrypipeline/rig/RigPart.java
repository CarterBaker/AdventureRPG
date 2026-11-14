package application.bootstrap.geometrypipeline.rig;

public enum RigPart {

    /*
     * The parts of a body a rig may name by their bones, so any creature's
     * part editors find them however its bones are called: the eyes, the
     * mouth, the hands and the feet. A part holds the bones it names and
     * every bone below them; a part whose bones each have a twin across the
     * body is worked one side at a time.
     */

    EYES,
    MOUTH,
    HANDS,
    FEET;

    // Values
    public static final RigPart[] VALUES = values();
}
