package application.bootstrap.renderpipeline.render;

import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RenderBatchStruct extends StructPackage {

    /*
     * Groups render calls sharing the same material within one depth layer.
     * Pooled per material by RenderQueueHandle and reset with the first
     * material pushed into it each frame. Source UBOs are cached against the
     * MaterialHandle's shared map they come from, so a batch reused for the
     * same material never rebuilds them. The render call list is cleared after
     * every draw flush.
     */

    private static final UBOHandle[] EMPTY_UBOS = new UBOHandle[0];

    // Internal
    private MaterialInstance representativeMaterial;
    private final ObjectArrayList<RenderCallStruct> renderCalls;

    // Cache
    private Object2ObjectOpenHashMap<String, UBOHandle> cachedSourceMap;
    private UBOHandle[] cachedSourceUBOs;

    // Constructor \\

    public RenderBatchStruct() {
        this.renderCalls = new ObjectArrayList<>();
    }

    public void reset(MaterialInstance material) {
        this.representativeMaterial = material;
        this.renderCalls.clear();
    }

    // Management \\

    public void addRenderCall(RenderCallStruct renderCall) {
        renderCalls.add(renderCall);
    }

    public void clear() {
        renderCalls.clear();
    }

    public boolean isEmpty() {
        return renderCalls.isEmpty();
    }

    // Accessible \\

    public MaterialInstance getRepresentativeMaterial() {
        return representativeMaterial;
    }

    public ObjectArrayList<RenderCallStruct> getRenderCalls() {
        return renderCalls;
    }

    public UBOHandle[] getCachedSourceUBOs() {

        Object2ObjectOpenHashMap<String, UBOHandle> sourceUBOs = representativeMaterial.getSourceUBOs();

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
