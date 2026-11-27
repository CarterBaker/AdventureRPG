package editor.runtime.layoutplanner.render;

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
import editor.runtime.EditorSetting;
import editor.runtime.layoutplanner.LayoutPlannerSetting;
import editor.runtime.layoutplanner.view.LayoutPlannerViewSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class LayoutPlannerRenderSystem extends SystemPackage {

    /*
     * Draws the settlement LayoutPlannerViewSystem plans into this window's
     * scene target where it stands, its low corner at the model origin, with
     * the item viewer material the Ship Viewer draws hulls with, which shades
     * faces as the game's G-buffer receives them and lights them here, never
     * cut away. Every draw takes a model of its own from ItemModelManager.
     */

    // Internal
    private LayoutPlannerViewSystem layoutPlannerViewSystem;
    private ItemModelManager itemModelManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;

    // Render Target
    private FBOInstance sceneFbo;

    // Materials
    private int layoutMaterialID;

    // Placement — the settlement where it stands, and the cutaway boxed once for every draw
    private Matrix4 restMatrix;
    private Float cutawayNone;

    // Base \\

    @Override
    protected void create() {
        this.restMatrix = new Matrix4();
        this.cutawayNone = EditorSetting.ITEM_VIEWER_CUTAWAY_NONE;
    }

    @Override
    protected void get() {
        this.layoutPlannerViewSystem = get(LayoutPlannerViewSystem.class);
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
        this.layoutMaterialID = materialManager.getMaterialIDFromMaterialName(EditorSetting.ITEM_VIEWER_MATERIAL);
        itemModelManager.registerPaintedTwin(
                layoutMaterialID,
                materialManager.getMaterialIDFromMaterialName(EditorSetting.ITEM_VIEWER_PAINTED_MATERIAL));
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();

        pushMeshes(layoutPlannerViewSystem.getMeshes(), window);
        fboRenderSystem.pushFbo(sceneFbo, RuntimeSetting.LAYER_WORLD, window);
    }

    private void pushMeshes(ObjectArrayList<MeshInstance> meshes, WindowInstance window) {

        for (int i = 0; i < meshes.size(); i++) {

            ModelInstance model = itemModelManager.acquireModel(meshes.get(i).getMeshData(), layoutMaterialID);
            model.getMaterial().setUniform(EngineSetting.UNIFORM_ITEM_MODEL, restMatrix);
            model.getMaterial().setUniform(EditorSetting.ITEM_VIEWER_UNIFORM_CUTAWAY_HEIGHT, cutawayNone);
            renderManager.pushRenderCall(model, sceneFbo, LayoutPlannerSetting.DEPTH_LAYOUT, window);
        }
    }
}
