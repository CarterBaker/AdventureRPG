package application.bootstrap.renderpipeline.render;

import application.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CompositeBatchStruct extends StructPackage {

    /*
     * Groups all CompositeBufferInstances sharing the same material for one
     * draw pass. The MaterialInstance drives shader and UBO binding; each
     * buffer keeps the material instance it was submitted with, whose own
     * uniforms (a label's color, for one) are pushed before that buffer
     * draws, and the mask it is clipped to, if any. Pooled per material by
     * RenderQueueHandle and reset with the first material submitted each
     * frame. Source UBOs are cached against the MaterialHandle's shared map
     * they come from. Cleared after every draw flush.
     */

    private static final UBOHandle[] EMPTY_UBOS = new UBOHandle[0];

    // Internal
    private MaterialInstance material;
    private final ObjectArrayList<CompositeBufferInstance> buffers;
    private final ObjectArrayList<MaterialInstance> bufferMaterials;
    private final ObjectArrayList<MaskStruct> bufferMasks;

    // Cache
    private Object2ObjectOpenHashMap<String, UBOHandle> cachedSourceMap;
    private UBOHandle[] cachedSourceUBOs;

    // Constructor \\

    public CompositeBatchStruct() {
        this.buffers = new ObjectArrayList<>();
        this.bufferMaterials = new ObjectArrayList<>();
        this.bufferMasks = new ObjectArrayList<>();
    }

    public void reset(MaterialInstance material) {
        this.material = material;
        clear();
    }

    // Management \\

    public void add(CompositeBufferInstance buffer, MaterialInstance bufferMaterial, MaskStruct bufferMask) {
        buffers.add(buffer);
        bufferMaterials.add(bufferMaterial);
        bufferMasks.add(bufferMask);
    }

    public void clear() {
        buffers.clear();
        bufferMaterials.clear();
        bufferMasks.clear();
    }

    public boolean isEmpty() {
        return buffers.isEmpty();
    }

    // Accessible \\

    public MaterialInstance getMaterial() {
        return material;
    }

    public ObjectArrayList<CompositeBufferInstance> getBuffers() {
        return buffers;
    }

    public ObjectArrayList<MaterialInstance> getBufferMaterials() {
        return bufferMaterials;
    }

    public ObjectArrayList<MaskStruct> getBufferMasks() {
        return bufferMasks;
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