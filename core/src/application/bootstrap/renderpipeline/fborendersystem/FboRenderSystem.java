package application.bootstrap.renderpipeline.fborendersystem;

import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class FboRenderSystem extends SystemPackage {

    private MeshManager meshManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;

    private Object2ObjectOpenHashMap<FboInstance, ModelInstance> fbo2BlitModel;
    private Object2ObjectOpenHashMap<WindowInstance, ObjectArrayList<FboLayerStruct>> window2BlitQueue;

    @Override
    protected void create() {
        this.fbo2BlitModel = new Object2ObjectOpenHashMap<>();
        this.window2BlitQueue = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
        this.renderManager = get(RenderManager.class);
    }

    public void pushFbo(FboInstance fbo, int layer, WindowInstance window) {
        pushFbo(fbo, layer, window, null);
    }

    public void pushFbo(FboInstance fbo, int layer, WindowInstance window, DestRectStruct destRect) {
        if (fbo == null || window == null)
            return;

        ObjectArrayList<FboLayerStruct> queue = window2BlitQueue.get(window);
        if (queue == null) {
            queue = new ObjectArrayList<>();
            window2BlitQueue.put(window, queue);
        }

        if (queue.size() >= EngineSetting.MAX_RENDER_CALLS_PER_FRAME)
            return;

        queue.add(new FboLayerStruct(fbo, layer, destRect));
    }

    public void setBlitOverride(FboInstance fbo, MeshData mesh, MaterialInstance material) {
        if (fbo == null)
            return;

        fbo.setBlitOverride(mesh, material);
        fbo2BlitModel.remove(fbo);
    }

    public void pushBlits(WindowInstance window) {
        ObjectArrayList<FboLayerStruct> queue = window2BlitQueue.get(window);

        if (queue == null || queue.isEmpty())
            return;

        for (int i = 1; i < queue.size(); i++) {
            FboLayerStruct entry = queue.get(i);
            int j = i - 1;

            while (j >= 0 && queue.get(j).layer > entry.layer) {
                queue.set(j + 1, queue.get(j));
                j--;
            }

            queue.set(j + 1, entry);
        }

        for (int i = 0; i < queue.size(); i++) {
            FboInstance fbo = queue.get(i).fbo;
            ModelInstance model = resolveBlitModel(fbo);
            model.getMaterial().setUniform("u_source", fbo.getTextureId());

            DestRectStruct destRect = queue.get(i).destRect;
            if (destRect != null)
                model.getMaterial().setUniform("u_destRect", new float[] {
                        destRect.x,
                        destRect.y,
                        destRect.width,
                        destRect.height
                });
            else
                model.getMaterial().setUniform("u_destRect", new float[] { -1f, -1f, -1f, -1f });
            renderManager.pushScreenCall(model, window);
        }

        queue.clear();
    }

    public void removeWindowResources(WindowInstance window) {
        window2BlitQueue.remove(window);
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

    public static class DestRectStruct {
        public final float x;
        public final float y;
        public final float width;
        public final float height;

        public DestRectStruct(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private static class FboLayerStruct {
        private final FboInstance fbo;
        private final int layer;
        private final DestRectStruct destRect;

        private FboLayerStruct(FboInstance fbo, int layer, DestRectStruct destRect) {
            this.fbo = fbo;
            this.layer = layer;
            this.destRect = destRect;
        }
    }
}
