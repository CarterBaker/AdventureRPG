package com.AdventureRPG.core.util;

import java.io.File;
import java.io.FileReader;

import com.AdventureRPG.core.engine.UtilityPackage;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonUtility extends UtilityPackage {

    public static JsonArray validateArray(JsonObject json, String key) {
        return validateArray(json, key, 0);
    }

    public static JsonArray validateArray(JsonObject json, String key, int requiredSize) {

        JsonArray array = json.getAsJsonArray(key);

        if (array == null || (requiredSize > 0 && array.size() != requiredSize))
            throwException(); // TODO: Not the best error

        return array;
    }

    public static JsonObject loadJsonObject(File file) {

        try (FileReader reader = new FileReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }

        catch (Exception e) {
            return throwException(); // TODO: Not the best error
        }
    }

    public static String validateString(JsonObject json, String key) {
        if (!json.has(key)) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return json.get(key).getAsString();
    }
}
