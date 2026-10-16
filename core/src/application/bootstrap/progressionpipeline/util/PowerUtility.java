package application.bootstrap.progressionpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class PowerUtility extends EngineUtility {

    /*
     * The exponential half of progression. Every level is worth the same
     * growth factor, but a scaled value is only ever kept relative to its
     * owner's level, so only the gap between two levels is raised to a power
     * and no number overflows however high levels climb. toFactor() is that
     * power for a gap, held to a bounded gap past which nothing could tell the
     * difference anyway. Displayed values are absolute — relative value times
     * growth to the owner's level — and formatAbsolute() writes them through
     * logarithms, so a level-million sword prints as cleanly as a level-one
     * sword: whole numbers, then thousands, millions, billions and trillions,
     * then scientific notation.
     */

    // Constants \\

    private static final double LOG_GROWTH = Math.log10(EngineSetting.POWER_GROWTH_PER_LEVEL);
    private static final double LN_GROWTH = Math.log(EngineSetting.POWER_GROWTH_PER_LEVEL);

    // Factor \\

    // What a gap of levels multiplies a scaled value by — above one when the gap is positive
    public static float toFactor(int levelGap) {

        int gap = Math.max(-EngineSetting.POWER_LEVEL_GAP_MAX, Math.min(EngineSetting.POWER_LEVEL_GAP_MAX, levelGap));

        return (float) Math.exp(gap * LN_GROWTH);
    }

    // Format \\

    // A relative value read at a level, written absolute
    public static String formatAbsolute(float relative, int level, String wholeFormat) {

        if (relative == 0f)
            return String.format(wholeFormat, 0f);

        double log10 = Math.log10(Math.abs(relative)) + level * LOG_GROWTH;
        String sign = relative < 0f ? EngineSetting.POWER_NEGATIVE_SIGN : EngineSetting.PROGRESSION_TEXT_NONE;

        if (log10 < EngineSetting.POWER_SUFFIX_START_LOG10)
            return String.format(wholeFormat, (float) (Math.signum(relative) * Math.pow(10.0, log10)));

        int group = (int) Math.floor(log10 / EngineSetting.POWER_SUFFIX_GROUP_LOG10);

        if (group < EngineSetting.POWER_SUFFIXES.length)
            return sign + String.format(
                    EngineSetting.POWER_SUFFIX_FORMAT,
                    Math.pow(10.0, log10 - group * EngineSetting.POWER_SUFFIX_GROUP_LOG10),
                    EngineSetting.POWER_SUFFIXES[group]);

        int exponent = (int) Math.floor(log10);

        return sign + String.format(EngineSetting.POWER_SCIENTIFIC_FORMAT, Math.pow(10.0, log10 - exponent), exponent);
    }
}
