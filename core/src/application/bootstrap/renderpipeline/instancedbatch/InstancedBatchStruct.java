package application.bootstrap.renderpipeline.instancedbatch;

import application.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import engine.root.StructPackage;

public class InstancedBatchStruct extends StructPackage {

    /*
     * Pairs one generic world-space instanced CompositeBufferInstance with
     * the MaterialInstance every instance inside it draws with this frame —
     * the depth-tested counterpart to SkinnedBatchStruct, used for things
     * like physical weather clouds rather than skinned characters. Source
     * UBOs are lazily cached on first access, owned by the MaterialHandle
     * and never change after bootstrap. A fresh instance is created the
     * first time a given buffer is pushed in a frame — never reused across
     * frames, since the queue it lives in is cleared every rewindFrame().
     */

    private static final UBOHandle[] EMPTY_UBOS = new UBOHandle[0];

    private final CompositeBufferInstance buffer;
    private final MaterialInstance material;

    private UBOHandle[] cachedSourceUBOs;

    public InstancedBatchStruct(CompositeBufferInstance buffer, MaterialInstance material) {
        this.buffer = buffer;
        this.material = material;
    }

    public CompositeBufferInstance getBuffer() {
        return buffer;
    }

    public MaterialInstance getMaterial() {
        return material;
    }

    public UBOHandle[] getCachedSourceUBOs() {

        if (cachedSourceUBOs != null)
            return cachedSourceUBOs;

        var sourceUBOs = material.getSourceUBOs();

        if (sourceUBOs == null || sourceUBOs.isEmpty())
            cachedSourceUBOs = EMPTY_UBOS;
        else
            cachedSourceUBOs = sourceUBOs.values().toArray(new UBOHandle[0]);

        return cachedSourceUBOs;
    }
}