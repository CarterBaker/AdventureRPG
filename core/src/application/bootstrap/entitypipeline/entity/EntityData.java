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
     * Immutable entity template loaded from JSON: size, weight and eye level
     * ranges, behavior, and the optional character model with its material,
     * animation tree, appearance, authored height and equipment anchors. The
     * material is resolved once per template and shared by every instance so
     * characters batch together.
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