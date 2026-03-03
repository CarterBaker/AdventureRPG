package com.internal.bootstrap.shaderpipeline.pass;

import com.internal.bootstrap.geometrypipeline.meshmanager.MeshHandle;
import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.ubo.UBOInstance;
import com.internal.core.engine.InstancePackage;

/*
 * Runtime pass handed to external systems by PassManager.clonePass().
 * Owns all data it needs directly — name, ID, a cloned MaterialInstance, and a
 * ModelInstance constructed from the shared mesh geometry and that material.
 */
public class PassInstance extends InstancePackage {

    // Internal
    private String passName;
    private int passID;
    private MaterialInstance material;
    private ModelInstance modelInstance;
    // Internal \\

    public void constructor(
            String passName,
            int passID,
            MeshHandle meshHandle,
            MaterialInstance material) {
        this.passName = passName;
        this.passID = passID;
        this.material = material;
        this.modelInstance = create(ModelInstance.class);
        this.modelInstance.constructor(meshHandle.getMeshStruct(), material);
    }

    // Utility \\

    public void setUBO(UBOInstance ubo) {
        material.setUBO(ubo);
    }

    public <T> void setUniform(String uniformName, T value) {
        material.setUniform(uniformName, value);
    }

    // Accessible \\

    public String getPassName() {
        return passName;
    }

    public int getPassID() {
        return passID;
    }

    public MaterialInstance getMaterial() {
        return material;
    }

    public ModelInstance getModelInstance() {
        return modelInstance;
    }
}