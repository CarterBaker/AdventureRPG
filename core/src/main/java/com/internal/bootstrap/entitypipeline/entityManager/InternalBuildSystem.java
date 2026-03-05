package com.internal.bootstrap.entitypipeline.entitymanager;

import java.io.File;

import com.google.gson.JsonObject;
import com.internal.core.engine.BuilderPackage;
import com.internal.core.util.JsonUtility;
import com.internal.core.util.mathematics.vectors.Vector3;

class InternalBuildSystem extends BuilderPackage {

    // Build \\

    EntityData build(File root, File file, int templateID) {

        JsonObject json = JsonUtility.loadJsonObject(file);

        Vector3 sizeMin = parseSizeMin(json);
        Vector3 sizeMax = parseSizeMax(json);
        float weightMin = parseWeightMin(json);
        float weightMax = parseWeightMax(json);
        float eyeLevel = parseEyeLevel(json);

        if (sizeMin == null || sizeMax == null)
            return null;

        EntityData templateData = create(EntityData.class);
        templateData.constructor(sizeMin, sizeMax, weightMin, weightMax, eyeLevel);
        return templateData;
    }

    // Parse \\

    private Vector3 parseSizeMin(JsonObject json) {
        if (!json.has("size_min"))
            return new Vector3(1f, 1f, 1f);
        JsonObject o = json.getAsJsonObject("size_min");
        return new Vector3(
                o.has("x") ? o.get("x").getAsFloat() : 1f,
                o.has("y") ? o.get("y").getAsFloat() : 1f,
                o.has("z") ? o.get("z").getAsFloat() : 1f);
    }

    private Vector3 parseSizeMax(JsonObject json) {
        if (!json.has("size_max"))
            return new Vector3(1f, 1f, 1f);
        JsonObject o = json.getAsJsonObject("size_max");
        return new Vector3(
                o.has("x") ? o.get("x").getAsFloat() : 1f,
                o.has("y") ? o.get("y").getAsFloat() : 1f,
                o.has("z") ? o.get("z").getAsFloat() : 1f);
    }

    private float parseWeightMin(JsonObject json) {
        return json.has("weight_min") ? json.get("weight_min").getAsFloat() : 1f;
    }

    private float parseWeightMax(JsonObject json) {
        return json.has("weight_max") ? json.get("weight_max").getAsFloat() : 1f;
    }

    private float parseEyeLevel(JsonObject json) {
        return json.has("eye_level") ? json.get("eye_level").getAsFloat() : 0.91f;
    }
}