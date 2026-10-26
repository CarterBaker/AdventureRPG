package editor.bootstrap.shortcutpipeline.util;

import editor.runtime.EditorSetting;
import engine.input.Input;
import engine.input.Keys;

public enum ShortcutModifier {

    /*
     * The keys a shortcut is held with. Either side's key holds a modifier,
     * and shortcut files name modifiers by their lower-case constant names. A
     * shortcut's modifiers are a mask of these bits, so the modifiers held
     * are read once a frame and compared whole. Each reads out by its label
     * wherever a shortcut's keys are shown.
     */

    CONTROL(Keys.CONTROL_LEFT, Keys.CONTROL_RIGHT, EditorSetting.SHORTCUT_MODIFIER_CONTROL),
    SHIFT(Keys.SHIFT_LEFT, Keys.SHIFT_RIGHT, EditorSetting.SHORTCUT_MODIFIER_SHIFT),
    ALT(Keys.ALT_LEFT, Keys.ALT_RIGHT, EditorSetting.SHORTCUT_MODIFIER_ALT);

    // Values
    public static final ShortcutModifier[] VALUES = values();
    public static final int NONE = 0;

    // Internal
    private final int leftKey;
    private final int rightKey;
    private final String label;

    // Constructor \\

    ShortcutModifier(int leftKey, int rightKey, String label) {
        this.leftKey = leftKey;
        this.rightKey = rightKey;
        this.label = label;
    }

    // Accessible \\

    public int getBit() {
        return 1 << ordinal();
    }

    public String getLabel() {
        return label;
    }

    public boolean isHeld(Input input) {
        return input.isKeyDown(leftKey) || input.isKeyDown(rightKey);
    }

    public boolean hasKey(int key) {
        return key == leftKey || key == rightKey;
    }

    // The mask of every modifier held on the input
    public static int resolveHeld(Input input) {

        int held = NONE;

        for (int i = 0; i < VALUES.length; i++)
            if (VALUES[i].isHeld(input))
                held |= VALUES[i].getBit();

        return held;
    }

    // True when the key holds a modifier, so it never names a shortcut's key itself
    public static boolean isModifierKey(int key) {

        for (int i = 0; i < VALUES.length; i++)
            if (VALUES[i].hasKey(key))
                return true;

        return false;
    }
}
