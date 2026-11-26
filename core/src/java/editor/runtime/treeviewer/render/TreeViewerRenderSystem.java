package editor.runtime.treeviewer.render;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.itempipeline.itemmodelmanager.ItemModelManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
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
     * standing on the origin: its bark and leaf meshes with the viewer's bark
     * and leaf materials, which shade the wood and leaves exactly as the
     * game's G-buffer receives them and light them here, as an editor
     * viewport has no lighting pass, or, for a layer the world draws as its
     * impostor, the impostor's quad facing the camera with the viewer's
     * impostor material, from the very arrays the world's instances read.
     * Every draw takes a model of its own from ItemModelManager; the meshes
     * are the viewer manager's and shared by every viewer.
     */

    // Internal
    private TreeViewerManager treeViewerManager;
    private TreeManager treeManager;
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
    private int impostorMaterialID;

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
        this.treeManager = get(TreeManager.class);
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
        this.impostorMaterialID = materialManager.getMaterialIDFromMaterialName(TreeViewerSetting.MATERIAL_IMPOSTOR);
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();
        int shown = treeViewerLayerSystem.getShownLayer();

        if (shown != EngineSetting.INDEX_NOT_FOUND && treeViewerManager.isPreviewImpostorLayer(shown))
            pushImpostor(treeViewerManager.getPreviewImpostorInstance(shown), window);
        else if (shown != EngineSetting.INDEX_NOT_FOUND) {

            TreePreviewLayerStruct layer = treeViewerManager.getPreviewLayer(shown);

            pushMeshes(layer.getBarkMeshes(), barkMaterialID, window);
            pushMeshes(layer.getLeafMeshes(), leafMaterialID, window);
        }

        fboRenderSystem.pushFbo(sceneFbo, RuntimeSetting.LAYER_WORLD, window);
    }

    // The impostor's quad at the sphere an instance names, its frame chosen by the camera as the world's are
    private void pushImpostor(float[] instance, WindowInstance window) {

        ModelInstance model = itemModelManager.acquireModel(treeManager.getImpostorQuad(), impostorMaterialID);

        model.getMaterial().setUniform(
                EngineSetting.UNIFORM_TREE_IMPOSTOR_ALBEDO, treeManager.getImpostorAlbedoArray());
        model.getMaterial().setUniform(
                EngineSetting.UNIFORM_TREE_IMPOSTOR_NORMAL, treeManager.getImpostorNormalArray());
        model.getMaterial().setUniform(
                TreeViewerSetting.UNIFORM_IMPOSTOR_CENTER_Y, instance[EngineSetting.TREE_IMPOSTOR_CENTER_Y_FLOAT]);
        model.getMaterial().setUniform(
                TreeViewerSetting.UNIFORM_IMPOSTOR_RADIUS, instance[EngineSetting.TREE_IMPOSTOR_RADIUS_FLOAT]);
        model.getMaterial().setUniform(
                TreeViewerSetting.UNIFORM_IMPOSTOR_LAYER, instance[EngineSetting.TREE_IMPOSTOR_LAYER_FLOAT]);
        renderManager.pushRenderCall(model, sceneFbo, TreeViewerSetting.DEPTH_TREE, window);
    }

    private void pushMeshes(ObjectArrayList<MeshInstance> meshes, int materialID, WindowInstance window) {

        for (int i = 0; i < meshes.size(); i++) {

            ModelInstance model = itemModelManager.acquireModel(meshes.get(i).getMeshData(), materialID);

            model.getMaterial().setUniform(EngineSetting.UNIFORM_ITEM_MODEL, layerMatrix);
            renderManager.pushRenderCall(model, sceneFbo, TreeViewerSetting.DEPTH_TREE, window);
        }
    }
}
