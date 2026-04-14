package engine.util.settings;

import engine.lwjgl3.input.Buttons;
import engine.lwjgl3.input.Keys;
import engine.util.input.Binding;
import engine.util.input.InputCode;

public final class KeyBindings {

    /*
     * Runtime key binding state. Holds all active Binding objects for the
     * session. Initialized with engine defaults at startup — overwritten by
     * LoadUtility.applyBindings() once Settings are loaded.
     */

    // Movement
    public static Binding MOVE_FORWARD = new Binding(InputCode.key(Keys.W));
    public static Binding MOVE_BACK = new Binding(InputCode.key(Keys.S));
    public static Binding MOVE_LEFT = new Binding(InputCode.key(Keys.A));
    public static Binding MOVE_RIGHT = new Binding(InputCode.key(Keys.D));
    public static Binding JUMP = new Binding(InputCode.key(Keys.SPACE));
    public static Binding WALK = new Binding(InputCode.key(Keys.CONTROL_LEFT));
    public static Binding SPRINT = new Binding(InputCode.key(Keys.SHIFT_LEFT));

    // Actions
    public static Binding INVENTORY = new Binding(InputCode.key(Keys.I));

    // Mouse
    public static Binding PRIMARY = new Binding(InputCode.mouse(Buttons.LEFT));
    public static Binding SECONDARY = new Binding(InputCode.mouse(Buttons.RIGHT));
    public static Binding LOOK = new Binding(InputCode.mouse(Buttons.RIGHT));

    // Editor — Single
    public static Binding TOGGLE_INSPECTOR = new Binding(InputCode.key(Keys.I));
    public static Binding FOCUS_SELECTED = new Binding(InputCode.key(Keys.F));
    public static Binding DELETE_SELECTED = new Binding(InputCode.key(Keys.DELETE));

    // Editor — Combos
    public static Binding SAVE = new Binding(InputCode.key(Keys.CONTROL_LEFT), InputCode.key(Keys.S));
    public static Binding UNDO = new Binding(InputCode.key(Keys.CONTROL_LEFT), InputCode.key(Keys.Z));
    public static Binding REDO = new Binding(InputCode.key(Keys.CONTROL_LEFT), InputCode.key(Keys.Y));
    public static Binding DUPLICATE = new Binding(InputCode.key(Keys.CONTROL_LEFT), InputCode.key(Keys.D));
    public static Binding OPEN_CONSOLE = new Binding(InputCode.key(Keys.CONTROL_LEFT), InputCode.key(Keys.GRAVE));
    public static Binding OPEN_PREVIEW = new Binding(InputCode.key(Keys.NUM_1));
}