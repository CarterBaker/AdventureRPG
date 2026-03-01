package com.internal.bootstrap.shaderpipeline.passmanager;

import com.internal.bootstrap.geometrypipeline.meshmanager.MeshHandle;
import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.core.engine.HandlePackage;

/*
 * Compiled fullscreen pass owned by PassManager. Holds a ModelInstance built
 * from the processing triangle mesh and a MaterialInstance for the pass shader.
 * The ModelInstance is constructed directly here since PassHandle owns it for
 * its lifetime and it is never handed to external systems.
 */
public class PassHandle extends HandlePackage {

    // Internal
    private String passName;
    private int passID;
    private ModelInstance modelInstance;
    private MaterialInstance material;

    // Internal \\

    public void constructor(
            String passName,
            int passID,
            MaterialInstance material,
            MeshHandle processingTriangle) {
        this.passName = passName;
        this.passID = passID;
        this.material = material;
        this.modelInstance = create(ModelInstance.class);
        this.modelInstance.constructor(processingTriangle, material);
    }

    // Accessible \\

    public String getPassName() {
        return passName;
    }

    public int getPassID() {
        return passID;
    }

    public ModelInstance getModelHandle() {
        return modelInstance;
    }

    public MaterialInstance getMaterial() {
        return material;
    }
}