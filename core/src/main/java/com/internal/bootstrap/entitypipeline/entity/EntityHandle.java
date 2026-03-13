package com.internal.bootstrap.entitypipeline.entity;

import com.internal.core.engine.HandlePackage;
import com.internal.core.util.mathematics.vectors.Vector3;

public class EntityHandle extends HandlePackage {

    // Internal
    private EntityData entityData;

    // Constructor \\

    public void constructor(EntityData entityData) {

        // Internal
        this.entityData = entityData;
    }

    // Accessible \\

    public EntityData getEntityData() {
        return entityData;
    }

    public float getWeightMin() {
        return entityData.weightMin;
    }

    public float getWeightMax() {
        return entityData.weightMax;
    }

    public float getEyeLevel() {
        return entityData.eyeLevel;
    }

    public String getBehaviorName() {
        return entityData.behaviorName;
    }

    public Vector3 getRandomSize() {
        return entityData.getRandomSize();
    }

    public float getRandomWeight() {
        return entityData.getRandomWeight();
    }
}