package editor.bootstrap.dockpipeline.dockgeometrysystem;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.dockpipeline.tab.TabInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector4;

public class DockGeometrySystem extends SystemPackage {

    private static final int FRAME_POOL_SIZE = 32;

    // Dependencies
    private MeshManager meshManager;
    private MaterialManager materialManager;
    private ModelManager modelManager;
    private RenderManager renderManager;

    // Static render data
    private MeshHandle dockQuadMesh;
    private Matrix4 rectTransform;
    private ModelInstance[] framePool;
    private int framePoolIndex;

    @Override
    protected void get() {
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
        this.modelManager = get(ModelManager.class);
        this.renderManager = get(RenderManager.class);
    }

    @Override
    protected void awake() {
        dockQuadMesh = meshManager.getMeshHandleFromMeshName(EngineSetting.DOCK_CHROME_MESH);
        rectTransform = new Matrix4();
        framePool = new ModelInstance[FRAME_POOL_SIZE];

        int materialID = materialManager
                .getMaterialHandleFromMaterialName(EngineSetting.DOCK_CHROME_MATERIAL)
                .getMaterialData().getMaterialID();

        for (int i = 0; i < FRAME_POOL_SIZE; i++) {
            MaterialInstance material = materialManager.cloneMaterial(materialID);
            framePool[i] = modelManager.createModel(dockQuadMesh, material);
        }

        framePoolIndex = 0;
    }

    public void beginFrame() {
        framePoolIndex = 0;
    }

    public void buildTabModel(TabInstance tab) {
        MaterialInstance material = materialManager.cloneMaterial(materialManager
                .getMaterialHandleFromMaterialName(EngineSetting.DOCK_CHROME_MATERIAL)
                .getMaterialData().getMaterialID());
        ModelInstance model = modelManager.createModel(dockQuadMesh, material);
        tab.setTabModel(model);
    }

    public void pushRect(WindowInstance window, TabInstance tab, int x, int y, int width, int height, Vector4 color) {
        if (tab == null || tab.getTabModel() == null)
            return;

        pushRect(window, tab.getTabModel(), x, y, width, height, color);
    }

    public void pushRect(WindowInstance window, ModelInstance model, int x, int y, int width, int height,
            Vector4 color) {
        if (width <= 0 || height <= 0)
            return;

        ModelInstance rectModel = model;

        if (rectModel == null) {
            rectModel = framePool[framePoolIndex % FRAME_POOL_SIZE];
            framePoolIndex++;
        }

        int screenY = window.getHeight() - y - height;

        rectTransform.set(
                width, 0, 0, x,
                0, height, 0, screenY,
                0, 0, 1, 0,
                0, 0, 0, 1);

        rectModel.getMaterial().setUniform("u_transform", rectTransform);
        rectModel.getMaterial().setUniform("u_color", color);

        renderManager.pushScreenCall(rectModel, window, 1);
    }
}