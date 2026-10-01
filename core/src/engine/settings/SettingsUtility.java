package engine.settings;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import engine.input.Binding;
import engine.input.InputCode;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.arpg.ArpgValueStruct;

public class SettingsUtility extends EngineUtility {

    /*
     * Handles all Settings I/O and bridges Settings to KeyBindings. Single
     * point of contact for loading, saving, applying, flushing, and resetting
     * bindings. Every public field of Settings is stored under its own name,
     * so a new field persists without further work; a field missing from the
     * file keeps its default. Loaded values are clamped to the ranges the
     * settings menu offers, so a malformed file can never push the engine out
     * of bounds. Never held — all methods static.
     */

    // Settings \\

    public static Settings load(File file) {

        if (!file.exists()) {
            Settings defaults = new Settings();
            save(file, defaults);
            return defaults;
        }

        ArpgObjectStruct settingsArpg = ArpgUtility.tryLoadObject(file);
        Settings loaded = settingsArpg != null ? readSettings(settingsArpg) : null;

        if (loaded == null) {
            errorLog("Settings could not be read, defaults applied: " + file.getAbsolutePath());
            return new Settings();
        }

        sanitize(loaded);
        return loaded;
    }

    public static void save(File file, Settings settings) {
        ArpgUtility.tryWriteObject(file, writeSettings(settings));
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

    // Fields \\

    private static Settings readSettings(ArpgObjectStruct settingsArpg) {

        Settings settings = new Settings();

        try {

            for (Field field : Settings.class.getFields())
                if (isStoredField(field) && settingsArpg.has(field.getName()))
                    field.set(settings, readValue(field.getType(), settingsArpg.get(field.getName())));

            return settings;
        } catch (IllegalAccessException | RuntimeException e) {
            return null;
        }
    }

    private static ArpgObjectStruct writeSettings(Settings settings) {

        ArpgObjectStruct settingsArpg = new ArpgObjectStruct();

        try {

            for (Field field : Settings.class.getFields())
                if (isStoredField(field))
                    settingsArpg.add(field.getName(), writeValue(field.get(settings)));
        } catch (IllegalAccessException e) {
            throwException("Settings could not be captured for saving", e);
        }

        return settingsArpg;
    }

    private static boolean isStoredField(Field field) {
        return !Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers());
    }

    private static Object readValue(Class<?> type, ArpgElementStruct element) {

        if (type == boolean.class)
            return element.getAsBoolean();

        if (type == int.class)
            return element.getAsInt();

        if (type == float.class)
            return element.getAsFloat();

        if (type == int[].class)
            return readIntArray(element.getAsArray());

        if (type == float[].class)
            return readFloatArray(element.getAsArray());

        return throwException("Settings field type is not supported: " + type.getSimpleName());
    }

    private static ArpgElementStruct writeValue(Object value) {

        if (value instanceof Boolean booleanValue)
            return new ArpgValueStruct(booleanValue);

        if (value instanceof Integer intValue)
            return new ArpgValueStruct(intValue);

        if (value instanceof Float floatValue)
            return new ArpgValueStruct(floatValue);

        if (value instanceof int[] intArray)
            return writeIntArray(intArray);

        if (value instanceof float[] floatArray)
            return writeFloatArray(floatArray);

        return throwException("Settings field type is not supported: " + value.getClass().getSimpleName());
    }

    private static int[] readIntArray(ArpgArrayStruct array) {

        int[] values = new int[array.size()];

        for (int i = 0; i < values.length; i++)
            values[i] = array.get(i).getAsInt();

        return values;
    }

    private static float[] readFloatArray(ArpgArrayStruct array) {

        float[] values = new float[array.size()];

        for (int i = 0; i < values.length; i++)
            values[i] = array.get(i).getAsFloat();

        return values;
    }

    private static ArpgArrayStruct writeIntArray(int[] values) {

        ArpgArrayStruct array = new ArpgArrayStruct();

        for (int value : values)
            array.add(value);

        return array;
    }

    private static ArpgArrayStruct writeFloatArray(float[] values) {

        ArpgArrayStruct array = new ArpgArrayStruct();

        for (float value : values)
            array.add(value);

        return array;
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
        KeyBindings.ACTIVATE.set(toInputCodes(settings.bindActivate));
        KeyBindings.INVENTORY.set(toInputCodes(settings.bindInventory));
        KeyBindings.ROTATE_ITEM.set(toInputCodes(settings.bindRotateItem));
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
        settings.bindActivate = toCodes(KeyBindings.ACTIVATE);
        settings.bindInventory = toCodes(KeyBindings.INVENTORY);
        settings.bindRotateItem = toCodes(KeyBindings.ROTATE_ITEM);
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