package application.bootstrap.entitypipeline.entity;

import application.bootstrap.entitypipeline.race.RaceData;
import application.bootstrap.entitypipeline.race.RaceHandle;
import engine.root.DataPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;

public class EntityData extends DataPackage {

    /*
     * Immutable entity template loaded from ARPG: size, weight and eye level
     * ranges, behavior, the weapon its own body fights with when its hands
     * hold nothing it can wield, and the race it is made of, which it
     * requires. The race holds the genders a being of it may be made as and
     * the character model each requires — mesh, rig, material and animation
     * tree — with its appearance, equipment anchors and model height, so
     * every template of one race shares them.
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

    // Combat
    private final short weaponID;

    // Race
    private final RaceHandle raceHandle;

    // Constructor \\

    public EntityData(
            Vector3 sizeMin,
            Vector3 sizeMax,
            float weightMin,
            float weightMax,
            float eyeLevel,
            String behaviorName,
            short weaponID,
            RaceHandle raceHandle) {

        // Size
        this.sizeMin = sizeMin;
        this.sizeMax = sizeMax;

        // Weight
        this.weightMin = weightMin;
        this.weightMax = weightMax;
        this.eyeLevel = eyeLevel;

        // Behavior
        this.behaviorName = behaviorName;

        // Combat
        this.weaponID = weaponID;

        // Race
        this.raceHandle = raceHandle;
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

    public short getWeaponID() {
        return weaponID;
    }

    public RaceHandle getRaceHandle() {
        return raceHandle;
    }

    public RaceData getRaceData() {
        return raceHandle.getRaceData();
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