package com.internal.bootstrap.entitypipeline.entitymanager;

import java.io.File;
import com.google.gson.JsonObject;
import com.internal.bootstrap.entitypipeline.entity.EntityData;
import com.internal.bootstrap.entitypipeline.entity.EntityHandle;
import com.internal.core.engine.BuilderPackage;
import com.internal.core.util.JsonUtility;
import com.internal.core.util.mathematics.vectors.Vector3;

class InternalBuilder extends BuilderPackage {

    // Build \\

    EntityHandle build(File file) {

        JsonObject json = JsonUtility.loadJsonObject(file);
        Vector3 sizeMin = parseSizeMin(json);
        Vector3 sizeMax = parseSizeMax(json);
        float weightMin = parseWeightMin(json);
        float weightMax = parseWeightMax(json);
        float eyeLevel = parseEyeLevel(json);
        String behaviorName = parseBehaviorName(json, file);

        EntityData entityData = new EntityData(sizeMin, sizeMax, weightMin, weightMax, eyeLevel, behaviorName);

        EntityHandle entityHandle = create(EntityHandle.class);
        entityHandle.constructor(entityData);
        return entityHandle;
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

    private String parseBehaviorName(JsonObject json, File file) {

        if (!json.has("behavior"))
            throwException("Entity JSON missing 'behavior' field: " + file.getAbsolutePath());

        return json.get("behavior").getAsString();
    }
}