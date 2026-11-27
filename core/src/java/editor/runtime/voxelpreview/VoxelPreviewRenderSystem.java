package editor.runtime.voxelpreview;

import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.itempipeline.itemmodelmanager.ItemModelManager;
import application.bootstrap.itempipeline.itemrotationmanager.ItemRotationBufferSystem;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;

public class VoxelPreviewRenderSystem extends SystemPackage {

    /*
     * Draws what the window's VoxelPreviewSystem lays into its scene target:
     * every mesh where it stands, its corner at the model origin, and every
     * item turned and stood at its corner, with the item viewer material the
     * Ship Viewer draws hulls and cargo with, which shades faces as the
     * game's G-buffer receives them and lights them here, never cut away.
     * Every draw takes a model of its own from ItemModelManager.
     */

    // Internal
    private VoxelPreviewSystem voxelPreviewSystem;
    private ItemModelManager itemModelManager;
    private ItemRotationBufferSystem itemRotationBufferSystem;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;

    // Render Target
    private FBOInstance sceneFbo;

    // Materials
    private int previewMaterialID;

    // Placement — the meshes where they stand, one item's place, and the cutaway boxed once for every draw
    private Matrix4 restMatrix;
    private Matrix4 itemMatrix;
    private Float cutawayNone;

    // Base \\

    @Override
    protected void create() {
        this.restMatrix = new Matrix4();
        this.itemMatrix = new Matrix4();
        this.cutawayNone = EditorSetting.ITEM_VIEWER_CUTAWAY_NONE;
    }

    @Override
    protected void get() {
        this.voxelPreviewSystem = get(VoxelPreviewSystem.class);
        this.itemModelManager = get(ItemModelManager.class);
        this.itemRotationBufferSystem = get(ItemRotationBufferSystem.class);
        this.materialManager = get(MaterialManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
    }

    @Override
    protected void awake() {

        this.sceneFbo = fboManager.cloneFbo(EditorSetting.FBO_EDITOR_SCENE, context.getWindow());

        // Materials
        this.previewMaterialID = materialManager.getMaterialIDFromMaterialName(EditorSetting.ITEM_VIEWER_MATERIAL);
        itemModelManager.registerPaintedTwin(
                previewMaterialID,
                materialManager.getMaterialIDFromMaterialName(EditorSetting.ITEM_VIEWER_PAINTED_MATERIAL));
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();

        for (int i = 0; i < voxelPreviewSystem.getMeshes().size(); i++)
            push(itemModelManager.acquireModel(voxelPreviewSystem.getMeshes().get(i).getMeshData(), previewMaterialID),
                    restMatrix, window);

        for (int i = 0; i < voxelPreviewSystem.getItems().size(); i++) {

            itemRotationBufferSystem.composeTransform(
                    voxelPreviewSystem.getItemCorner(i, EngineSetting.AXIS_X),
                    voxelPreviewSystem.getItemCorner(i, EngineSetting.AXIS_Y),
                    voxelPreviewSystem.getItemCorner(i, EngineSetting.AXIS_Z),
                    voxelPreviewSystem.getItemOrientation(i),
                    itemMatrix);
            push(itemModelManager.acquireModel(voxelPreviewSystem.getItems().get(i).getMeshHandle(), previewMaterialID),
                    itemMatrix, window);
        }

        fboRenderSystem.pushFbo(sceneFbo, RuntimeSetting.LAYER_WORLD, window);
    }

    private void push(ModelInstance model, Matrix4 placement, WindowInstance window) {

        model.getMaterial().setUniform(EngineSetting.UNIFORM_ITEM_MODEL, placement);
        model.getMaterial().setUniform(EditorSetting.ITEM_VIEWER_UNIFORM_CUTAWAY_HEIGHT, cutawayNone);
        renderManager.pushRenderCall(model, sceneFbo, EditorSetting.VOXEL_PREVIEW_DEPTH, window);
    }
}
