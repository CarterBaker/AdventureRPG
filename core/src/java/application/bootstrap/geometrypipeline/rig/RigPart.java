package application.bootstrap.geometrypipeline.rig;

public enum RigPart {

    /*
     * The parts of a body a rig may name by their bones, so the Character
     * Editor's part layers find them on any creature however its bones are
     * called: the head, the ears, the eyes, the mouth, the hands and the
     * feet. A part holds the bones it names and every bone below them; a
     * part whose bones each have a twin across the body is worked one side at
     * a time.
     */

    HEAD,
    EARS,
    EYES,
    MOUTH,
    HANDS,
    FEET;

    // Values
    public static final RigPart[] VALUES = values();
}
