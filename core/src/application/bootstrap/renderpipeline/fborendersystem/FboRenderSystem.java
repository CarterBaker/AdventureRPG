package application.bootstrap.renderpipeline.fborendersystem;

import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class FboRenderSystem extends SystemPackage {

    private MeshManager meshManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;

    private Object2ObjectOpenHashMap<FboInstance, ModelInstance> fbo2BlitModel;
    private FboInstance[] blitFboBuffer;
    private int[] blitLayerBuffer;
    private int blitCount;

    @Override
    protected void create() {
        this.fbo2BlitModel = new Object2ObjectOpenHashMap<>();
        this.blitFboBuffer = new FboInstance[EngineSetting.MAX_RENDER_CALLS_PER_FRAME];
        this.blitLayerBuffer = new int[EngineSetting.MAX_RENDER_CALLS_PER_FRAME];
        this.blitCount = 0;
    }

    @Override
    protected void get() {
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
        this.renderManager = get(RenderManager.class);
    }

    public void pushFbo(FboInstance fbo, int layer) {
        if (fbo == null || blitCount >= blitFboBuffer.length)
            return;

        blitFboBuffer[blitCount] = fbo;
        blitLayerBuffer[blitCount] = layer;
        blitCount++;
    }

    public void setBlitOverride(FboInstance fbo, MeshData mesh, MaterialInstance material) {
        if (fbo == null)
            return;

        fbo.setBlitOverride(mesh, material);
        fbo2BlitModel.remove(fbo);
    }

    public void pushBlits() {
        if (blitCount == 0)
            return;

        for (int i = 1; i < blitCount; i++) {
            FboInstance fbo = blitFboBuffer[i];
            int layer = blitLayerBuffer[i];
            int j = i - 1;

            while (j >= 0 && blitLayerBuffer[j] > layer) {
                blitFboBuffer[j + 1] = blitFboBuffer[j];
                blitLayerBuffer[j + 1] = blitLayerBuffer[j];
                j--;
            }

            blitFboBuffer[j + 1] = fbo;
            blitLayerBuffer[j + 1] = layer;
        }

        for (int i = 0; i < blitCount; i++) {
            FboInstance fbo = blitFboBuffer[i];
            ModelInstance model = resolveBlitModel(fbo);
            model.getMaterial().setUniform("u_source", fbo.getTextureId());
            renderManager.pushScreenCall(model);
        }

        blitCount = 0;
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
