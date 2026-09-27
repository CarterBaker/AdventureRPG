package application.bootstrap.entitypipeline.entitymanager;

import java.io.File;

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
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class EntityBuilder extends BuilderPackage {

    /*
     * Parses entity template ARPG into EntityData wrapped in an EntityHandle,
     * defaulting size, weight and eye level. The optional model block resolves
     * mesh, shared material, rig and animation tree, plus appearance and
     * equipment anchors. Bootstrap only.
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

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);

        Vector3 sizeMin = parseSizeMin(arpg);
        Vector3 sizeMax = parseSizeMax(arpg);
        float weightMin = parseWeightMin(arpg);
        float weightMax = parseWeightMax(arpg);
        float eyeLevel = parseEyeLevel(arpg);
        String behaviorName = parseBehaviorName(arpg, file);

        MeshHandle characterMesh = null;
        MaterialInstance characterMaterial = null;
        AnimationTreeHandle animationTreeHandle = null;
        float modelHeight = 0f;
        AppearanceData appearanceData = null;
        ObjectArrayList<EquipmentAnchorStruct> equipmentAnchors = new ObjectArrayList<>();

        if (arpg.has("model") && !arpg.get("model").isNull()) {

            ArpgObjectStruct modelArpg = arpg.getAsObject("model");
            String meshName = ArpgUtility.validateString(modelArpg, "mesh");
            String materialName = ArpgUtility.validateString(modelArpg, "material");

            characterMesh = meshManager.getMeshHandleFromMeshName(meshName);

            if (!characterMesh.hasRig())
                throwException("Entity model mesh \"" + meshName
                        + "\" has no rig — cannot be used as a character model. File: " + file.getName());

            characterMaterial = materialManager.cloneMaterial(materialName);
            animationTreeHandle = parseAnimationTree(modelArpg, characterMesh, file);

            if (ArpgUtility.hasObject(modelArpg, "appearance"))
                appearanceData = appearanceBuilder.build(
                        modelArpg.getAsObject("appearance"),
                        characterMesh.getRigHandle(),
                        file);

            modelHeight = resolveModelHeight(characterMesh, appearanceData);

            if (ArpgUtility.hasArray(modelArpg, "equipment"))
                parseEquipmentAnchors(
                        modelArpg.getAsArray("equipment"),
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

    private AnimationTreeHandle parseAnimationTree(ArpgObjectStruct modelArpg, MeshHandle characterMesh, File file) {

        String treeName = ArpgUtility.validateString(modelArpg, "animation_tree");
        AnimationTreeHandle animationTreeHandle = animationTreeManager.getAnimationTreeHandleFromTreeName(treeName);

        if (animationTreeHandle.getRigHandle() != characterMesh.getRigHandle())
            throwException("Entity animation tree \"" + treeName
                    + "\" targets a different rig than its model mesh. File: " + file.getName());

        return animationTreeHandle;
    }

    private void parseEquipmentAnchors(
            ArpgArrayStruct anchorsArpg,
            RigHandle rigHandle,
            ObjectArrayList<EquipmentAnchorStruct> equipmentAnchors,
            File file) {

        for (int i = 0; i < anchorsArpg.size(); i++) {

            ArpgObjectStruct anchorArpg = anchorsArpg.get(i).getAsObject();
            EquipmentSlot equipmentSlot = ArpgUtility.toEnum(
                    ArpgUtility.validateString(anchorArpg, "slot"), EquipmentSlot.class);
            String boneName = ArpgUtility.validateString(anchorArpg, "bone");

            if (!rigHandle.hasBone(boneName))
                throwException("Entity equipment anchor for slot \"" + equipmentSlot
                        + "\" names bone \"" + boneName + "\", which its rig does not have. File: " + file.getName());

            equipmentAnchors.add(new EquipmentAnchorStruct(
                    equipmentSlot,
                    rigHandle.getBoneIndex(boneName),
                    parseAnchorVector(anchorArpg, "position", 0f),
                    parseAnchorVector(anchorArpg, "rotation", 0f),
                    parseAnchorVector(anchorArpg, "size", EngineSetting.DEFAULT_ENTITY_SIZE),
                    ArpgUtility.getBoolean(anchorArpg, "hold", false)));
        }
    }

    private Vector3 parseAnchorVector(ArpgObjectStruct anchorArpg, String key, float defaultValue) {

        if (!ArpgUtility.hasArray(anchorArpg, key))
            return new Vector3(defaultValue);

        ArpgArrayStruct vectorArpg = ArpgUtility.validateArray(anchorArpg, key, 3);

        return new Vector3(
                vectorArpg.get(0).getAsFloat(),
                vectorArpg.get(1).getAsFloat(),
                vectorArpg.get(2).getAsFloat());
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

    private Vector3 parseSizeMin(ArpgObjectStruct arpg) {

        if (!arpg.has("size_min"))
            return new Vector3(
                    EngineSetting.DEFAULT_ENTITY_SIZE,
                    EngineSetting.DEFAULT_ENTITY_SIZE,
                    EngineSetting.DEFAULT_ENTITY_SIZE);

        ArpgObjectStruct o = arpg.getAsObject("size_min");

        return new Vector3(
                o.has("x") ? o.get("x").getAsFloat() : EngineSetting.DEFAULT_ENTITY_SIZE,
                o.has("y") ? o.get("y").getAsFloat() : EngineSetting.DEFAULT_ENTITY_SIZE,
                o.has("z") ? o.get("z").getAsFloat() : EngineSetting.DEFAULT_ENTITY_SIZE);
    }

    private Vector3 parseSizeMax(ArpgObjectStruct arpg) {

        if (!arpg.has("size_max"))
            return new Vector3(
                    EngineSetting.DEFAULT_ENTITY_SIZE,
                    EngineSetting.DEFAULT_ENTITY_SIZE,
                    EngineSetting.DEFAULT_ENTITY_SIZE);

        ArpgObjectStruct o = arpg.getAsObject("size_max");

        return new Vector3(
                o.has("x") ? o.get("x").getAsFloat() : EngineSetting.DEFAULT_ENTITY_SIZE,
                o.has("y") ? o.get("y").getAsFloat() : EngineSetting.DEFAULT_ENTITY_SIZE,
                o.has("z") ? o.get("z").getAsFloat() : EngineSetting.DEFAULT_ENTITY_SIZE);
    }

    private float parseWeightMin(ArpgObjectStruct arpg) {
        return arpg.has("weight_min")
                ? arpg.get("weight_min").getAsFloat()
                : EngineSetting.DEFAULT_ENTITY_WEIGHT;
    }

    private float parseWeightMax(ArpgObjectStruct arpg) {
        return arpg.has("weight_max")
                ? arpg.get("weight_max").getAsFloat()
                : EngineSetting.DEFAULT_ENTITY_WEIGHT;
    }

    private float parseEyeLevel(ArpgObjectStruct arpg) {
        return arpg.has("eye_level")
                ? arpg.get("eye_level").getAsFloat()
                : EngineSetting.DEFAULT_EYE_LEVEL;
    }

    private String parseBehaviorName(ArpgObjectStruct arpg, File file) {

        if (!arpg.has("behavior"))
            throwException("Entity ARPG missing 'behavior' field: " + file.getAbsolutePath());

        return arpg.get("behavior").getAsString();
    }
}