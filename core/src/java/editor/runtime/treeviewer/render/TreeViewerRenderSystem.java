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
import editor.bootstrap.treeplannerpipeline.treeplanner.TreePreviewTierStruct;
import editor.bootstrap.treeplannerpipeline.treeplannermanager.TreePlannerManager;
import editor.runtime.EditorSetting;
import editor.runtime.treeviewer.TreeViewerSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TreeViewerRenderSystem extends SystemPackage {

    /*
     * Draws the Tree Planner's row into this window's scene target: every
     * tier's bark and leaf meshes, each tier carried to its place along the
     * row by a matrix of its own, with the viewer's bark and leaf materials,
     * which shade the wood and leaves exactly as the game's G-buffer receives
     * them and light them here, as an editor viewport has no lighting pass.
     * Every draw takes a model of its own from ItemModelManager; the meshes
     * are the planner's and shared by every viewer.
     */

    // Internal
    private TreePlannerManager treePlannerManager;
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

    // Placement — one matrix per tier, set again only when the row is built again
    private Matrix4[] tierMatrices;
    private int placedRevision;

    // Base \\

    @Override
    protected void create() {
        this.placedRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.treePlannerManager = get(TreePlannerManager.class);
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

        // Placement
        this.tierMatrices = new Matrix4[treePlannerManager.getPreviewTierCount()];

        for (int i = 0; i < tierMatrices.length; i++)
            tierMatrices[i] = new Matrix4();
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();

        placeTiers();

        for (int i = 0; i < tierMatrices.length; i++) {

            TreePreviewTierStruct tier = treePlannerManager.getPreviewTier(i);

            pushMeshes(tier.getBarkMeshes(), barkMaterialID, tierMatrices[i], window);
            pushMeshes(tier.getLeafMeshes(), leafMaterialID, tierMatrices[i], window);
        }

        fboRenderSystem.pushFbo(sceneFbo, RuntimeSetting.LAYER_WORLD, window);
    }

    // Each tier carried along the row to where its root stands
    private void placeTiers() {

        int revision = treePlannerManager.getPreviewRevision();

        if (revision == placedRevision)
            return;

        placedRevision = revision;

        for (int i = 0; i < tierMatrices.length; i++)
            tierMatrices[i].set(
                    1f, 0f, 0f, treePlannerManager.getPreviewTierOffsetX(i),
                    0f, 1f, 0f, 0f,
                    0f, 0f, 1f, 0f,
                    0f, 0f, 0f, 1f);
    }

    private void pushMeshes(
            ObjectArrayList<MeshInstance> meshes,
            int materialID,
            Matrix4 tierMatrix,
            WindowInstance window) {

        for (int i = 0; i < meshes.size(); i++) {

            ModelInstance model = itemModelManager.acquireModel(meshes.get(i).getMeshData(), materialID);

            model.getMaterial().setUniform(EngineSetting.UNIFORM_ITEM_MODEL, tierMatrix);
            renderManager.pushRenderCall(model, sceneFbo, TreeViewerSetting.DEPTH_TREE, window);
        }
    }
}
