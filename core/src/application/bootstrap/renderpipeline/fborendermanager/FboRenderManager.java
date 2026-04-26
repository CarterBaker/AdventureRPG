package application.bootstrap.renderpipeline.fborendermanager;

import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class FboRenderManager extends ManagerPackage {

    private MeshManager meshManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;

    private Object2ObjectOpenHashMap<FboInstance, MaterialInstance> fbo2BlitMaterial;
    private Object2ObjectOpenHashMap<FboInstance, ModelInstance> fbo2BlitModel;
    private ObjectArrayList<FboInstance> blitQueue;

    @Override
    protected void create() {
        this.fbo2BlitMaterial = new Object2ObjectOpenHashMap<>();
        this.fbo2BlitModel = new Object2ObjectOpenHashMap<>();
        this.blitQueue = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
        this.renderManager = get(RenderManager.class);
    }

    public void pushFbo(FboInstance fbo) {
        if (fbo == null)
            return;
        blitQueue.add(fbo);
    }

    public void pushBlits() {
        if (blitQueue.isEmpty())
            return;

        Object[] elements = blitQueue.elements();
        int count = blitQueue.size();

        for (int i = 0; i < count; i++) {
            FboInstance fbo = (FboInstance) elements[i];
            MaterialInstance material = resolveBlitMaterial(fbo);
            ModelInstance model = resolveBlitModel(fbo, material);

            material.setUniform("u_source", fbo.getTextureId());
            renderManager.pushScreenCall(model);
        }

        blitQueue.clear();
    }

    private MaterialInstance resolveBlitMaterial(FboInstance fbo) {
        MaterialInstance material = fbo2BlitMaterial.get(fbo);

        if (material != null)
            return material;

        material = materialManager.cloneMaterial(EngineSetting.DEFAULT_BLIT_MATERIAL);
        fbo2BlitMaterial.put(fbo, material);
        return material;
    }

    private ModelInstance resolveBlitModel(FboInstance fbo, MaterialInstance material) {
        ModelInstance model = fbo2BlitModel.get(fbo);

        if (model != null)
            return model;

        model = create(ModelInstance.class);
        model.constructor(
                meshManager.getMeshHandleFromMeshName(EngineSetting.DEFAULT_BLIT_MESH).getMeshData(),
                material);

        fbo2BlitModel.put(fbo, model);
        return model;
    }
}
