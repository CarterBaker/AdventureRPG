package editor.bootstrap.shortcutpipeline.util;

import engine.input.Input;
import engine.input.Keys;

public enum ShortcutModifier {

    /*
     * The keys a shortcut is held with. Either side's key holds a modifier,
     * and shortcut files name modifiers by their lower-case constant names. A
     * shortcut's modifiers are a mask of these bits, so the modifiers held
     * are read once a frame and compared whole.
     */

    CONTROL(Keys.CONTROL_LEFT, Keys.CONTROL_RIGHT),
    SHIFT(Keys.SHIFT_LEFT, Keys.SHIFT_RIGHT),
    ALT(Keys.ALT_LEFT, Keys.ALT_RIGHT);

    // Values
    public static final ShortcutModifier[] VALUES = values();
    public static final int NONE = 0;

    // Internal
    private final int leftKey;
    private final int rightKey;

    // Constructor \\

    ShortcutModifier(int leftKey, int rightKey) {
        this.leftKey = leftKey;
        this.rightKey = rightKey;
    }

    // Accessible \\

    public int getBit() {
        return 1 << ordinal();
    }

    public boolean isHeld(Input input) {
        return input.isKeyDown(leftKey) || input.isKeyDown(rightKey);
    }

    // The mask of every modifier held on the input
    public static int resolveHeld(Input input) {

        int held = NONE;

        for (int i = 0; i < VALUES.length; i++)
            if (VALUES[i].isHeld(input))
                held |= VALUES[i].getBit();

        return held;
    }
}
