package editor.runtime.input;

import engine.input.Binding;
import engine.input.Keys;
import engine.settings.Settings;

public final class Bindings {

    /*
     * One Binding per editor action. Combos are multi-key Bindings —
     * all keys must be held for isDown() to return true.
     */

    // Single
    public static Binding TOGGLE_INSPECTOR = new Binding(Keys.I);
    public static Binding FOCUS_SELECTED = new Binding(Keys.F);
    public static Binding DELETE_SELECTED = new Binding(Keys.DELETE);

    // Combos
    public static Binding SAVE = new Binding(Keys.CONTROL_LEFT, Keys.S);
    public static Binding UNDO = new Binding(Keys.CONTROL_LEFT, Keys.Z);
    public static Binding REDO = new Binding(Keys.CONTROL_LEFT, Keys.Y);
    public static Binding DUPLICATE = new Binding(Keys.CONTROL_LEFT, Keys.D);
    public static Binding OPEN_CONSOLE = new Binding(Keys.CONTROL_LEFT, Keys.GRAVE);
    public static Binding OPEN_PREVIEW = new Binding(Keys.NUM_1);

    // Accessible \\

    public static void load(Settings settings) {
        TOGGLE_INSPECTOR.set(settings.bindToggleInspector);
        FOCUS_SELECTED.set(settings.bindFocusSelected);
        DELETE_SELECTED.set(settings.bindDeleteSelected);
        SAVE.set(settings.bindSave);
        UNDO.set(settings.bindUndo);
        REDO.set(settings.bindRedo);
        DUPLICATE.set(settings.bindDuplicate);
        OPEN_CONSOLE.set(settings.bindOpenConsole);
    }
}