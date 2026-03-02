package com.internal.bootstrap.shaderpipeline.pass;

import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.passmanager.PassHandle;
import com.internal.bootstrap.shaderpipeline.ubo.UBOInstance;
import com.internal.bootstrap.shaderpipeline.uniforms.Uniform;
import com.internal.core.engine.InstancePackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/*
 * Runtime pass handed to external systems by PassManager.clonePass().
 * Holds a back-reference to its source PassHandle for shared state — mesh geometry
 * is never duplicated. Owns a cloned MaterialInstance and a ModelInstance constructed
 * from the shared mesh and that material.
 */
public class PassInstance extends InstancePackage {

    // Internal
    private PassHandle source;
    private MaterialInstance material;
    private ModelInstance modelInstance;
    // Internal \\

    public void constructor(
            PassHandle source,
            MaterialInstance material) {
        this.source = source;
        this.material = material;
        this.modelInstance = create(ModelInstance.class);
        this.modelInstance.constructor(source.getMeshHandle().getMeshStruct(), material);
    }

    // Utility \\

    public void setUBO(UBOInstance ubo) {
        material.setUBO(ubo);
    }

    public <T> void setUniform(String uniformName, T value) {
        material.setUniform(uniformName, value);
    }

    // Accessible \\

    public PassHandle getSource() {
        return source;
    }

    public String getPassName() {
        return source.getPassName();
    }

    public int getPassID() {
        return source.getPassID();
    }

    public MaterialInstance getMaterial() {
        return material;
    }

    public ModelInstance getModelInstance() {
        return modelInstance;
    }
}