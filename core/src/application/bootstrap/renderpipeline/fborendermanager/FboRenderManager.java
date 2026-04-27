package application.bootstrap.renderpipeline.fborendermanager;

import application.bootstrap.geometrypipeline.mesh.MeshData;
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

    private Object2ObjectOpenHashMap<FboInstance, ModelInstance> fbo2BlitModel;
    private ObjectArrayList<FboInstance> blitQueue;

    @Override
    protected void create() {
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

    public void setBlitOverride(FboInstance fbo, MeshData mesh, MaterialInstance material) {
        if (fbo == null)
            return;

        fbo.setBlitOverride(mesh, material);
        fbo2BlitModel.remove(fbo);
    }

    public void pushBlits() {
        if (blitQueue.isEmpty())
            return;

        Object[] elements = blitQueue.elements();
        int count = blitQueue.size();

        for (int i = 0; i < count; i++) {
            FboInstance fbo = (FboInstance) elements[i];
            ModelInstance model = resolveBlitModel(fbo);
            model.getMaterial().setUniform("u_source", fbo.getTextureId());
            renderManager.pushScreenCall(model);
        }

        blitQueue.clear();
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
