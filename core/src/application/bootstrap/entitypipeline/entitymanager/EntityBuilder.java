package application.bootstrap.entitypipeline.entitymanager;

import java.io.File;
import com.google.gson.JsonObject;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.animationpipeline.animationmanager.AnimationManager;
import application.bootstrap.entitypipeline.entity.EntityData;
import application.bootstrap.entitypipeline.entity.EntityHandle;
import application.bootstrap.entitypipeline.entity.EntityState;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.io.JsonUtility;
import engine.util.mathematics.vectors.Vector3;

class EntityBuilder extends BuilderPackage {

    /*
     * Parses entity template JSON into an EntityData and wraps it in an
     * EntityHandle. All size, weight, and eye level fields fall back to
     * engine defaults if not specified. The optional "model" block resolves
     * a character mesh, a single shared material clone, and a rig, plus a
     * clip handle per EntityState this template declares animations for.
     * Bootstrap-only.
     */

    // Internal
    private MeshManager meshManager;
    private MaterialManager materialManager;
    private AnimationManager animationManager;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
        this.animationManager = get(AnimationManager.class);
    }

    // Build \\

    EntityHandle build(File file) {

        JsonObject json = JsonUtility.loadJsonObject(file);

        Vector3 sizeMin = parseSizeMin(json);
        Vector3 sizeMax = parseSizeMax(json);
        float weightMin = parseWeightMin(json);
        float weightMax = parseWeightMax(json);
        float eyeLevel = parseEyeLevel(json);
        String behaviorName = parseBehaviorName(json, file);

        MeshHandle characterMesh = null;
        MaterialInstance characterMaterial = null;
        AnimationClipHandle[] stateClips = null;

        if (json.has("model") && !json.get("model").isJsonNull()) {

            JsonObject modelJson = json.getAsJsonObject("model");
            String meshName = JsonUtility.validateString(modelJson, "mesh");
            String materialName = JsonUtility.validateString(modelJson, "material");

            characterMesh = meshManager.getMeshHandleFromMeshName(meshName);

            if (!characterMesh.hasRig())
                throwException("Entity model mesh \"" + meshName
                        + "\" has no rig — cannot be used as a character model. File: " + file.getName());

            characterMaterial = materialManager.cloneMaterial(materialName);
            stateClips = parseStateClips(modelJson, file);
        }

        EntityData entityData = new EntityData(
                sizeMin, sizeMax, weightMin, weightMax, eyeLevel, behaviorName,
                characterMesh, characterMaterial, stateClips);

        EntityHandle entityHandle = create(EntityHandle.class);
        entityHandle.constructor(entityData);

        return entityHandle;
    }

    // Model Parsing \\

    private AnimationClipHandle[] parseStateClips(JsonObject modelJson, File file) {

        AnimationClipHandle[] stateClips = new AnimationClipHandle[EntityState.values().length];

        if (!modelJson.has("animations") || modelJson.get("animations").isJsonNull())
            return stateClips;

        JsonObject animationsJson = modelJson.getAsJsonObject("animations");

        for (EntityState state : EntityState.values()) {

            String key = state.name().toLowerCase();

            if (!animationsJson.has(key))
                continue;

            String clipName = animationsJson.get(key).getAsString();
            stateClips[state.ordinal()] = animationManager.getClipHandleFromClipName(clipName);
        }

        return stateClips;
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