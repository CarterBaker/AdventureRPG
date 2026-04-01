package program.bootstrap.entitypipeline.entity;

import program.core.engine.DataPackage;
import program.core.util.mathematics.vectors.Vector3;

public class EntityData extends DataPackage {

    /*
     * Immutable entity template definition loaded from JSON. Holds the size
     * range, weight range, eye level, and behavior name for one entity type.
     * Owned by EntityHandle in the manager palette for the engine lifetime.
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

    // Constructor \\

    public EntityData(
            Vector3 sizeMin,
            Vector3 sizeMax,
            float weightMin,
            float weightMax,
            float eyeLevel,
            String behaviorName) {

        // Size
        this.sizeMin = sizeMin;
        this.sizeMax = sizeMax;

        // Weight
        this.weightMin = weightMin;
        this.weightMax = weightMax;
        this.eyeLevel = eyeLevel;

        // Behavior
        this.behaviorName = behaviorName;
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
}