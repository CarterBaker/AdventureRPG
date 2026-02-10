package com.internal.bootstrap.entitypipeline.entityManager;

import com.internal.core.engine.DataPackage;
import com.internal.core.util.mathematics.vectors.Vector3;

public class EntityData extends DataPackage {

    // Size
    private Vector3 sizeMin;
    private Vector3 sizeMax;

    // Weight
    private float weightMin;
    private float weightMax;

    private float eyeLevel;

    // Internal \\

    @Override
    protected void create() {
        this.sizeMin = new Vector3();
        this.sizeMax = new Vector3();
    }

    public void constructor(
            Vector3 sizeMin,
            Vector3 sizeMax,
            float weightMin,
            float weightMax,
            float eyeLevel) {

        this.sizeMin.set(sizeMin);
        this.sizeMax.set(sizeMax);
        this.weightMin = weightMin;
        this.weightMax = weightMax;
        this.eyeLevel = eyeLevel;
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