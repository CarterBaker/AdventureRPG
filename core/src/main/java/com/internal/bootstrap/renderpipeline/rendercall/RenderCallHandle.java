package com.internal.bootstrap.renderpipeline.rendercall;

import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.ubo.UBOInstance;
import com.internal.bootstrap.shaderpipeline.uniforms.Uniform;
import com.internal.core.engine.HandlePackage;

/*
 * Single render submission pairing a ModelInstance with its per-instance
 * material data. Uniform and instance UBO arrays are cached at construction
 * time so the draw loop iterates plain arrays with no collection access.
 * Owned by RenderBatchHandle and cleared after each draw flush.
 */
public class RenderCallHandle extends HandlePackage {

    private static final Uniform<?>[] EMPTY_UNIFORMS = new Uniform[0];
    private static final UBOInstance[] EMPTY_UBOS = new UBOInstance[0];

    // Internal
    private ModelInstance modelInstance;
    private MaterialInstance materialInstance;
    private Uniform<?>[] cachedUniforms;
    private UBOInstance[] cachedInstanceUBOs;
    // Internal \\

    public void constructor(ModelInstance modelInstance) {

        this.modelInstance = modelInstance;
        this.materialInstance = modelInstance.getMaterial();

        var uniforms = materialInstance.getUniforms();
        this.cachedUniforms = (uniforms != null && !uniforms.isEmpty())
                ? uniforms.values().toArray(new Uniform[0])
                : EMPTY_UNIFORMS;

        var instanceUBOs = materialInstance.getInstanceUBOs();
        this.cachedInstanceUBOs = (instanceUBOs != null && !instanceUBOs.isEmpty())
                ? instanceUBOs.values().toArray(new UBOInstance[0])
                : EMPTY_UBOS;
    }

    public void dispose() {
        this.modelInstance = null;
        this.materialInstance = null;
        this.cachedUniforms = null;
        this.cachedInstanceUBOs = null;
    }

    // Accessible \\

    public ModelInstance getModelInstance() {
        return modelInstance;
    }

    public MaterialInstance getMaterialInstance() {
        return materialInstance;
    }

    public Uniform<?>[] getCachedUniforms() {
        return cachedUniforms;
    }

    public UBOInstance[] getCachedInstanceUBOs() {
        return cachedInstanceUBOs;
    }
}