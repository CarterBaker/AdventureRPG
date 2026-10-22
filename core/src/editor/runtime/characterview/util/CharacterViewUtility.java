package editor.runtime.characterview.util;

import java.math.BigDecimal;

import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.arpg.ArpgValueStruct;

public class CharacterViewUtility extends EngineUtility {

    /*
     * Stateless helpers the character tools share. Row keys pair a name with
     * an argument — an index, or a bone, slot or node name — so one panel
     * button names exactly what it edits. Every number a tool writes is
     * rounded to a fixed precision first, so files never fill with float
     * noise, and reads and writes the x, y and z of a vector whether it is
     * stored as an { x, y, z } object or a three-value array. Enum names read
     * back from a file being edited fall back rather than fail, so a value
     * typed wrong elsewhere never takes a tool down.
     */

    // Keys \\

    public static String toKey(String name, int index) {
        return name + EditorSetting.CHARACTER_VIEW_KEY_SEPARATOR + index;
    }

    public static String toKey(String name, String argument) {
        return name + EditorSetting.CHARACTER_VIEW_KEY_SEPARATOR + argument;
    }

    public static String getKeyName(String key) {

        int separator = key.indexOf(EditorSetting.CHARACTER_VIEW_KEY_SEPARATOR);
        return separator == EngineSetting.INDEX_NOT_FOUND ? key : key.substring(0, separator);
    }

    public static String getKeyArgument(String key) {

        int separator = key.indexOf(EditorSetting.CHARACTER_VIEW_KEY_SEPARATOR);

        return separator == EngineSetting.INDEX_NOT_FOUND
                ? ""
                : key.substring(separator + EditorSetting.CHARACTER_VIEW_KEY_SEPARATOR.length());
    }

    public static int getKeyIndex(String key) {

        String argument = getKeyArgument(key);

        try {
            return argument.isEmpty() ? EngineSetting.INDEX_NOT_FOUND : Integer.parseInt(argument);
        } catch (NumberFormatException e) {
            return EngineSetting.INDEX_NOT_FOUND;
        }
    }

    // Numbers \\

    public static float round(float value) {

        float scale = EditorSetting.CHARACTER_VIEW_DECIMAL_SCALE;
        return Math.round(value * scale) / scale;
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static String formatNumber(float value) {

        float rounded = round(value);

        if (rounded == 0f)
            return EditorSetting.CHARACTER_VIEW_ZERO_TEXT;

        return new BigDecimal(Float.toString(rounded)).stripTrailingZeros().toPlainString();
    }

    // Not a number when the text is not one, or not finite
    public static float parseNumber(String text) {

        try {
            float value = Float.parseFloat(text.trim());
            return Float.isFinite(value) ? value : Float.NaN;
        } catch (NumberFormatException e) {
            return Float.NaN;
        }
    }

    public static boolean isNumber(String text) {
        return !Float.isNaN(parseNumber(text));
    }

    // Enums \\

    // An enum constant by name ignoring case, or the fallback when a file holds a name the enum does not have
    public static <E extends Enum<E>> E toEnumOrDefault(String name, Class<E> enumClass, E fallback) {

        if (name == null)
            return fallback;

        for (E constant : enumClass.getEnumConstants())
            if (constant.name().equalsIgnoreCase(name))
                return constant;

        return fallback;
    }

    // Trees \\

    // The object under a key, added empty first when the key holds none
    public static ArpgObjectStruct requireObject(ArpgObjectStruct arpg, String key) {

        if (!ArpgUtility.hasObject(arpg, key))
            arpg.add(key, new ArpgObjectStruct());

        return arpg.getAsObject(key);
    }

    // The array under a key, added empty first when the key holds none
    public static ArpgArrayStruct requireArray(ArpgObjectStruct arpg, String key) {

        if (!ArpgUtility.hasArray(arpg, key))
            arpg.add(key, new ArpgArrayStruct());

        return arpg.getAsArray(key);
    }

    // The object under a key, or null when the key holds none
    public static ArpgObjectStruct findObject(ArpgObjectStruct arpg, String key) {
        return arpg != null && ArpgUtility.hasObject(arpg, key) ? arpg.getAsObject(key) : null;
    }

    // Vectors \\

    public static float getComponent(ArpgObjectStruct arpg, String key, int axis, float defaultValue) {

        if (arpg == null)
            return defaultValue;

        if (ArpgUtility.hasArray(arpg, key)) {

            ArpgArrayStruct vector = arpg.getAsArray(key);
            return axis < vector.size() ? vector.get(axis).getAsFloat() : defaultValue;
        }

        if (ArpgUtility.hasObject(arpg, key))
            return ArpgUtility.getFloat(
                    arpg.getAsObject(key),
                    EditorSetting.CHARACTER_VIEW_AXIS_NAMES[axis],
                    defaultValue);

        return defaultValue;
    }

    // Writes one value of a three-value array, filling a missing or short array with the default first
    public static void setArrayComponent(
            ArpgObjectStruct arpg,
            String key,
            int axis,
            float value,
            float defaultValue) {

        ArpgArrayStruct vector = ArpgUtility.hasArray(arpg, key) ? arpg.getAsArray(key) : new ArpgArrayStruct();

        while (vector.size() < EditorSetting.CHARACTER_VIEW_AXIS_NAMES.length)
            vector.add(defaultValue);

        vector.set(axis, new ArpgValueStruct(round(value)));
        arpg.add(key, vector);
    }

    // Writes one axis of an { x, y, z } object, giving a missing object every axis at the default first
    public static void setObjectComponent(
            ArpgObjectStruct arpg,
            String key,
            int axis,
            float value,
            float defaultValue) {

        ArpgObjectStruct vector = ArpgUtility.hasObject(arpg, key) ? arpg.getAsObject(key) : new ArpgObjectStruct();

        for (String axisName : EditorSetting.CHARACTER_VIEW_AXIS_NAMES)
            if (!vector.has(axisName))
                vector.addProperty(axisName, defaultValue);

        vector.addProperty(EditorSetting.CHARACTER_VIEW_AXIS_NAMES[axis], round(value));
        arpg.add(key, vector);
    }

    public static ArpgArrayStruct toArray(float x, float y, float z) {

        ArpgArrayStruct vector = new ArpgArrayStruct();
        vector.add(round(x));
        vector.add(round(y));
        vector.add(round(z));
        return vector;
    }
}
