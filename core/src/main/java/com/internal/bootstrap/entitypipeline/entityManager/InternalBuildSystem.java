package com.internal.bootstrap.entitypipeline.entityManager;

import java.io.File;

import com.google.gson.JsonObject;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.JsonUtility;
import com.internal.core.util.mathematics.vectors.Vector3;

class InternalBuildSystem extends SystemPackage {

    // Build \\

    EntityData buildTemplateData(
            File root,
            File file,
            int templateID,
            InternalLoadManager loadManager) {

        JsonObject jsonObject = JsonUtility.loadJsonObject(file);

        Vector3 sizeMin = parseSizeMin(jsonObject);
        Vector3 sizeMax = parseSizeMax(jsonObject);
        float weightMin = parseWeightMin(jsonObject);
        float weightMax = parseWeightMax(jsonObject);
        float eyeLevel = parseEyeLevel(jsonObject);

        return createTemplateData(
                sizeMin,
                sizeMax,
                weightMin,
                weightMax,
                eyeLevel);
    }

    private EntityData createTemplateData(
            Vector3 sizeMin,
            Vector3 sizeMax,
            float weightMin,
            float weightMax,
            float eyeLevel) {

        if (sizeMin == null || sizeMax == null) {
            return null;
        }

        EntityData templateData = create(EntityData.class);
        templateData.constructor(
                sizeMin,
                sizeMax,
                weightMin,
                weightMax,
                eyeLevel);

        return templateData;
    }

    // Parsing \\

    private Vector3 parseSizeMin(JsonObject jsonObject) {
        if (!jsonObject.has("size_min"))
            return new Vector3(1f, 1f, 1f); // Default

        JsonObject sizeObj = jsonObject.getAsJsonObject("size_min");
        float x = sizeObj.has("x") ? sizeObj.get("x").getAsFloat() : 1f;
        float y = sizeObj.has("y") ? sizeObj.get("y").getAsFloat() : 1f;
        float z = sizeObj.has("z") ? sizeObj.get("z").getAsFloat() : 1f;

        return new Vector3(x, y, z);
    }

    private Vector3 parseSizeMax(JsonObject jsonObject) {
        if (!jsonObject.has("size_max"))
            return new Vector3(1f, 1f, 1f); // Default

        JsonObject sizeObj = jsonObject.getAsJsonObject("size_max");
        float x = sizeObj.has("x") ? sizeObj.get("x").getAsFloat() : 1f;
        float y = sizeObj.has("y") ? sizeObj.get("y").getAsFloat() : 1f;
        float z = sizeObj.has("z") ? sizeObj.get("z").getAsFloat() : 1f;

        return new Vector3(x, y, z);
    }

    private float parseWeightMin(JsonObject jsonObject) {
        if (!jsonObject.has("weight_min"))
            return 1f; // Default

        return jsonObject.get("weight_min").getAsFloat();
    }

    private float parseWeightMax(JsonObject jsonObject) {
        if (!jsonObject.has("weight_max"))
            return 1f; // Default

        return jsonObject.get("weight_max").getAsFloat();
    }

    private float parseEyeLevel(JsonObject jsonObject) {
        if (!jsonObject.has("eye_level"))
            return 0.91f;

        return jsonObject.get("eye_level").getAsFloat();
    }
}