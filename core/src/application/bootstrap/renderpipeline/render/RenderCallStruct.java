package application.bootstrap.renderpipeline.render;

import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.uniforms.UniformStruct;
import engine.root.StructPackage;

public class RenderCallStruct extends StructPackage {

    /*
     * One render submission, drawn from a fixed pool by cursor and rewound each
     * frame. The mask is copied in on init, since submitters reuse pooled masks
     * while calls draw later.
     */

    private static final UniformStruct<?>[] EMPTY_UNIFORMS = new UniformStruct<?>[0];
    private static final UBOInstance[] EMPTY_UBOS = new UBOInstance[0];

    // Internal
    private ModelInstance modelInstance;
    private MaterialInstance materialInstance;
    private UniformStruct<?>[] cachedUniforms;
    private UBOInstance[] cachedInstanceUBOs;
    private final MaskStruct mask = new MaskStruct();
    private boolean masked;

    // Init \\

    public void init(ModelInstance modelInstance, MaskStruct mask) {

        this.modelInstance = modelInstance;
        this.materialInstance = modelInstance.getMaterial();
        this.masked = mask != null;

        if (masked)
            this.mask.set(mask);

        var keys = materialInstance.getUniformKeys();
        if (keys != null && !keys.isEmpty()) {
            var uniforms = materialInstance.getUniforms();
            this.cachedUniforms = new UniformStruct<?>[keys.size()];
            for (int i = 0; i < keys.size(); i++)
                this.cachedUniforms[i] = uniforms.get(keys.get(i));
        } else {
            this.cachedUniforms = EMPTY_UNIFORMS;
        }

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
        return masked ? mask : null;
    }

    public boolean hasMask() {
        return masked;
    }
}