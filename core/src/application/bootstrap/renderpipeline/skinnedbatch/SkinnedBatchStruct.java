package application.bootstrap.renderpipeline.skinnedbatch;

import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedBufferInstance;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import engine.root.StructPackage;

public class SkinnedBatchStruct extends StructPackage {

    /*
     * Pairs one SkinnedBufferInstance with the MaterialInstance every
     * instance inside it draws with this frame. Source UBOs are lazily
     * cached on first access, same as RenderBatchStruct — owned by the
     * MaterialHandle and never change after bootstrap. A fresh
     * SkinnedBatchStruct is created the first time a given (mesh, material)
     * combination is pushed in a frame — never reused across frames, since
     * the queue it lives in is cleared every rewindFrame().
     */

    private static final UBOHandle[] EMPTY_UBOS = new UBOHandle[0];

    // Internal
    private final SkinnedBufferInstance skinnedBuffer;
    private final MaterialInstance material;

    // Cache
    private UBOHandle[] cachedSourceUBOs;

    // Constructor \\

    public SkinnedBatchStruct(SkinnedBufferInstance skinnedBuffer, MaterialInstance material) {
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