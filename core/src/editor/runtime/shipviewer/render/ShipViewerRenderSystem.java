package editor.runtime.shipviewer.render;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.itempipeline.itemmodelmanager.ItemModelManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.vehiclepipeline.util.VehicleSpaceUtility;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
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
     * Draws the shown vehicle into this window's scene target at rest in its
     * own model space: its hull and every moving part but the anchor cables,
     * which hang only from a ship at anchor, with the viewer's material,
     * which shades the faces exactly as the game's G-buffer receives them
     * and lights them here, as an editor viewport has no lighting pass. The
     * meshes are the palette's own, so nothing is meshed for the viewer, and
     * every draw takes a model of its own from ItemModelManager.
     */

    // Internal
    private ShipViewerSystem shipViewerSystem;
    private ItemModelManager itemModelManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;

    // Render Target
    private FBOInstance sceneFbo;

    // Materials
    private int shipMaterialID;

    // Placement — the model grid drawn where it stands, every part at rest
    private Matrix4 restMatrix;

    // Base \\

    @Override
    protected void create() {
        this.restMatrix = new Matrix4();
    }

    @Override
    protected void get() {
        this.shipViewerSystem = get(ShipViewerSystem.class);
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
        this.shipMaterialID = materialManager.getMaterialIDFromMaterialName(ShipViewerSetting.MATERIAL_SHIP);
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();
        VehicleHandle vehicleHandle = shipViewerSystem.getVehicleHandle();

        if (vehicleHandle != null)
            pushVehicle(vehicleHandle, window);

        fboRenderSystem.pushFbo(sceneFbo, RuntimeSetting.LAYER_WORLD, window);
    }

    private void pushVehicle(VehicleHandle vehicleHandle, WindowInstance window) {

        pushMeshes(vehicleHandle.getHullMeshes(), window);

        for (int partIndex = 0; partIndex < vehicleHandle.getPartCount(); partIndex++) {

            VehiclePartStruct part = vehicleHandle.getPart(partIndex);

            if (!part.getRole().isStatic() && VehicleSpaceUtility.isPartShownUnderWay(part))
                pushMeshes(part.getMeshes(), window);
        }
    }

    private void pushMeshes(ObjectArrayList<MeshInstance> meshes, WindowInstance window) {

        for (int i = 0; i < meshes.size(); i++) {

            ModelInstance model = itemModelManager.acquireModel(meshes.get(i).getMeshData(), shipMaterialID);

            model.getMaterial().setUniform(EngineSetting.UNIFORM_ITEM_MODEL, restMatrix);
            renderManager.pushRenderCall(model, sceneFbo, ShipViewerSetting.DEPTH_SHIP, window);
        }
    }
}
