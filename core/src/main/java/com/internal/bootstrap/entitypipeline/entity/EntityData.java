package com.internal.bootstrap.entitypipeline.entity;

import com.internal.core.engine.DataPackage;
import com.internal.core.util.mathematics.vectors.Vector3;

public class EntityData extends DataPackage {

    // Size
    public final Vector3 sizeMin;
    public final Vector3 sizeMax;

    // Weight
    public final float weightMin;
    public final float weightMax;
    public final float eyeLevel;

    // Behavior
    public final String behaviorName;

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