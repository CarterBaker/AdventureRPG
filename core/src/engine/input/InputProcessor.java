package engine.input;

import engine.settings.Settings;

public final class InputProcessor {

    /*
     * Loads bindings from Settings at startup and applies them to
     * GameBindings and EditorBindings. Hook for rebinding lives here —
     * when rebinding is supported, update the binding, update settings,
     * and persist via Loader.
     */

    public void loadFromSettings(Settings settings) {
        // Game
        application.util.input.Bindings.MOVE_FORWARD.set(settings.bindMoveForward);
        application.util.input.Bindings.MOVE_BACK.set(settings.bindMoveBack);
        application.util.input.Bindings.MOVE_LEFT.set(settings.bindMoveLeft);
        application.util.input.Bindings.MOVE_RIGHT.set(settings.bindMoveRight);
        application.util.input.Bindings.JUMP.set(settings.bindJump);
        application.util.input.Bindings.WALK.set(settings.bindWalk);
        application.util.input.Bindings.SPRINT.set(settings.bindSprint);
        application.util.input.Bindings.INVENTORY.set(settings.bindInventory);

        // Editor
        editor.util.input.Bindings.TOGGLE_INSPECTOR.set(settings.bindToggleInspector);
        editor.util.input.Bindings.FOCUS_SELECTED.set(settings.bindFocusSelected);
        editor.util.input.Bindings.DELETE_SELECTED.set(settings.bindDeleteSelected);
        editor.util.input.Bindings.SAVE.set(settings.bindSave);
        editor.util.input.Bindings.UNDO.set(settings.bindUndo);
        editor.util.input.Bindings.REDO.set(settings.bindRedo);
        editor.util.input.Bindings.DUPLICATE.set(settings.bindDuplicate);
        editor.util.input.Bindings.OPEN_CONSOLE.set(settings.bindOpenConsole);
    }

    // Hook for later — update binding + settings field + persist
    public void rebind(engine.input.Binding binding, int... keys) {
        binding.set(keys);
    }
}