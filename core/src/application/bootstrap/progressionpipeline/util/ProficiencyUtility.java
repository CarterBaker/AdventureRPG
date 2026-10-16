package application.bootstrap.progressionpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class ProficiencyUtility extends EngineUtility {

    /*
     * The one rule set every ranged value follows — trades, skills and
     * morality alike. learn() grows a value by practice: gains shrink as the
     * value climbs, and are multiplied while it lies below its past peak, so
     * relearning is quick. shift() moves a value straight by an amount, as
     * deeds move morality. decay() settles neglect since the value was last
     * settled: nothing is lost for a grace period after last use, then the
     * value drifts toward zero more slowly the further from zero it is, slowed
     * again by retention, and never below a share of its peak. The drift has
     * an exact closed form, so a week away costs one call and lands exactly
     * where a week of frames would. step() is the whole step a value stands
     * at, which is all its bonuses and its listing read.
     */

    // Learn \\

    // Grows a value by an amount of practice at a rate, held between min and max — returns how far it rose
    public static float learn(
            ProficiencyStruct proficiency,
            float amount,
            float rate,
            float min,
            float max,
            double time) {

        proficiency.setLastUsedTime(Math.max(proficiency.getLastUsedTime(), time));

        if (amount <= 0f || rate <= 0f)
            return 0f;

        float value = proficiency.getValue();
        float difficulty = value > 0f ? 1f / (1f + value / EngineSetting.PROFICIENCY_LEARN_SOFTNESS) : 1f;
        float relearn = value < proficiency.getPeak() ? EngineSetting.PROFICIENCY_RELEARN_BOOST : 1f;
        float learned = clamp(value + amount * rate * difficulty * relearn, min, max);

        proficiency.setValue(learned);
        proficiency.setPeak(Math.max(proficiency.getPeak(), learned));

        return learned - value;
    }

    // Shift \\

    public static void shift(ProficiencyStruct proficiency, float amount, float min, float max) {

        float shifted = clamp(proficiency.getValue() + amount, min, max);

        proficiency.setValue(shifted);
        proficiency.setPeak(Math.max(proficiency.getPeak(), shifted));
    }

    // Decay \\

    // Settles neglect up to a time — true when the value moved
    public static boolean decay(ProficiencyStruct proficiency, double time, float retention) {

        double start = Math.max(
                proficiency.getSettledTime(),
                proficiency.getLastUsedTime() + EngineSetting.PROFICIENCY_DECAY_GRACE_SECONDS);

        proficiency.setSettledTime(Math.max(proficiency.getSettledTime(), time));

        float value = proficiency.getValue();

        if (time <= start || value == 0f)
            return false;

        double days = (time - start) / EngineSetting.PROFICIENCY_SECONDS_PER_DAY;
        double budget = days * EngineSetting.PROFICIENCY_DECAY_PER_DAY
                / Math.max(retention, EngineSetting.PROFICIENCY_RETENTION_MIN);
        float floor = value > 0f ? proficiency.getPeak() * EngineSetting.PROFICIENCY_MEMORY_FLOOR : 0f;
        float magnitude = Math.abs(value);

        if (value > 0f && value <= floor)
            return false;

        float decayed = toMagnitude(toEffort(magnitude) - budget);
        float settled = value > 0f ? Math.max(floor, decayed) : -decayed;

        proficiency.setValue(settled);

        return settled != value;
    }

    // The work it takes to drift from a magnitude to zero, when the drift slows by 1 / (1 + magnitude / softness)
    private static double toEffort(float magnitude) {
        return magnitude + magnitude * (double) magnitude / (2.0 * EngineSetting.PROFICIENCY_DECAY_SOFTNESS);
    }

    // The magnitude left once an effort remains — the inverse of toEffort()
    private static float toMagnitude(double effort) {

        if (effort <= 0.0)
            return 0f;

        double softness = EngineSetting.PROFICIENCY_DECAY_SOFTNESS;

        return (float) (softness * (Math.sqrt(1.0 + 2.0 * effort / softness) - 1.0));
    }

    // Step \\

    public static int step(float value) {
        return (int) Math.floor(value);
    }

    // Utility \\

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
