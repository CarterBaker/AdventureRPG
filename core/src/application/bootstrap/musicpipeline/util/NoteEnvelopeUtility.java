package application.bootstrap.musicpipeline.util;

import application.bootstrap.musicpipeline.noteset.NoteEnvelopeStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class NoteEnvelopeUtility extends EngineUtility {

    /*
     * The one envelope curve every note follows, live and offline alike. A
     * held note rises in a straight line over the attack, holds at full,
     * then decays along a curve to its sustain level, where it stays; once let
     * go it falls along the same curve from wherever it stood to silence over
     * the release. The curve eases as decaying sound does, falling fast at
     * first and gently near its end. Never held — all methods static.
     */

    // Held \\

    // The note's gain the given seconds after it began, while it is held
    public static float gainAt(NoteEnvelopeStruct envelope, float secondsSinceOn) {

        float time = secondsSinceOn;

        if (time < envelope.getAttack())
            return time / envelope.getAttack();

        time -= envelope.getAttack();

        if (time < envelope.getHold())
            return 1f;

        time -= envelope.getHold();

        if (time >= envelope.getDecay())
            return envelope.getSustain();

        return envelope.getSustain() + (1f - envelope.getSustain()) * fall(time / envelope.getDecay());
    }

    // Released \\

    // The note's gain the given seconds after it was let go, from the level it stood at then
    public static float releaseGainAt(NoteEnvelopeStruct envelope, float levelAtRelease, float secondsSinceRelease) {

        if (secondsSinceRelease >= envelope.getRelease())
            return 0f;

        return levelAtRelease * fall(secondsSinceRelease / envelope.getRelease());
    }

    // Whether the release has faded the note out
    public static boolean isReleased(NoteEnvelopeStruct envelope, float secondsSinceRelease) {
        return secondsSinceRelease >= envelope.getRelease();
    }

    // Curve \\

    // What remains of a fall the given share of the way through it, 1 at its start and 0 at its end
    private static float fall(float progress) {

        float remaining = 1f - progress;

        return (float) Math.pow(remaining, EngineSetting.NOTE_ENVELOPE_CURVE);
    }
}
