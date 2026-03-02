package com.internal.bootstrap.shaderpipeline.passmanager;

import com.internal.bootstrap.geometrypipeline.meshmanager.MeshHandle;
import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.core.engine.HandlePackage;

/*
 * Compiled fullscreen pass owned by PassManager. Holds a ModelInstance built
 * from the processing triangle mesh and a MaterialInstance for the pass shader.
 * Retains the MeshHandle so PassManager.clonePass() can construct new ModelInstances
 * for each PassInstance without duplicating mesh geometry.
 */
public class PassHandle extends HandlePackage {

    // Internal
    private String passName;
    private int passID;
    private MeshHandle meshHandle;
    private ModelInstance modelInstance;
    private MaterialInstance material;
    // Internal \\

    public void constructor(
            String passName,
            int passID,
            MaterialInstance material,
            MeshHandle meshHandle) {
        this.passName = passName;
        this.passID = passID;
        this.meshHandle = meshHandle;
        this.material = material;
        this.modelInstance = create(ModelInstance.class);
        this.modelInstance.constructor(meshHandle.getMeshStruct(), material);
    }

    // Accessible \\

    public String getPassName() {
        return passName;
    }

    public int getPassID() {
        return passID;
    }

    public MeshHandle getMeshHandle() {
        return meshHandle;
    }

    public ModelInstance getModelInstance() {
        return modelInstance;
    }

    public MaterialInstance getMaterial() {
        return material;
    }
}