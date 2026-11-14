package editor.bootstrap.transitionpipeline.util;

import editor.runtime.EditorSetting;

public enum TransitionDirection {

    /*
     * The way a wipe sweeps and a push slides, as the step across the frame
     * the incoming picture travels, the frame running from zero to one each
     * way with its height upward.
     */

    LEFT(EditorSetting.TRANSITION_DIRECTION_LEFT, -1f, 0f),
    RIGHT(EditorSetting.TRANSITION_DIRECTION_RIGHT, 1f, 0f),
    UP(EditorSetting.TRANSITION_DIRECTION_UP, 0f, 1f),
    DOWN(EditorSetting.TRANSITION_DIRECTION_DOWN, 0f, -1f);

    // Values
    public static final TransitionDirection[] VALUES = values();

    // Internal
    private final String label;
    private final float x;
    private final float y;

    // Constructor \\

    TransitionDirection(String label, float x, float y) {
        this.label = label;
        this.x = x;
        this.y = y;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}
