package program.core.util;

import java.io.File;
import java.io.FileReader;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import program.core.engine.EngineUtility;

public class JsonUtility extends EngineUtility {

    // Loaders \\

    public static JsonObject loadJsonObject(File file) {
        try (FileReader reader = new FileReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            return throwException("Failed to load JSON file: " + file.getAbsolutePath(), e);
        }
    }

    // Required field accessors — throw if missing \\

    public static String validateString(JsonObject json, String key) {
        if (!json.has(key))
            return throwException("Missing required field: '" + key + "'");
        return json.get(key).getAsString();
    }

    public static int validateInt(JsonObject json, String key) {
        if (!json.has(key))
            return throwException("Missing required field: '" + key + "'");
        return json.get(key).getAsInt();
    }

    public static boolean validateBoolean(JsonObject json, String key) {
        if (!json.has(key))
            return throwException("Missing required field: '" + key + "'");
        return json.get(key).getAsBoolean();
    }

    public static float validateFloat(JsonObject json, String key) {
        if (!json.has(key))
            return throwException("Missing required field: '" + key + "'");
        return json.get(key).getAsFloat();
    }

    public static JsonArray validateArray(JsonObject json, String key) {
        if (!json.has(key))
            return throwException("Missing required array field: '" + key + "'");
        try {
            return json.getAsJsonArray(key);
        } catch (ClassCastException e) {
            return throwException("Field '" + key + "' is not a valid JSON array", e);
        }
    }

    public static JsonArray validateArray(JsonObject json, String key, int requiredSize) {
        JsonArray array = validateArray(json, key);
        if (requiredSize > 0 && array.size() != requiredSize)
            return throwException("Array '" + key + "' must have exactly "
                    + requiredSize + " elements, found " + array.size());
        return array;
    }

    // Optional field accessors — return default if missing \\

    public static String getString(JsonObject json, String key, String defaultValue) {
        return json.has(key) ? json.get(key).getAsString() : defaultValue;
    }

    public static int getInt(JsonObject json, String key, int defaultValue) {
        return json.has(key) ? json.get(key).getAsInt() : defaultValue;
    }

    public static boolean getBoolean(JsonObject json, String key, boolean defaultValue) {
        return json.has(key) ? json.get(key).getAsBoolean() : defaultValue;
    }

    public static float getFloat(JsonObject json, String key, float defaultValue) {
        return json.has(key) ? json.get(key).getAsFloat() : defaultValue;
    }
}