package application.runtime.menueventsmanager.menus;

import application.runtime.RuntimeSetting;

public enum PhotoModeOption {

    /*
     * The rows photo mode's panel lists, in order, each stepped by its
     * arrows. Filter and Pose step through what the game holds — every photo
     * filter, and every pose made for the player's own rig after the stance
     * they were caught in — Hide Player turns on and off, and every other row
     * is a number held within its range by its step, starting where it
     * leaves the picture as it was. Field of View starts at the player's own.
     */

    FILTER(RuntimeSetting.PHOTO_OPTION_FILTER, 0f, 0f, 0f, 0f),
    POSE(RuntimeSetting.PHOTO_OPTION_POSE, 0f, 0f, 0f, 0f),
    HIDE_PLAYER(RuntimeSetting.PHOTO_OPTION_HIDE_PLAYER, 0f, 0f, 0f, 0f),
    FIELD_OF_VIEW(RuntimeSetting.PHOTO_OPTION_FIELD_OF_VIEW, 0f, 20f, 110f, 5f),
    EXPOSURE(RuntimeSetting.PHOTO_OPTION_EXPOSURE, 1f, 0.25f, 2f, 0.05f),
    CONTRAST(RuntimeSetting.PHOTO_OPTION_CONTRAST, 1f, 0.5f, 2f, 0.05f),
    SATURATION(RuntimeSetting.PHOTO_OPTION_SATURATION, 1f, 0f, 2f, 0.05f),
    VIGNETTE(RuntimeSetting.PHOTO_OPTION_VIGNETTE, 0f, 0f, 1f, 0.05f),
    GRAIN(RuntimeSetting.PHOTO_OPTION_GRAIN, 0f, 0f, 1f, 0.05f);

    public static final PhotoModeOption[] VALUES = values();

    // Internal
    private final String label;
    private final float neutral;
    private final float min;
    private final float max;
    private final float step;

    // Constructor \\

    PhotoModeOption(String label, float neutral, float min, float max, float step) {
        this.label = label;
        this.neutral = neutral;
        this.min = min;
        this.max = max;
        this.step = step;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public float getNeutral() {
        return neutral;
    }

    // A number a number of steps on from another, held within the range
    public float step(float value, int steps) {
        return Math.clamp(value + steps * step, min, max);
    }

    public boolean isNumber() {
        return step > 0f;
    }
}
