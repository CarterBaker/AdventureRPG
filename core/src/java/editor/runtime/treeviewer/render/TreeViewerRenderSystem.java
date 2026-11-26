package editor.runtime.treeviewer.render;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.itempipeline.itemmodelmanager.ItemModelManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.treeviewerpipeline.treeviewer.TreePreviewLayerStruct;
import editor.bootstrap.treeviewerpipeline.treeviewermanager.TreeViewerManager;
import editor.runtime.EditorSetting;
import editor.runtime.treeviewer.TreeViewerSetting;
import editor.runtime.treeviewer.layer.TreeViewerLayerSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TreeViewerRenderSystem extends SystemPackage {

    /*
     * Draws the layer the Layers tab shows into this window's scene target,
     * its bark and leaf meshes standing on the origin, with the viewer's bark
     * and leaf materials, which shade the wood and leaves exactly as the
     * game's G-buffer receives them and light them here, as an editor
     * viewport has no lighting pass. Every draw takes a model of its own from
     * ItemModelManager; the meshes are the viewer manager's and shared by
     * every viewer.
     */

    // Internal
    private TreeViewerManager treeViewerManager;
    private TreeViewerLayerSystem treeViewerLayerSystem;
    private ItemModelManager itemModelManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;

    // Render Target
    private FBOInstance sceneFbo;

    // Materials
    private int barkMaterialID;
    private int leafMaterialID;

    // Placement — every layer stands on the origin
    private Matrix4 layerMatrix;

    // Base \\

    @Override
    protected void create() {
        this.layerMatrix = new Matrix4();
    }

    @Override
    protected void get() {
        this.treeViewerManager = get(TreeViewerManager.class);
        this.treeViewerLayerSystem = get(TreeViewerLayerSystem.class);
        this.itemModelManager = get(ItemModelManager.class);
        this.materialManager = get(MaterialManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
    }

    @Override
    protected void awake() {

        this.sceneFbo = fboManager.cloneFbo(EditorSetting.FBO_EDITOR_SCENE, context.getWindow());

        // Materials
        this.barkMaterialID = materialManager.getMaterialIDFromMaterialName(TreeViewerSetting.MATERIAL_BARK);
        this.leafMaterialID = materialManager.getMaterialIDFromMaterialName(TreeViewerSetting.MATERIAL_LEAF);
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();
        int shown = treeViewerLayerSystem.getShownLayer();

        if (shown != EngineSetting.INDEX_NOT_FOUND) {

            TreePreviewLayerStruct layer = treeViewerManager.getPreviewLayer(shown);

            pushMeshes(layer.getBarkMeshes(), barkMaterialID, window);
            pushMeshes(layer.getLeafMeshes(), leafMaterialID, window);
        }

        fboRenderSystem.pushFbo(sceneFbo, RuntimeSetting.LAYER_WORLD, window);
    }

    private void pushMeshes(ObjectArrayList<MeshInstance> meshes, int materialID, WindowInstance window) {

        for (int i = 0; i < meshes.size(); i++) {

            ModelInstance model = itemModelManager.acquireModel(meshes.get(i).getMeshData(), materialID);

            model.getMaterial().setUniform(EngineSetting.UNIFORM_ITEM_MODEL, layerMatrix);
            renderManager.pushRenderCall(model, sceneFbo, TreeViewerSetting.DEPTH_TREE, window);
        }
    }
}
