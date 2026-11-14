package editor.runtime.shipviewer.render;

import application.bootstrap.furnishingpipeline.furnishing.FurnishingSlotStruct;
import application.bootstrap.furnishingpipeline.furnishingmanager.FurnishingManager;
import application.bootstrap.geometrypipeline.mesh.MeshInstance;
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
import editor.runtime.shipviewer.ShipViewerSetting;
import editor.runtime.shipviewer.view.ShipViewerSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShipViewerRenderSystem extends SystemPackage {

    /*
     * Draws the shown ship into this window's scene target at rest in its own
     * model space: every mesh ShipViewerSystem lays out with the viewer's
     * material, which shades the faces exactly as the game's G-buffer
     * receives them and lights them here, as an editor viewport has no
     * lighting pass, and cuts away everything above the deck shown. Every
     * cargo slot is drawn as the first item its furnishing offers, turned and
     * placed in the slot with the same material, which places and textures an
     * item's mesh as it does the ship's, never cut in the item's own space,
     * and passed over where the cutaway takes its slot. Nothing is meshed for the viewer, and every draw
     * takes a model of its own from ItemModelManager.
     */

    // Internal
    private ShipViewerSystem shipViewerSystem;
    private ItemModelManager itemModelManager;
    private ItemRotationBufferSystem itemRotationBufferSystem;
    private FurnishingManager furnishingManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;

    // Render Target
    private FBOInstance sceneFbo;

    // Materials
    private int shipMaterialID;

    // Placement — the model grid drawn where it stands, every part at rest, and one cargo item's place
    private Matrix4 restMatrix;
    private Matrix4 itemMatrix;

    // Cutaway — the height in model blocks everything above is cut away at, boxed again only when it moves; an item's
    // never cuts
    private float cutawayHeight;
    private Float shownCutaway;
    private Float itemCutaway;

    // Base \\

    @Override
    protected void create() {
        this.restMatrix = new Matrix4();
        this.itemMatrix = new Matrix4();
        this.cutawayHeight = ShipViewerSetting.CUTAWAY_NONE;
        this.shownCutaway = cutawayHeight;
        this.itemCutaway = ShipViewerSetting.CUTAWAY_NONE;
    }

    @Override
    protected void get() {
        this.shipViewerSystem = get(ShipViewerSystem.class);
        this.itemModelManager = get(ItemModelManager.class);
        this.itemRotationBufferSystem = get(ItemRotationBufferSystem.class);
        this.furnishingManager = get(FurnishingManager.class);
        this.materialManager = get(MaterialManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
    }

    @Override
    protected void awake() {

        this.sceneFbo = fboManager.cloneFbo(EditorSetting.FBO_EDITOR_SCENE, context.getWindow());

        // Materials
        this.shipMaterialID = materialManager.getMaterialIDFromMaterialName(ShipViewerSetting.MATERIAL_SHIP);
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();

        if (shipViewerSystem.isShown()) {
            resolveCutaway();
            pushMeshes(shipViewerSystem.getMeshes(), window);
            pushCargo(shipViewerSystem.getCargo(), window);
        }

        fboRenderSystem.pushFbo(sceneFbo, RuntimeSetting.LAYER_WORLD, window);
    }

    // The cutaway's height taken from the deck shown, boxed for the material only when it moves
    private void resolveCutaway() {

        float height = shipViewerSystem.getCutawayHeight();

        if (height == cutawayHeight)
            return;

        this.cutawayHeight = height;
        this.shownCutaway = height;
    }

    private void pushMeshes(ObjectArrayList<MeshInstance> meshes, WindowInstance window) {

        for (int i = 0; i < meshes.size(); i++) {

            ModelInstance model = itemModelManager.acquireModel(meshes.get(i).getMeshData(), shipMaterialID);

            model.getMaterial().setUniform(EngineSetting.UNIFORM_ITEM_MODEL, restMatrix);
            model.getMaterial().setUniform(ShipViewerSetting.UNIFORM_CUTAWAY_HEIGHT, shownCutaway);
            renderManager.pushRenderCall(model, sceneFbo, ShipViewerSetting.DEPTH_SHIP, window);
        }
    }

    // Every cargo slot under the cutaway drawn as the first item its furnishing offers, turned and placed in the slot
    private void pushCargo(ObjectArrayList<FurnishingSlotStruct> cargo, WindowInstance window) {

        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;

        for (int i = 0; i < cargo.size(); i++) {

            FurnishingSlotStruct slot = cargo.get(i);

            if (slot.getCornerY() / resolution >= cutawayHeight)
                continue;

            ItemDefinitionHandle item = furnishingManager.getFurnishingHandleFromFurnishingID(slot.getFurnishingID())
                    .getItems()[0];

            itemRotationBufferSystem.composeTransform(
                    slot.getCornerX() / resolution,
                    slot.getCornerY() / resolution,
                    slot.getCornerZ() / resolution,
                    slot.getOrientation(),
                    itemMatrix);

            ModelInstance model = itemModelManager.acquireModel(item.getMeshHandle(), shipMaterialID);

            model.getMaterial().setUniform(EngineSetting.UNIFORM_ITEM_MODEL, itemMatrix);
            model.getMaterial().setUniform(ShipViewerSetting.UNIFORM_CUTAWAY_HEIGHT, itemCutaway);
            renderManager.pushRenderCall(model, sceneFbo, ShipViewerSetting.DEPTH_SHIP, window);
        }
    }
}
