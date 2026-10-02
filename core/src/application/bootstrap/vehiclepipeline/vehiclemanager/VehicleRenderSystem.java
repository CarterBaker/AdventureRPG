package application.bootstrap.vehiclepipeline.vehiclemanager;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemmodelmanager.ItemModelManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.vehiclepipeline.util.VehicleSpaceUtility;
import application.bootstrap.vehiclepipeline.vehicle.VehicleCargoInstance;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.quaternions.Quaternion;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class VehicleRenderSystem extends SystemPackage {

    /*
     * Draws every vehicle near a viewer into a window's world target. Its hull
     * and parts draw with the vehicle material, which repeats each part's
     * texture once per block across faces merged over many blocks, and its
     * cargo with the item material worn gear uses; both write the same
     * G-buffer and are lit like everything else. A vehicle is placed relative
     * to the viewer's chunk: its hull meshes through its own transform, each
     * moving part through that transform and its own pose, an anchor only
     * while it is aboard, and every cargo item through the vehicle's transform
     * and its own placement on the vehicle's grid, save a container standing
     * open, which the inventory draws open where it lies. Every draw takes a
     * model of its own from ItemModelManager.
     */

    // Internal
    private RenderManager renderManager;
    private MaterialManager materialManager;
    private ItemModelManager itemModelManager;
    private VehicleManager vehicleManager;
    private VehicleCargoSystem vehicleCargoSystem;

    // Material
    private int vehicleMaterialID;
    private int equipmentMaterialID;

    // Scratch
    private Matrix4 vehicleMatrix;
    private Matrix4 partMatrix;
    private Matrix4 turnMatrix;
    private Matrix4 drawMatrix;
    private Quaternion turnScratch;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.vehicleMatrix = new Matrix4();
        this.partMatrix = new Matrix4();
        this.turnMatrix = new Matrix4();
        this.drawMatrix = new Matrix4();
        this.turnScratch = new Quaternion();
    }

    @Override
    protected void get() {
        this.renderManager = get(RenderManager.class);
        this.materialManager = get(MaterialManager.class);
        this.itemModelManager = get(ItemModelManager.class);
        this.vehicleManager = get(VehicleManager.class);
        this.vehicleCargoSystem = get(VehicleCargoSystem.class);
    }

    @Override
    protected void awake() {

        // Material
        this.vehicleMaterialID = materialManager.getMaterialIDFromMaterialName(EngineSetting.VEHICLE_ITEM_MATERIAL);
        this.equipmentMaterialID = materialManager.getMaterialIDFromMaterialName(
                EngineSetting.EQUIPMENT_ITEM_MATERIAL);
    }

    // Render \\

    public void pushVehicles(
            WorldHandle worldHandle,
            long viewerChunkCoordinate,
            FBOInstance targetFbo,
            WindowInstance window) {

        ObjectArrayList<VehicleInstance> vehicles = vehicleManager.getVehicles();

        for (int i = 0; i < vehicles.size(); i++) {

            VehicleInstance vehicle = vehicles.get(i);

            if (vehicle.getWorldHandle() != worldHandle || !VehicleSpaceUtility.composeModelMatrix(
                    vehicle, viewerChunkCoordinate, EngineSetting.VEHICLE_RENDER_CHUNK_RADIUS, vehicleMatrix))
                continue;

            pushHull(vehicle, targetFbo, window);
            pushParts(vehicle, targetFbo, window);
            pushCargo(vehicle, targetFbo, window);
        }
    }

    private void pushHull(VehicleInstance vehicle, FBOInstance targetFbo, WindowInstance window) {

        ObjectArrayList<MeshInstance> hullMeshes = vehicle.getVehicleHandle().getHullMeshes();

        for (int i = 0; i < hullMeshes.size(); i++)
            pushMesh(hullMeshes.get(i), vehicleMaterialID, vehicleMatrix, targetFbo, window);
    }

    private void pushParts(VehicleInstance vehicle, FBOInstance targetFbo, WindowInstance window) {

        VehicleHandle vehicleHandle = vehicle.getVehicleHandle();

        for (int partIndex = 0; partIndex < vehicleHandle.getPartCount(); partIndex++) {

            VehiclePartStruct part = vehicleHandle.getPart(partIndex);

            if (part.getRole().isStatic() || !VehicleSpaceUtility.isPartShown(vehicle, part))
                continue;

            VehicleSpaceUtility.composePartMatrix(vehicle, part, turnScratch, turnMatrix, partMatrix);
            drawMatrix.set(vehicleMatrix).multiply(partMatrix);

            ObjectArrayList<MeshInstance> meshes = part.getMeshes();

            for (int i = 0; i < meshes.size(); i++)
                pushMesh(meshes.get(i), vehicleMaterialID, drawMatrix, targetFbo, window);
        }
    }

    // Every closed cargo item placed by the vehicle's transform and its own corner and turn on the vehicle's grid
    private void pushCargo(VehicleInstance vehicle, FBOInstance targetFbo, WindowInstance window) {

        ObjectArrayList<VehicleCargoInstance> cargo = vehicle.getCargo();

        for (int i = 0; i < cargo.size(); i++) {

            VehicleCargoInstance item = cargo.get(i);

            if (item.isOpen())
                continue;

            ItemDefinitionHandle definition = item.getItemInstance().getItemDefinitionHandle();

            drawMatrix.set(vehicleMatrix).multiply(vehicleCargoSystem.composeCargoMatrix(item, partMatrix));

            ModelInstance model = itemModelManager.acquireModel(definition.getMeshHandle(), equipmentMaterialID);

            model.getMaterial().setUniform(EngineSetting.UNIFORM_ITEM_MODEL, drawMatrix);
            renderManager.pushRenderCall(model, targetFbo, EngineSetting.EQUIPMENT_RENDER_DEPTH, window);
        }
    }

    // Draw \\

    private void pushMesh(
            MeshInstance mesh,
            int materialID,
            Matrix4 transform,
            FBOInstance targetFbo,
            WindowInstance window) {

        ModelInstance model = itemModelManager.acquireModel(mesh.getMeshData(), materialID);

        model.getMaterial().setUniform(EngineSetting.UNIFORM_ITEM_MODEL, transform);
        renderManager.pushRenderCall(model, targetFbo, EngineSetting.EQUIPMENT_RENDER_DEPTH, window);
    }
}
