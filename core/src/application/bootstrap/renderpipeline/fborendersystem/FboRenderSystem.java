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
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class FboRenderSystem extends SystemPackage {

    private static final int LAYER_STRIDE = 10_000;

    private MeshManager meshManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;

    private Object2ObjectOpenHashMap<FboInstance, ModelInstance> fbo2BlitModel;
    private Object2ObjectOpenHashMap<WindowInstance, ObjectArrayList<FboInstance>> window2BlitQueue;

    // Scratch — avoids allocation per blit per frame
    private final Vector4 destRectScratch = new Vector4();
    private final Vector2 resolutionScratch = new Vector2();

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

        // Resolution is constant for all blits in this window — set once outside the
        // loop
        resolutionScratch.set(window.getWidth(), window.getHeight());

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
            model.getMaterial().setUniform("u_resolution", resolutionScratch);

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