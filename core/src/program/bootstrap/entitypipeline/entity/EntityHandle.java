package program.bootstrap.entitypipeline.entity;

import program.core.engine.HandlePackage;
import program.core.util.mathematics.vectors.Vector3;

public class EntityHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded entity template. Registered and owned
     * by EntityManager. Used only as a template source — runtime entities are
     * always handed out as EntityInstance, never EntityHandle.
     */

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
        return entityData.getWeightMin();
    }

    public float getWeightMax() {
        return entityData.getWeightMax();
    }

    public float getEyeLevel() {
        return entityData.getEyeLevel();
    }

    public String getBehaviorName() {
        return entityData.getBehaviorName();
    }

    public Vector3 getRandomSize() {
        return entityData.getRandomSize();
    }

    public float getRandomWeight() {
        return entityData.getRandomWeight();
    }
}