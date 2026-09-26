package application.bootstrap.entitypipeline.entity;

import application.bootstrap.entitypipeline.animationtree.AnimationTreeHandle;
import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.inventory.EquipmentAnchorStruct;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import engine.root.DataPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class EntityData extends DataPackage {

    /*
     * Immutable entity template definition loaded from JSON. Holds the size
     * range, weight range, eye level, and behavior name for one entity type.
     * Owned by EntityHandle in the manager palette for the engine lifetime.
     *
     * characterMesh, characterMaterial, and animationTreeHandle are all null
     * for any entity template with no "model" block in its JSON — entirely
     * optional.
     * characterMaterial is resolved exactly once here, at template-load
     * time, and every EntityInstance of this template shares this exact
     * same reference — the same guarantee EntityData itself already gives
     * every other field. This is load-bearing for instancing: a distinct
     * MaterialInstance per entity would make every entity its own draw
     * batch of one — per-entity looks ride in each instance's appearance
     * row instead (see AppearanceHandle). appearanceData is null unless the
     * "model" block also declares an "appearance". modelHeight is the full
     * authored height of the character — body plus default head — that an
     * entity's size.y is divided by to scale the model. equipmentAnchors
     * places worn items on the model and is empty for an entity with no
     * "equipment" block.
     */

    // Size
    private final Vector3 sizeMin;
    private final Vector3 sizeMax;

    // Weight
    private final float weightMin;
    private final float weightMax;
    private final float eyeLevel;

    // Behavior
    private final String behaviorName;

    // Model — optional
    private final MeshHandle characterMesh;
    private final MaterialInstance characterMaterial;
    private final RigHandle rigHandle;
    private final AnimationTreeHandle animationTreeHandle;
    private final float modelHeight;

    // Appearance — optional
    private final AppearanceData appearanceData;

    // Equipment
    private final ObjectArrayList<EquipmentAnchorStruct> equipmentAnchors;

    // Constructor \\

    public EntityData(
            Vector3 sizeMin,
            Vector3 sizeMax,
            float weightMin,
            float weightMax,
            float eyeLevel,
            String behaviorName,
            MeshHandle characterMesh,
            MaterialInstance characterMaterial,
            AnimationTreeHandle animationTreeHandle,
            float modelHeight,
            AppearanceData appearanceData,
            ObjectArrayList<EquipmentAnchorStruct> equipmentAnchors) {

        // Size
        this.sizeMin = sizeMin;
        this.sizeMax = sizeMax;

        // Weight
        this.weightMin = weightMin;
        this.weightMax = weightMax;
        this.eyeLevel = eyeLevel;

        // Behavior
        this.behaviorName = behaviorName;

        // Model
        this.characterMesh = characterMesh;
        this.characterMaterial = characterMaterial;
        this.rigHandle = characterMesh != null ? characterMesh.getRigHandle() : null;
        this.animationTreeHandle = animationTreeHandle;
        this.modelHeight = modelHeight;

        // Appearance
        this.appearanceData = appearanceData;

        // Equipment
        this.equipmentAnchors = equipmentAnchors;
    }

    // Accessible \\

    public Vector3 getSizeMin() {
        return sizeMin;
    }

    public Vector3 getSizeMax() {
        return sizeMax;
    }

    public float getWeightMin() {
        return weightMin;
    }

    public float getWeightMax() {
        return weightMax;
    }

    public float getEyeLevel() {
        return eyeLevel;
    }

    public String getBehaviorName() {
        return behaviorName;
    }

    public boolean hasCharacterModel() {
        return characterMesh != null;
    }

    public MeshHandle getCharacterMesh() {
        return characterMesh;
    }

    public MaterialInstance getCharacterMaterial() {
        return characterMaterial;
    }

    public RigHandle getRigHandle() {
        return rigHandle;
    }

    public AnimationTreeHandle getAnimationTreeHandle() {
        return animationTreeHandle;
    }

    public float getModelHeight() {
        return modelHeight;
    }

    public boolean hasAppearance() {
        return appearanceData != null;
    }

    public AppearanceData getAppearanceData() {
        return appearanceData;
    }

    public ObjectArrayList<EquipmentAnchorStruct> getEquipmentAnchors() {
        return equipmentAnchors;
    }

    // Utility \\

    public Vector3 getRandomSize() {

        float x = sizeMin.x + (float) (Math.random() * (sizeMax.x - sizeMin.x));
        float y = sizeMin.y + (float) (Math.random() * (sizeMax.y - sizeMin.y));
        float z = sizeMin.z + (float) (Math.random() * (sizeMax.z - sizeMin.z));

        return new Vector3(x, y, z);
    }

    public float getRandomWeight() {
        return weightMin + (float) (Math.random() * (weightMax - weightMin));
    }

    public float getWeightRatio(float weight) {

        float range = weightMax - weightMin;

        if (range <= 0f)
            return EngineSetting.DEFAULT_WEIGHT_RATIO;

        return Math.max(0f, Math.min(1f, (weight - weightMin) / range));
    }
}