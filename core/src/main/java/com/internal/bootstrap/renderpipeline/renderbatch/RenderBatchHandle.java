package com.internal.bootstrap.renderpipeline.renderbatch;

import com.internal.bootstrap.renderpipeline.rendercall.RenderCallHandle;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.core.engine.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Batches all render calls sharing the same source MaterialHandle within a
 * single depth layer. The representative MaterialInstance is the first one
 * registered — its source UBOs are shared by all calls in the batch, so it
 * drives shader binding and source UBO binding. Source UBOs are cached on
 * first access so the draw loop never touches a live collection. Cleared after
 * each draw flush — never holds calls across frames.
 */
public class RenderBatchHandle extends HandlePackage {

    private static final UBOHandle[] EMPTY_UBOS = new UBOHandle[0];

    // Internal
    private MaterialInstance representativeMaterial;
    private UBOHandle[] cachedSourceUBOs;
    private ObjectArrayList<RenderCallHandle> renderCalls;
    // Internal \\

    public void constructor(MaterialInstance material) {
        this.representativeMaterial = material;
        this.renderCalls = new ObjectArrayList<>();
    }

    // Utility \\

    public void addRenderCall(RenderCallHandle renderCall) {
        renderCalls.add(renderCall);
    }

    public void clear() {
        renderCalls.clear();
    }

    public boolean isEmpty() {
        return renderCalls.isEmpty();
    }

    public void dispose() {
        renderCalls.clear();
        this.representativeMaterial = null;
        this.cachedSourceUBOs = null;
    }

    // Accessible \\

    /*
     * Returns the array of source UBOs for this batch, cached on first call.
     * Safe to cache permanently — source UBOs are owned by the MaterialHandle
     * and do not change after bootstrap.
     */
    public UBOHandle[] getCachedSourceUBOs() {
        if (cachedSourceUBOs != null)
            return cachedSourceUBOs;
        var sourceUBOs = representativeMaterial.getSourceUBOs();
        if (sourceUBOs == null || sourceUBOs.isEmpty()) {
            cachedSourceUBOs = EMPTY_UBOS;
        } else {
            cachedSourceUBOs = sourceUBOs.values().toArray(new UBOHandle[0]);
        }
        return cachedSourceUBOs;
    }

    public MaterialInstance getRepresentativeMaterial() {
        return representativeMaterial;
    }

    public ObjectArrayList<RenderCallHandle> getRenderCalls() {
        return renderCalls;
    }
}