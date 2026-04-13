package application.bootstrap.renderpipeline.renderbatch;

import application.bootstrap.renderpipeline.rendercall.RenderCallStruct;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.core.engine.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RenderBatchStruct extends StructPackage {

    /*
     * Groups render calls sharing the same material within one depth layer.
     * Created on demand by RenderManager the first time a material/depth combo
     * is encountered. Source UBOs are lazily cached on first access — they are
     * owned by the MaterialHandle and never change after bootstrap.
     * The render call list is cleared after every draw flush.
     */

    private static final UBOHandle[] EMPTY_UBOS = new UBOHandle[0];

    // Internal
    private final MaterialInstance representativeMaterial;
    private final ObjectArrayList<RenderCallStruct> renderCalls;

    // Cache
    private UBOHandle[] cachedSourceUBOs;

    // Constructor \\

    public RenderBatchStruct(MaterialInstance material) {
        this.representativeMaterial = material;
        this.renderCalls = new ObjectArrayList<>();
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

    /*
     * Lazily resolved on first call — source UBOs are owned by the
     * MaterialHandle and never change after bootstrap.
     */
    public UBOHandle[] getCachedSourceUBOs() {

        if (cachedSourceUBOs != null)
            return cachedSourceUBOs;

        var sourceUBOs = representativeMaterial.getSourceUBOs();

        if (sourceUBOs == null || sourceUBOs.isEmpty())
            cachedSourceUBOs = EMPTY_UBOS;
        else
            cachedSourceUBOs = sourceUBOs.values().toArray(new UBOHandle[0]);

        return cachedSourceUBOs;
    }
}