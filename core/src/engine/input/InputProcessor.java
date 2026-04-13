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
        application.runtime.input.Bindings.MOVE_FORWARD.set(settings.bindMoveForward);
        application.runtime.input.Bindings.MOVE_BACK.set(settings.bindMoveBack);
        application.runtime.input.Bindings.MOVE_LEFT.set(settings.bindMoveLeft);
        application.runtime.input.Bindings.MOVE_RIGHT.set(settings.bindMoveRight);
        application.runtime.input.Bindings.JUMP.set(settings.bindJump);
        application.runtime.input.Bindings.WALK.set(settings.bindWalk);
        application.runtime.input.Bindings.SPRINT.set(settings.bindSprint);
        application.runtime.input.Bindings.INVENTORY.set(settings.bindInventory);

        // Editor
        editor.runtime.Bindings.TOGGLE_INSPECTOR.set(settings.bindToggleInspector);
        editor.runtime.Bindings.FOCUS_SELECTED.set(settings.bindFocusSelected);
        editor.runtime.Bindings.DELETE_SELECTED.set(settings.bindDeleteSelected);
        editor.runtime.Bindings.SAVE.set(settings.bindSave);
        editor.runtime.Bindings.UNDO.set(settings.bindUndo);
        editor.runtime.Bindings.REDO.set(settings.bindRedo);
        editor.runtime.Bindings.DUPLICATE.set(settings.bindDuplicate);
        editor.runtime.Bindings.OPEN_CONSOLE.set(settings.bindOpenConsole);
    }

    // Hook for later — update binding + settings field + persist
    public void rebind(engine.input.Binding binding, int... keys) {
        binding.set(keys);
    }
}