package application.bootstrap.renderpipeline.render;

import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedBufferInstance;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class SkinnedBatchStruct extends StructPackage {

    /*
     * Pairs one SkinnedBufferInstance with the MaterialInstance every
     * instance inside it draws with this frame. Pooled by RenderQueueHandle
     * and reset the first time a given (mesh, material) combination is pushed
     * in a frame, then returned to the pool at rewindFrame(). Source UBOs are
     * cached against the MaterialHandle's shared map they come from, same as
     * RenderBatchStruct.
     */

    private static final UBOHandle[] EMPTY_UBOS = new UBOHandle[0];

    // Internal
    private SkinnedBufferInstance skinnedBuffer;
    private MaterialInstance material;

    // Cache
    private Object2ObjectOpenHashMap<String, UBOHandle> cachedSourceMap;
    private UBOHandle[] cachedSourceUBOs;

    // Constructor \\

    public void reset(SkinnedBufferInstance skinnedBuffer, MaterialInstance material) {
        this.skinnedBuffer = skinnedBuffer;
        this.material = material;
    }

    // Accessible \\

    public SkinnedBufferInstance getSkinnedBuffer() {
        return skinnedBuffer;
    }

    public MaterialInstance getMaterial() {
        return material;
    }

    public UBOHandle[] getCachedSourceUBOs() {

        Object2ObjectOpenHashMap<String, UBOHandle> sourceUBOs = material.getSourceUBOs();

        if (cachedSourceUBOs != null && sourceUBOs == cachedSourceMap)
            return cachedSourceUBOs;

        cachedSourceMap = sourceUBOs;

        if (sourceUBOs == null || sourceUBOs.isEmpty())
            cachedSourceUBOs = EMPTY_UBOS;
        else
            cachedSourceUBOs = sourceUBOs.values().toArray(new UBOHandle[0]);

        return cachedSourceUBOs;
    }
}