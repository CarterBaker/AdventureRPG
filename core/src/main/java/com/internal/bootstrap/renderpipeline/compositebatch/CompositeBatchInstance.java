package com.internal.bootstrap.renderpipeline.compositebatch;

import com.internal.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.core.engine.InstancePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Groups all CompositeBufferInstances sharing the same material for a single
 * draw pass. The representative MaterialInstance drives shader and UBO binding.
 * Source UBOs cached on first access — safe since they never change after bootstrap.
 * Cleared after each draw flush, never holds submissions across frames.
 */
public class CompositeBatchInstance extends InstancePackage {

    private static final UBOHandle[] EMPTY_UBOS = new UBOHandle[0];

    private MaterialInstance material;
    private ObjectArrayList<CompositeBufferInstance> buffers;
    private UBOHandle[] cachedSourceUBOs;

    // Constructor \\

    public void constructor(MaterialInstance material) {
        this.material = material;
        this.buffers = new ObjectArrayList<>();
    }

    // Management \\

    public void add(CompositeBufferInstance buffer) {
        buffers.add(buffer);
    }

    public void clear() {
        buffers.clear();
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

    public UBOHandle[] getCachedSourceUBOs() {
        if (cachedSourceUBOs != null)
            return cachedSourceUBOs;
        var sourceUBOs = material.getSourceUBOs();
        if (sourceUBOs == null || sourceUBOs.isEmpty()) {
            cachedSourceUBOs = EMPTY_UBOS;
        } else {
            cachedSourceUBOs = sourceUBOs.values().toArray(new UBOHandle[0]);
        }
        return cachedSourceUBOs;
    }
}