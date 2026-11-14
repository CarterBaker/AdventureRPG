package application.bootstrap.entitypipeline.appearance;

public enum AppearanceExpression {

    /*
     * The morph targets a character's face is animated with, apart from the
     * shapes its sliders set. Each blink closes one eye fully; its half shape
     * is the in-between the lid passes through, so a closing lid follows the
     * eyeball instead of cutting straight across it.
     */

    BLINK_LEFT,
    BLINK_RIGHT,
    BLINK_LEFT_HALF,
    BLINK_RIGHT_HALF;

    // Values
    public static final AppearanceExpression[] VALUES = values();
}
