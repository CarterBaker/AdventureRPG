package com.AdventureRPG.Core.Util;

import com.AdventureRPG.Core.Util.Exceptions.FileException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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
}
