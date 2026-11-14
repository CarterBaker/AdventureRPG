package application.bootstrap.menupipeline.util;

import engine.root.EngineSetting;

public enum MenuEase {

    /*
     * Easing curve applied to the segment leading into a menu animation
     * keyframe. Maps linear segment progress in [0, 1] to eased progress.
     * BACK_OUT overshoots past the target before settling, for cartoony pops.
     */

    LINEAR,
    IN,
    OUT,
    IN_OUT,
    BACK_OUT;

    public static MenuEase fromString(String s) {
        return switch (s.toLowerCase()) {
            case "linear" -> LINEAR;
            case "in" -> IN;
            case "out" -> OUT;
            case "in_out" -> IN_OUT;
            case "back_out" -> BACK_OUT;
            default -> null;
        };
    }

    public float apply(float t) {
        return switch (this) {
            case LINEAR -> t;
            case IN -> t * t;
            case OUT -> 1f - (1f - t) * (1f - t);
            case IN_OUT -> t * t * (3f - 2f * t);
            case BACK_OUT -> {
                float c = EngineSetting.MENU_EASE_BACK_OVERSHOOT;
                float u = t - 1f;
                yield 1f + (c + 1f) * u * u * u + c * u * u;
            }
        };
    }
}
