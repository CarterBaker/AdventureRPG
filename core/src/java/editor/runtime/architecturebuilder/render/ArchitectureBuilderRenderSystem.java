package editor.runtime.architecturebuilder.render;

import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
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
import editor.runtime.architecturebuilder.ArchitectureBuilderSetting;
import editor.runtime.architecturebuilder.view.ArchitectureBuilderViewSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ArchitectureBuilderRenderSystem extends SystemPackage {

    /*
     * Draws what the Architecture Builder shows into this window's scene
     * target: every item ArchitectureBuilderViewSystem lays out, standing
     * unturned at its corner, with the item viewer material the Ship Viewer
     * draws its cargo with, which shades faces as the game's G-buffer
     * receives them and lights them here, never cut away. Nothing is meshed
     * for the builder, and every draw takes a model of its own from
     * ItemModelManager.
     */

    // Internal
    private ArchitectureBuilderViewSystem architectureBuilderViewSystem;
    private ItemModelManager itemModelManager;
    private ItemRotationBufferSystem itemRotationBufferSystem;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;

    // Render Target
    private FBOInstance sceneFbo;

    // Materials
    private int itemMaterialID;

    // Placement — one item's model grid where it stands, and the cutaway boxed once for every draw
    private Matrix4 itemMatrix;
    private Float cutawayNone;

    // Base \\

    @Override
    protected void create() {
        this.itemMatrix = new Matrix4();
        this.cutawayNone = EditorSetting.ITEM_VIEWER_CUTAWAY_NONE;
    }

    @Override
    protected void get() {
        this.architectureBuilderViewSystem = get(ArchitectureBuilderViewSystem.class);
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
        this.itemMaterialID = materialManager.getMaterialIDFromMaterialName(EditorSetting.ITEM_VIEWER_MATERIAL);
        itemModelManager.registerPaintedTwin(
                itemMaterialID,
                materialManager.getMaterialIDFromMaterialName(EditorSetting.ITEM_VIEWER_PAINTED_MATERIAL));
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();

        pushItems(architectureBuilderViewSystem.getItems(), window);
        fboRenderSystem.pushFbo(sceneFbo, RuntimeSetting.LAYER_WORLD, window);
    }

    // Every item laid out, unturned at its corner
    private void pushItems(ObjectArrayList<ItemDefinitionHandle> items, WindowInstance window) {

        for (int i = 0; i < items.size(); i++) {

            itemRotationBufferSystem.composeTransform(
                    architectureBuilderViewSystem.getCornerX(i),
                    architectureBuilderViewSystem.getCornerY(i),
                    0f,
                    EngineSetting.DEFAULT_BLOCK_ORIENTATION,
                    itemMatrix);

            ModelInstance model = itemModelManager.acquireModel(items.get(i).getMeshHandle(), itemMaterialID);
            model.getMaterial().setUniform(EngineSetting.UNIFORM_ITEM_MODEL, itemMatrix);
            model.getMaterial().setUniform(EditorSetting.ITEM_VIEWER_UNIFORM_CUTAWAY_HEIGHT, cutawayNone);
            renderManager.pushRenderCall(model, sceneFbo, ArchitectureBuilderSetting.DEPTH_PREVIEW, window);
        }
    }
}
