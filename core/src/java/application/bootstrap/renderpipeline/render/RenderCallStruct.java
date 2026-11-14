package application.bootstrap.renderpipeline.render;

import java.util.function.Consumer;

import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.uniforms.UniformStruct;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RenderCallStruct extends StructPackage {

    /*
     * One render submission, drawn from a fixed pool by cursor and rewound each
     * frame. The mask is copied in on init, since submitters reuse pooled masks
     * while calls draw later. The material's uniforms and instance UBOs are
     * snapshotted into grow-only arrays read up to their counts, so a call
     * allocates nothing once its arrays have reached the largest material.
     */

    // Internal
    private ModelInstance modelInstance;
    private MaterialInstance materialInstance;
    private final MaskStruct mask = new MaskStruct();
    private boolean masked;

    // Snapshot
    private UniformStruct<?>[] cachedUniforms = new UniformStruct<?>[0];
    private int cachedUniformCount;
    private UBOInstance[] cachedInstanceUBOs = new UBOInstance[0];
    private int cachedInstanceUBOCount;
    private final Consumer<UBOInstance> instanceUBOCollector = this::collectInstanceUBO;

    // Init \\

    public void init(ModelInstance modelInstance, MaskStruct mask) {

        this.modelInstance = modelInstance;
        this.materialInstance = modelInstance.getMaterial();
        this.masked = mask != null;

        if (masked)
            this.mask.set(mask);

        snapshotUniforms();
        snapshotInstanceUBOs();
    }

    private void snapshotUniforms() {

        ObjectArrayList<String> keys = materialInstance.getUniformKeys();
        int keyCount = keys == null ? 0 : keys.size();

        if (cachedUniforms.length < keyCount)
            cachedUniforms = new UniformStruct<?>[keyCount];

        Object2ObjectOpenHashMap<String, UniformStruct<?>> uniforms = materialInstance.getUniforms();

        for (int i = 0; i < keyCount; i++)
            cachedUniforms[i] = uniforms.get(keys.get(i));

        cachedUniformCount = keyCount;
    }

    private void snapshotInstanceUBOs() {

        Int2ObjectOpenHashMap<UBOInstance> instanceUBOs = materialInstance.getInstanceUBOs();
        int uboCount = instanceUBOs == null ? 0 : instanceUBOs.size();

        if (cachedInstanceUBOs.length < uboCount)
            cachedInstanceUBOs = new UBOInstance[uboCount];

        cachedInstanceUBOCount = 0;

        if (uboCount > 0)
            instanceUBOs.values().forEach(instanceUBOCollector);
    }

    private void collectInstanceUBO(UBOInstance ubo) {
        cachedInstanceUBOs[cachedInstanceUBOCount++] = ubo;
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

    public int getCachedUniformCount() {
        return cachedUniformCount;
    }

    public UBOInstance[] getCachedInstanceUBOs() {
        return cachedInstanceUBOs;
    }

    public int getCachedInstanceUBOCount() {
        return cachedInstanceUBOCount;
    }

    public MaskStruct getMask() {
        return masked ? mask : null;
    }

    public boolean hasMask() {
        return masked;
    }
}
