package editor.bootstrap.stamppipeline.util;

public enum StampCategory {

    /*
     * What kind of body part a stamp is, the way a creature kit sorts its
     * parts: the Stamps tab groups and labels stamps by it, and a part cut
     * from a character takes the kind its layer names where one matches.
     */

    TORSO,
    HEAD,
    NECK,
    ARM,
    HAND,
    LEG,
    FOOT,
    TAIL,
    WING,
    EAR,
    EYE,
    MOUTH,
    NOSE,
    HORN,
    FUR,
    OTHER;

    // Values
    public static final StampCategory[] VALUES = values();
}
