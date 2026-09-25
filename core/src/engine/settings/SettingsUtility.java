package engine.settings;

import com.google.gson.Gson;

import engine.input.Binding;
import engine.input.InputCode;
import engine.root.EngineSetting;

import java.io.*;

public class SettingsUtility {

    /*
     * Handles all Settings I/O and bridges Settings to KeyBindings. Single
     * point of contact for loading, saving, applying, flushing, and resetting
     * bindings. Loaded values are clamped to the ranges the settings menu
     * offers, so a hand-edited file can never push the engine out of bounds.
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

        settings.nearTessellationRadius = Math.clamp(
                settings.nearTessellationRadius,
                EngineSetting.NEAR_TESSELLATION_RADIUS_MIN,
                EngineSetting.NEAR_TESSELLATION_RADIUS_MAX);
        settings.maxRenderDistance = Math.clamp(
                settings.maxRenderDistance,
                EngineSetting.RENDER_DISTANCE_MIN,
                EngineSetting.RENDER_DISTANCE_MAX);
        settings.FOV = Math.clamp(
                settings.FOV,
                EngineSetting.FIELD_OF_VIEW_MIN,
                EngineSetting.FIELD_OF_VIEW_MAX);
        settings.mouseSensitivity = Math.clamp(
                settings.mouseSensitivity,
                EngineSetting.MOUSE_SENSITIVITY_MIN,
                EngineSetting.MOUSE_SENSITIVITY_MAX);

        sanitizeColors(settings);
    }

    private static void sanitizeColors(Settings settings) {

        Settings defaults = new Settings();

        settings.uiColorBackground = sanitizeColor(settings.uiColorBackground, defaults.uiColorBackground);
        settings.uiColorPanel = sanitizeColor(settings.uiColorPanel, defaults.uiColorPanel);
        settings.uiColorHeader = sanitizeColor(settings.uiColorHeader, defaults.uiColorHeader);
        settings.uiColorControl = sanitizeColor(settings.uiColorControl, defaults.uiColorControl);
        settings.uiColorControlHover = sanitizeColor(settings.uiColorControlHover, defaults.uiColorControlHover);
        settings.uiColorAccent = sanitizeColor(settings.uiColorAccent, defaults.uiColorAccent);
        settings.uiColorAccentHover = sanitizeColor(settings.uiColorAccentHover, defaults.uiColorAccentHover);
        settings.uiColorOutline = sanitizeColor(settings.uiColorOutline, defaults.uiColorOutline);
        settings.uiColorShadow = sanitizeColor(settings.uiColorShadow, defaults.uiColorShadow);
        settings.uiColorText = sanitizeColor(settings.uiColorText, defaults.uiColorText);
        settings.uiColorTextMuted = sanitizeColor(settings.uiColorTextMuted, defaults.uiColorTextMuted);
        settings.uiColorTextOnAccent = sanitizeColor(settings.uiColorTextOnAccent, defaults.uiColorTextOnAccent);
        settings.uiColorDanger = sanitizeColor(settings.uiColorDanger, defaults.uiColorDanger);
    }

    private static float[] sanitizeColor(float[] color, float[] fallback) {

        if (color == null || color.length != EngineSetting.COLOR_CHANNEL_COUNT)
            return fallback;

        for (int i = 0; i < color.length; i++)
            color[i] = Math.max(EngineSetting.COLOR_CHANNEL_MIN,
                    Math.min(EngineSetting.COLOR_CHANNEL_MAX, color[i]));

        return color;
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
        KeyBindings.SECONDARY.set(toInputCodes(settings.bindSecondary));
        KeyBindings.SCREENSHOT.set(toInputCodes(settings.bindScreenshot));
        KeyBindings.RECORD_VIDEO.set(toInputCodes(settings.bindRecordVideo));
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
        settings.bindSecondary = toCodes(KeyBindings.SECONDARY);
        settings.bindScreenshot = toCodes(KeyBindings.SCREENSHOT);
        settings.bindRecordVideo = toCodes(KeyBindings.RECORD_VIDEO);
        settings.bindToggleInspector = toCodes(KeyBindings.TOGGLE_INSPECTOR);
        settings.bindFocusSelected = toCodes(KeyBindings.FOCUS_SELECTED);
        settings.bindDeleteSelected = toCodes(KeyBindings.DELETE_SELECTED);
        settings.bindSave = toCodes(KeyBindings.SAVE);
        settings.bindUndo = toCodes(KeyBindings.UNDO);
        settings.bindRedo = toCodes(KeyBindings.REDO);
        settings.bindDuplicate = toCodes(KeyBindings.DUPLICATE);
        settings.bindOpenConsole = toCodes(KeyBindings.OPEN_CONSOLE);
    }

    public static void resetBindings(Settings settings) {
        applyBindings(new Settings());
        flushBindings(settings);
    }

    // Internal \\

    private static InputCode[] toInputCodes(int[] codes) {
        if (codes == null)
            return null;
        InputCode[] result = new InputCode[codes.length];
        for (int i = 0; i < codes.length; i++)
            result[i] = InputCode.fromStoredCode(codes[i]);
        return result;
    }

    private static int[] toCodes(Binding binding) {
        InputCode[] codes = binding.getCodes();
        int[] result = new int[codes.length];
        for (int i = 0; i < codes.length; i++)
            result[i] = codes[i].toStoredCode();
        return result;
    }
}