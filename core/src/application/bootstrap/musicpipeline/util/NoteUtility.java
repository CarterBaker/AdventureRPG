package application.bootstrap.musicpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class NoteUtility extends EngineUtility {

    /*
     * Stateless note arithmetic shared by every part of the music pipeline:
     * a pitch's name and back (C4 is 60, sharps written with '#'), how fast a
     * sample plays to sound one pitch from the pitch it was recorded at, the
     * gain a decibel level and a velocity stand for, and whether a pitch lies
     * on a key's scale, with the nearest one on it. Never held — all methods
     * static.
     */

    // Names \\

    // The pitch's name, its letter, any sharp and its octave: 60 is C4
    public static String toNoteName(int pitch) {
        return EngineSetting.NOTE_NAMES[Math.floorMod(pitch, EngineSetting.NOTE_SEMITONES_PER_OCTAVE)]
                + (Math.floorDiv(pitch, EngineSetting.NOTE_SEMITONES_PER_OCTAVE) - EngineSetting.NOTE_OCTAVE_OFFSET);
    }

    // The pitch a name such as C4, F#3 or Bb2 stands for, -1 when it names none
    public static int parseNoteName(String noteName) {

        if (noteName == null || noteName.length() < EngineSetting.NOTE_NAME_MIN_LENGTH)
            return EngineSetting.INDEX_NOT_FOUND;

        int step = EngineSetting.NOTE_LETTERS.indexOf(Character.toUpperCase(noteName.charAt(0)));

        if (step == EngineSetting.INDEX_NOT_FOUND)
            return EngineSetting.INDEX_NOT_FOUND;

        int semitone = EngineSetting.NOTE_LETTER_STEPS[step];
        int at = 1;

        while (at < noteName.length() && EngineSetting.NOTE_ACCIDENTALS.indexOf(noteName.charAt(at)) >= 0) {
            semitone += noteName.charAt(at) == EngineSetting.NOTE_SHARP ? 1 : -1;
            at++;
        }

        try {
            int octave = Integer.parseInt(noteName.substring(at));
            int pitch = (octave + EngineSetting.NOTE_OCTAVE_OFFSET) * EngineSetting.NOTE_SEMITONES_PER_OCTAVE
                    + semitone;

            return pitch >= EngineSetting.NOTE_PITCH_MIN && pitch <= EngineSetting.NOTE_PITCH_MAX
                    ? pitch
                    : EngineSetting.INDEX_NOT_FOUND;
        } catch (NumberFormatException e) {
            return EngineSetting.INDEX_NOT_FOUND;
        }
    }

    // Pitch \\

    // How fast a sample recorded at the root plays to sound the pitch, tuned by the cents given
    public static float toPitchRatio(int pitch, int root, float tuneCents) {
        return (float) Math.pow(2.0, (double) (pitch - root) / EngineSetting.NOTE_SEMITONES_PER_OCTAVE
                + tuneCents / EngineSetting.NOTE_CENTS_PER_OCTAVE);
    }

    // Gain \\

    // The gain a level in decibels scales by
    public static float toGain(float decibels) {
        return (float) Math.pow(10.0, decibels / EngineSetting.NOTE_DECIBELS_PER_GAIN_DECADE);
    }

    // How loud a velocity plays: the square of its share of the loudest at full tracking, always full at none
    public static float toVelocityGain(int velocity, float tracking) {

        float share = (float) velocity / EngineSetting.NOTE_VELOCITY_MAX;

        return 1f - tracking + tracking * share * share;
    }

    // Scales \\

    // Whether the pitch lies on the scale of the key whose root pitch class is given
    public static boolean isInScale(int pitch, int keyRoot, NoteScale scale) {
        return scale.holdsStep(Math.floorMod(pitch - keyRoot, EngineSetting.NOTE_SEMITONES_PER_OCTAVE));
    }

    // The pitch on the key's scale nearest the one given, the lower of two equally near
    public static int snapToScale(int pitch, int keyRoot, NoteScale scale) {

        for (int distance = 0; distance < EngineSetting.NOTE_SEMITONES_PER_OCTAVE; distance++) {

            if (pitch - distance >= EngineSetting.NOTE_PITCH_MIN && isInScale(pitch - distance, keyRoot, scale))
                return pitch - distance;

            if (pitch + distance <= EngineSetting.NOTE_PITCH_MAX && isInScale(pitch + distance, keyRoot, scale))
                return pitch + distance;
        }

        return pitch;
    }
}
