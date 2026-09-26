package application.bootstrap.entitypipeline.entitymanager;

import java.io.File;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import application.bootstrap.entitypipeline.animationtree.AnimationTreeHandle;
import application.bootstrap.entitypipeline.animationtreemanager.AnimationTreeManager;
import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.entity.EntityData;
import application.bootstrap.entitypipeline.entity.EntityHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.entitypipeline.inventory.EquipmentAnchorStruct;
import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.io.JsonUtility;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class EntityBuilder extends BuilderPackage {

    /*
     * Parses entity template JSON into an EntityData and wraps it in an
     * EntityHandle. All size, weight, and eye level fields fall back to
     * engine defaults if not specified. The optional "model" block resolves
     * a character mesh, a single shared material clone, and a rig, plus the
     * animation tree that drives it, which must be built on that same rig,
     * and — through AppearanceBuilder — the optional "appearance" block of
     * swappable features. The model's full height is read off the body
     * mesh and, when present, the default head together. The model's optional
     * "equipment" array anchors each equipment slot's worn item to a bone of
     * that rig. Bootstrap-only.
     */

    // Internal
    private MeshManager meshManager;
    private MaterialManager materialManager;
    private AnimationTreeManager animationTreeManager;
    private AppearanceBuilder appearanceBuilder;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
        this.animationTreeManager = get(AnimationTreeManager.class);
        this.appearanceBuilder = get(AppearanceBuilder.class);
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
        AnimationTreeHandle animationTreeHandle = null;
        float modelHeight = 0f;
        AppearanceData appearanceData = null;
        ObjectArrayList<EquipmentAnchorStruct> equipmentAnchors = new ObjectArrayList<>();

        if (json.has("model") && !json.get("model").isJsonNull()) {

            JsonObject modelJson = json.getAsJsonObject("model");
            String meshName = JsonUtility.validateString(modelJson, "mesh");
            String materialName = JsonUtility.validateString(modelJson, "material");

            characterMesh = meshManager.getMeshHandleFromMeshName(meshName);

            if (!characterMesh.hasRig())
                throwException("Entity model mesh \"" + meshName
                        + "\" has no rig — cannot be used as a character model. File: " + file.getName());

            characterMaterial = materialManager.cloneMaterial(materialName);
            animationTreeHandle = parseAnimationTree(modelJson, characterMesh, file);

            if (JsonUtility.hasObject(modelJson, "appearance"))
                appearanceData = appearanceBuilder.build(
                        modelJson.getAsJsonObject("appearance"),
                        characterMesh.getRigHandle(),
                        file);

            modelHeight = resolveModelHeight(characterMesh, appearanceData);

            if (JsonUtility.hasArray(modelJson, "equipment"))
                parseEquipmentAnchors(
                        modelJson.getAsJsonArray("equipment"),
                        characterMesh.getRigHandle(),
                        equipmentAnchors,
                        file);
        }

        EntityData entityData = new EntityData(
                sizeMin, sizeMax, weightMin, weightMax, eyeLevel, behaviorName,
                characterMesh, characterMaterial, animationTreeHandle, modelHeight, appearanceData,
                equipmentAnchors);

        EntityHandle entityHandle = create(EntityHandle.class);
        entityHandle.constructor(entityData);

        return entityHandle;
    }

    // Model Parsing \\

    private AnimationTreeHandle parseAnimationTree(JsonObject modelJson, MeshHandle characterMesh, File file) {

        String treeName = JsonUtility.validateString(modelJson, "animation_tree");
        AnimationTreeHandle animationTreeHandle = animationTreeManager.getAnimationTreeHandleFromTreeName(treeName);

        if (animationTreeHandle.getRigHandle() != characterMesh.getRigHandle())
            throwException("Entity animation tree \"" + treeName
                    + "\" targets a different rig than its model mesh. File: " + file.getName());

        return animationTreeHandle;
    }

    private void parseEquipmentAnchors(
            JsonArray anchorsJson,
            RigHandle rigHandle,
            ObjectArrayList<EquipmentAnchorStruct> equipmentAnchors,
            File file) {

        for (int i = 0; i < anchorsJson.size(); i++) {

            JsonObject anchorJson = anchorsJson.get(i).getAsJsonObject();
            EquipmentSlot equipmentSlot = JsonUtility.toEnum(
                    JsonUtility.validateString(anchorJson, "slot"), EquipmentSlot.class);
            String boneName = JsonUtility.validateString(anchorJson, "bone");

            if (!rigHandle.hasBone(boneName))
                throwException("Entity equipment anchor for slot \"" + equipmentSlot
                        + "\" names bone \"" + boneName + "\", which its rig does not have. File: " + file.getName());

            equipmentAnchors.add(new EquipmentAnchorStruct(
                    equipmentSlot,
                    rigHandle.getBoneIndex(boneName),
                    parseAnchorVector(anchorJson, "position", 0f),
                    parseAnchorVector(anchorJson, "rotation", 0f),
                    parseAnchorVector(anchorJson, "size", EngineSetting.DEFAULT_ENTITY_SIZE),
                    JsonUtility.getBoolean(anchorJson, "hold", false)));
        }
    }

    private Vector3 parseAnchorVector(JsonObject anchorJson, String key, float defaultValue) {

        if (!JsonUtility.hasArray(anchorJson, key))
            return new Vector3(defaultValue);

        JsonArray vectorJson = JsonUtility.validateArray(anchorJson, key, 3);

        return new Vector3(
                vectorJson.get(0).getAsFloat(),
                vectorJson.get(1).getAsFloat(),
                vectorJson.get(2).getAsFloat());
    }

    private float resolveModelHeight(MeshHandle characterMesh, AppearanceData appearanceData) {

        if (appearanceData == null)
            return characterMesh.getHeight();

        MeshHandle headMesh = appearanceData.getDefaultFeature(FeatureSlot.HEAD).getMeshHandle();
        float bottom = Math.min(characterMesh.getBoundsMin().y, headMesh.getBoundsMin().y);
        float top = Math.max(characterMesh.getBoundsMax().y, headMesh.getBoundsMax().y);

        return top - bottom;
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