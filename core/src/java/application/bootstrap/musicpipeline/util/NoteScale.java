package application.bootstrap.musicpipeline.util;

import engine.root.EngineSetting;

public enum NoteScale {

    /*
     * The scales a key can be in, each the steps above its root that sound
     * right together: the major and minor keys every song tool offers, their
     * pentatonic and blues cousins, the church modes and, for music that
     * keeps to no key, all twelve. Scale Lock keeps drawn and typed notes on
     * them. A song names its scale by the lower-case constant name.
     */

    MAJOR(EngineSetting.NOTE_SCALE_MAJOR),
    MINOR(EngineSetting.NOTE_SCALE_MINOR),
    HARMONIC_MINOR(EngineSetting.NOTE_SCALE_HARMONIC_MINOR),
    MELODIC_MINOR(EngineSetting.NOTE_SCALE_MELODIC_MINOR),
    MAJOR_PENTATONIC(EngineSetting.NOTE_SCALE_MAJOR_PENTATONIC),
    MINOR_PENTATONIC(EngineSetting.NOTE_SCALE_MINOR_PENTATONIC),
    BLUES(EngineSetting.NOTE_SCALE_BLUES),
    DORIAN(EngineSetting.NOTE_SCALE_DORIAN),
    PHRYGIAN(EngineSetting.NOTE_SCALE_PHRYGIAN),
    LYDIAN(EngineSetting.NOTE_SCALE_LYDIAN),
    MIXOLYDIAN(EngineSetting.NOTE_SCALE_MIXOLYDIAN),
    LOCRIAN(EngineSetting.NOTE_SCALE_LOCRIAN),
    CHROMATIC(EngineSetting.NOTE_SCALE_CHROMATIC);

    // Values
    public static final NoteScale[] VALUES = values();

    // Internal — bit n set for every step n above the root the scale holds
    private final int stepMask;

    // Constructor \\

    NoteScale(int[] steps) {

        int mask = 0;

        for (int i = 0; i < steps.length; i++)
            mask |= 1 << steps[i];

        this.stepMask = mask;
    }

    // Accessible \\

    // Whether the step above the root, 0 to 11, lies on the scale
    public boolean holdsStep(int step) {
        return (stepMask & (1 << step)) != 0;
    }
}
