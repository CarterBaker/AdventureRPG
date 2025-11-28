package com.AdventureRPG.Core.Util;

import java.io.File;
import java.io.FileReader;

import com.AdventureRPG.Core.Util.Exceptions.FileException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonUtility {

    public static JsonArray validateArray(JsonObject json, String key) {
        return validateArray(json, key, 0);
    }

    public static JsonArray validateArray(JsonObject json, String key, int requiredSize) {

        JsonArray array = json.getAsJsonArray(key);

        if (array == null || (requiredSize > 0 && array.size() != requiredSize))
            throw new FileException.FileNotFoundException(null); // TODO: Not the best error

        return array;
    }

    public static JsonObject loadJsonObject(File file) {

        try (FileReader reader = new FileReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }

        catch (Exception e) {
            throw new FileException.FileNotFoundException(null); // TODO: Not the best error
        }
    }

}
