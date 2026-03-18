package com.internal.bootstrap.renderpipeline.rendercall;

import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.bootstrap.renderpipeline.util.MaskStruct;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.ubo.UBOInstance;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformStruct;
import com.internal.core.engine.StructPackage;

public class RenderCallStruct extends StructPackage {

    /*
     * Per-frame render submission. Pre-allocated in a fixed array by RenderManager
     * and handed out by cursor — never allocated per frame. Reset by cursor
     * rewind at the start of each draw. Never instantiate directly.
     */

    private static final UniformStruct<?>[] EMPTY_UNIFORMS = new UniformStruct<?>[0];
    private static final UBOInstance[] EMPTY_UBOS = new UBOInstance[0];

    // Internal
    private ModelInstance modelInstance;
    private MaterialInstance materialInstance;
    private UniformStruct<?>[] cachedUniforms;
    private UBOInstance[] cachedInstanceUBOs;
    private MaskStruct mask;

    // Init \\

    public void init(ModelInstance modelInstance, MaskStruct mask) {

        this.modelInstance = modelInstance;
        this.materialInstance = modelInstance.getMaterial();
        this.mask = mask;

        var uniforms = materialInstance.getUniforms();
        this.cachedUniforms = (uniforms != null && !uniforms.isEmpty())
                ? uniforms.values().toArray(new UniformStruct<?>[0])
                : EMPTY_UNIFORMS;

        var instanceUBOs = materialInstance.getInstanceUBOs();
        this.cachedInstanceUBOs = (instanceUBOs != null && !instanceUBOs.isEmpty())
                ? instanceUBOs.values().toArray(new UBOInstance[0])
                : EMPTY_UBOS;
    }

    // Accessible \\

    public ModelInstance getModelInstance() {
        return modelInstance;
    }

    public MaterialInstance getMaterialInstance() {
        return materialInstance;
    }

    public UniformStruct<?>[] getCachedUniforms() {
        return cachedUniforms;
    }

    public UBOInstance[] getCachedInstanceUBOs() {
        return cachedInstanceUBOs;
    }

    public MaskStruct getMask() {
        return mask;
    }

    public boolean hasMask() {
        return mask != null;
    }
}