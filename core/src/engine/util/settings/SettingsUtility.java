package engine.util.settings;

import com.google.gson.Gson;
import engine.root.EngineSetting;
import engine.util.input.Binding;
import engine.util.input.InputCode;
import java.io.*;

public class SettingsUtility {

    /*
     * Handles all Settings I/O and bridges Settings to KeyBindings. Single
     * point of contact for loading, saving, applying, and flushing bindings.
     * Never held — all methods static.
     */

    // Settings \\

    public static Settings load(File file, Gson gson) {

        if (!file.exists()) {
            Settings defaults = new Settings();
            save(file, defaults, gson);
            return defaults;
        }

        try (Reader reader = new FileReader(file)) {
            Settings loaded = gson.fromJson(reader, Settings.class);

            if (loaded == null)
                return new Settings();

            sanitize(loaded);
            return loaded;
        } catch (IOException e) {
            e.printStackTrace();
            return new Settings();
        }
    }

    public static void save(File file, Settings settings, Gson gson) {

        try (Writer writer = new FileWriter(file)) {
            gson.toJson(settings, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sanitize(Settings settings) {

        if (settings.windowWidth < EngineSetting.MIN_WINDOW_DIMENSION)
            settings.windowWidth = EngineSetting.MIN_WINDOW_DIMENSION;

        if (settings.windowHeight < EngineSetting.MIN_WINDOW_DIMENSION)
            settings.windowHeight = EngineSetting.MIN_WINDOW_DIMENSION;
    }

    // KeyBindings \\

    public static void applyBindings(Settings settings) {

        KeyBindings.MOVE_FORWARD.set(toInputCodes(settings.bindMoveForward));
        KeyBindings.MOVE_BACK.set(toInputCodes(settings.bindMoveBack));
        KeyBindings.MOVE_LEFT.set(toInputCodes(settings.bindMoveLeft));
        KeyBindings.MOVE_RIGHT.set(toInputCodes(settings.bindMoveRight));
        KeyBindings.JUMP.set(toInputCodes(settings.bindJump));
        KeyBindings.WALK.set(toInputCodes(settings.bindWalk));
        KeyBindings.SPRINT.set(toInputCodes(settings.bindSprint));
        KeyBindings.INVENTORY.set(toInputCodes(settings.bindInventory));
        KeyBindings.TOGGLE_INSPECTOR.set(toInputCodes(settings.bindToggleInspector));
        KeyBindings.FOCUS_SELECTED.set(toInputCodes(settings.bindFocusSelected));
        KeyBindings.DELETE_SELECTED.set(toInputCodes(settings.bindDeleteSelected));
        KeyBindings.SAVE.set(toInputCodes(settings.bindSave));
        KeyBindings.UNDO.set(toInputCodes(settings.bindUndo));
        KeyBindings.REDO.set(toInputCodes(settings.bindRedo));
        KeyBindings.DUPLICATE.set(toInputCodes(settings.bindDuplicate));
        KeyBindings.OPEN_CONSOLE.set(toInputCodes(settings.bindOpenConsole));
    }

    public static void flushBindings(Settings settings) {

        settings.bindMoveForward = toCodes(KeyBindings.MOVE_FORWARD);
        settings.bindMoveBack = toCodes(KeyBindings.MOVE_BACK);
        settings.bindMoveLeft = toCodes(KeyBindings.MOVE_LEFT);
        settings.bindMoveRight = toCodes(KeyBindings.MOVE_RIGHT);
        settings.bindJump = toCodes(KeyBindings.JUMP);
        settings.bindWalk = toCodes(KeyBindings.WALK);
        settings.bindSprint = toCodes(KeyBindings.SPRINT);
        settings.bindInventory = toCodes(KeyBindings.INVENTORY);
        settings.bindToggleInspector = toCodes(KeyBindings.TOGGLE_INSPECTOR);
        settings.bindFocusSelected = toCodes(KeyBindings.FOCUS_SELECTED);
        settings.bindDeleteSelected = toCodes(KeyBindings.DELETE_SELECTED);
        settings.bindSave = toCodes(KeyBindings.SAVE);
        settings.bindUndo = toCodes(KeyBindings.UNDO);
        settings.bindRedo = toCodes(KeyBindings.REDO);
        settings.bindDuplicate = toCodes(KeyBindings.DUPLICATE);
        settings.bindOpenConsole = toCodes(KeyBindings.OPEN_CONSOLE);
    }

    // Internal \\

    private static InputCode[] toInputCodes(int[] codes) {
        InputCode[] result = new InputCode[codes.length];
        for (int i = 0; i < codes.length; i++)
            result[i] = InputCode.key(codes[i]);
        return result;
    }

    private static int[] toCodes(Binding binding) {
        InputCode[] codes = binding.getCodes();
        int[] result = new int[codes.length];
        for (int i = 0; i < codes.length; i++)
            result[i] = codes[i].code;
        return result;
    }
}