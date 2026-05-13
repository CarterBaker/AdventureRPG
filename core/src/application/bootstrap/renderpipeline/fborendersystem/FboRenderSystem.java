package application.bootstrap.renderpipeline.fborendersystem;

import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.renderpipeline.fbo.FBODestinationStruct;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class FboRenderSystem extends SystemPackage {

    /*
     * Manages screen-space FBO compositing. Queues blits per OS window and
     * flushes them sorted by composite sort key during the draw phase.
     * Logical windows (tabs) are transparently redirected to their composite
     * target OS window and rect — callers never need to know the difference.
     *
     * Sort key = windowDepth * LAYER_STRIDE + layer.
     * Window depth is the primary draw order: editor(0) < tabs(1) < content(2).
     * Layer is secondary — dozens of layers per window sort cleanly within each
     * band.
     * LAYER_STRIDE of 10,000 gives each window depth 10,000 layer slots.
     *
     * Screen order (before/after composite pass) is a separate concern from
     * draw order. It is passed explicitly at push time; default is 1 (after
     * composite, on top of game content). Window depth does NOT bleed into
     * screen order — those are two different axes.
     *
     * Null dest rect means fullscreen — the shader handles both cases identically.
     */

    private static final int LAYER_STRIDE = 10_000;

    private MeshManager meshManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;

    private Object2ObjectOpenHashMap<FboInstance, ModelInstance> fbo2BlitModel;
    private Object2ObjectOpenHashMap<WindowInstance, ObjectArrayList<FboInstance>> window2BlitQueue;

    // Scratch — avoids allocation per blit per frame
    private final Vector4 destRectScratch = new Vector4();

    @Override
    protected void create() {
        fbo2BlitModel = new Object2ObjectOpenHashMap<>();
        window2BlitQueue = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        meshManager = get(MeshManager.class);
        materialManager = get(MaterialManager.class);
        renderManager = get(RenderManager.class);
    }

    // Accessible \\

    public void pushFbo(FboInstance fbo, int layer, WindowInstance window) {
        pushFbo(fbo, layer, window, null, 1);
    }

    public void pushFbo(FboInstance fbo, int layer, WindowInstance window, FBODestinationStruct destRect) {
        pushFbo(fbo, layer, window, destRect, 1);
    }

    public void pushFbo(FboInstance fbo, int layer, WindowInstance window, int screenOrder) {
        pushFbo(fbo, layer, window, null, screenOrder);
    }

    public void pushFbo(FboInstance fbo, int layer, WindowInstance window, FBODestinationStruct destRect,
            int screenOrder) {

        if (fbo == null || window == null)
            return;

        // Logical windows have no composite rect until the layout system fires
        // the first valid resize. Pushing before that would resolve to a null
        // dest rect and blit the FBO full-screen, overwriting the entire OS
        // window on the opening frame.
        if (window.hasCompositeTarget() && !window.hasCompositeRect())
            return;

        WindowInstance target = window.hasCompositeTarget() ? window.getCompositeTarget() : window;

        ObjectArrayList<FboInstance> queue = window2BlitQueue.get(target);
        if (queue == null) {
            queue = new ObjectArrayList<>();
            window2BlitQueue.put(target, queue);
        }

        if (queue.size() >= EngineSetting.MAX_RENDER_CALLS_PER_FRAME)
            return;

        // Sort key: window depth is primary, layer is secondary within each window.
        // Separating sort key from screen order is intentional — they are different
        // concerns. Window depth tells us draw order relative to other windows.
        // Screen order tells us where this blit lands relative to the composite pass.
        int sortKey = window.getDepth() * LAYER_STRIDE + layer;

        fbo.setPushData(window, layer, sortKey, screenOrder, resolveDestRect(window, destRect));
        queue.add(fbo);
    }

    public void setBlitOverride(FboInstance fbo, MeshData mesh, MaterialInstance material) {
        if (fbo == null)
            return;

        fbo.setBlitOverride(mesh, material);
        fbo2BlitModel.remove(fbo);
    }

    public void pushBlits(WindowInstance window) {
        ObjectArrayList<FboInstance> queue = window2BlitQueue.get(window);

        if (queue == null || queue.isEmpty())
            return;

        // Insertion sort by composite sort key.
        // editor(depth=0, any layer) always before tab(depth=1, any layer)
        // before content(depth=2, any layer). Within each window, layers
        // sort low-to-high. Dozens of layers per window are handled cleanly
        // because LAYER_STRIDE gives each depth band 10,000 slots.
        for (int i = 1; i < queue.size(); i++) {
            FboInstance entry = queue.get(i);
            int entryKey = entry.getPushSortKey();
            int j = i - 1;

            while (j >= 0 && queue.get(j).getPushSortKey() > entryKey) {
                queue.set(j + 1, queue.get(j));
                j--;
            }

            queue.set(j + 1, entry);
        }

        for (int i = 0; i < queue.size(); i++) {
            FboInstance fbo = queue.get(i);
            ModelInstance model = resolveBlitModel(fbo);
            model.getMaterial().setUniform("u_source", fbo.getTextureId());

            FBODestinationStruct destRect = fbo.getPushDestRect();
            if (destRect != null)
                destRectScratch.set(destRect.x, destRect.y, destRect.width, destRect.height);
            else
                destRectScratch.set(-1f, -1f, -1f, -1f);

            model.getMaterial().setUniform("u_destRect", destRectScratch);

            // screenOrder is explicit — not derived from window depth.
            // All blits default to order 1 (after composite). Callers that
            // need a blit before the composite pass pass screenOrder = 0.
            renderManager.pushScreenCall(model, window, fbo.getPushScreenOrder());
        }

        queue.clear();
    }

    public void removeWindowResources(WindowInstance window) {
        window2BlitQueue.remove(window);
    }

    // Internal \\

    private FBODestinationStruct resolveDestRect(WindowInstance window, FBODestinationStruct destRect) {
        if (destRect != null)
            return destRect;

        if (window.hasCompositeTarget() && window.hasCompositeRect())
            return new FBODestinationStruct(
                    window.getCompositeX(), window.getCompositeY(),
                    window.getCompositeW(), window.getCompositeH());

        return null;
    }

    private ModelInstance resolveBlitModel(FboInstance fbo) {
        ModelInstance model = fbo2BlitModel.get(fbo);

        if (model != null)
            return model;

        MeshData meshData = fbo.getBlitMeshOverride();
        if (meshData == null)
            meshData = meshManager.getMeshHandleFromMeshName(EngineSetting.DEFAULT_BLIT_MESH).getMeshData();

        MaterialInstance material = fbo.getBlitMaterialOverride();
        if (material == null)
            material = materialManager.cloneMaterial(EngineSetting.DEFAULT_BLIT_MATERIAL);

        model = create(ModelInstance.class);
        model.constructor(meshData, material);

        fbo2BlitModel.put(fbo, model);

        return model;
    }
}