package com.internal.bootstrap.renderpipeline.renderbatch;

import com.internal.bootstrap.renderpipeline.rendercall.RenderCallHandle;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialHandle;
import com.internal.core.engine.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RenderBatchHandle extends HandlePackage {

    // Internal
    private MaterialHandle materialHandle;
    private ObjectArrayList<RenderCallHandle> renderCalls;

    // Internal \\

    public void constructor(MaterialHandle materialHandle) {

        // Internal
        this.materialHandle = materialHandle;
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
        this.materialHandle = null;
    }

    // Accessible \\

    public MaterialHandle getMaterial() {
        return materialHandle;
    }

    public ObjectArrayList<RenderCallHandle> getRenderCalls() {
        return renderCalls;
    }
}