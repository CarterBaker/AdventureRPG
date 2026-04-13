package application.bootstrap.entitypipeline.entitymanager;

import java.io.File;
import com.google.gson.JsonObject;

import application.bootstrap.entitypipeline.entity.EntityData;
import application.bootstrap.entitypipeline.entity.EntityHandle;
import application.core.engine.BuilderPackage;
import application.core.settings.EngineSetting;
import application.core.util.JsonUtility;
import application.core.util.mathematics.vectors.Vector3;

class InternalBuilder extends BuilderPackage {

    /*
     * Parses entity template JSON into an EntityData and wraps it in an
     * EntityHandle. All size, weight, and eye level fields fall back to
     * engine defaults if not specified. Bootstrap-only.
     */

    // Build \\

    EntityHandle build(File file) {

        JsonObject json = JsonUtility.loadJsonObject(file);

        Vector3 sizeMin = parseSizeMin(json);
        Vector3 sizeMax = parseSizeMax(json);
        float weightMin = parseWeightMin(json);
        float weightMax = parseWeightMax(json);
        float eyeLevel = parseEyeLevel(json);
        String behaviorName = parseBehaviorName(json, file);

        EntityData entityData = new EntityData(
                sizeMin, sizeMax, weightMin, weightMax, eyeLevel, behaviorName);

        EntityHandle entityHandle = create(EntityHandle.class);
        entityHandle.constructor(entityData);

        return entityHandle;
    }

    // Parse \\

    private Vector3 parseSizeMin(JsonObject json) {

        if (!json.has("size_min"))
            return new Vector3(
                    EngineSetting.DEFAULT_ENTITY_SIZE,
                    EngineSetting.DEFAULT_ENTITY_SIZE,
                    EngineSetting.DEFAULT_ENTITY_SIZE);

        JsonObject o = json.getAsJsonObject("size_min");

        return new Vector3(
                o.has("x") ? o.get("x").getAsFloat() : EngineSetting.DEFAULT_ENTITY_SIZE,
                o.has("y") ? o.get("y").getAsFloat() : EngineSetting.DEFAULT_ENTITY_SIZE,
                o.has("z") ? o.get("z").getAsFloat() : EngineSetting.DEFAULT_ENTITY_SIZE);
    }

    private Vector3 parseSizeMax(JsonObject json) {

        if (!json.has("size_max"))
            return new Vector3(
                    EngineSetting.DEFAULT_ENTITY_SIZE,
                    EngineSetting.DEFAULT_ENTITY_SIZE,
                    EngineSetting.DEFAULT_ENTITY_SIZE);

        JsonObject o = json.getAsJsonObject("size_max");

        return new Vector3(
                o.has("x") ? o.get("x").getAsFloat() : EngineSetting.DEFAULT_ENTITY_SIZE,
                o.has("y") ? o.get("y").getAsFloat() : EngineSetting.DEFAULT_ENTITY_SIZE,
                o.has("z") ? o.get("z").getAsFloat() : EngineSetting.DEFAULT_ENTITY_SIZE);
    }

    private float parseWeightMin(JsonObject json) {
        return json.has("weight_min")
                ? json.get("weight_min").getAsFloat()
                : EngineSetting.DEFAULT_ENTITY_WEIGHT;
    }

    private float parseWeightMax(JsonObject json) {
        return json.has("weight_max")
                ? json.get("weight_max").getAsFloat()
                : EngineSetting.DEFAULT_ENTITY_WEIGHT;
    }

    private float parseEyeLevel(JsonObject json) {
        return json.has("eye_level")
                ? json.get("eye_level").getAsFloat()
                : EngineSetting.DEFAULT_EYE_LEVEL;
    }

    private String parseBehaviorName(JsonObject json, File file) {

        if (!json.has("behavior"))
            throwException("Entity JSON missing 'behavior' field: " + file.getAbsolutePath());

        return json.get("behavior").getAsString();
    }
}