package engine.util.input;

import engine.settings.Settings;

public final class Bindings {

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

    // Load \\

    public static void load(Settings settings) {
        MOVE_FORWARD.set(toKeys(settings.bindMoveForward));
        MOVE_BACK.set(toKeys(settings.bindMoveBack));
        MOVE_LEFT.set(toKeys(settings.bindMoveLeft));
        MOVE_RIGHT.set(toKeys(settings.bindMoveRight));
        JUMP.set(toKeys(settings.bindJump));
        WALK.set(toKeys(settings.bindWalk));
        SPRINT.set(toKeys(settings.bindSprint));
        INVENTORY.set(toKeys(settings.bindInventory));
        TOGGLE_INSPECTOR.set(toKeys(settings.bindToggleInspector));
        FOCUS_SELECTED.set(toKeys(settings.bindFocusSelected));
        DELETE_SELECTED.set(toKeys(settings.bindDeleteSelected));
        SAVE.set(toKeys(settings.bindSave));
        UNDO.set(toKeys(settings.bindUndo));
        REDO.set(toKeys(settings.bindRedo));
        DUPLICATE.set(toKeys(settings.bindDuplicate));
        OPEN_CONSOLE.set(toKeys(settings.bindOpenConsole));
    }

    private static InputCode[] toKeys(int[] codes) {
        InputCode[] result = new InputCode[codes.length];
        for (int i = 0; i < codes.length; i++)
            result[i] = InputCode.key(codes[i]);
        return result;
    }
}