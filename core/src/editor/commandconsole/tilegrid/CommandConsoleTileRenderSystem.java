package editor.commandconsole.tilegrid;

import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemmodelmanager.ItemModelManager;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.render.MaskStruct;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.vehiclepipeline.util.VehicleSpaceUtility;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import application.runtime.inventory.InventoryViewUtility;
import editor.commandconsole.CommandConsoleSetting;
import editor.commandconsole.panel.CommandConsolePanelSystem;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CommandConsoleTileRenderSystem extends SystemPackage {

    /*
     * Draws each tile's icon into this window's own target, composited over
     * the command console's menu, items and vehicles alike turned to the same
     * three-quarter view the inventory's slots show. An item draws through
     * the inventory's transforms and item material; a vehicle is fitted to
     * its model grid's bounds and drawn as it sails, every moving part at
     * rest, with a material that repeats each part's texture once per block
     * as the world does. Icons are clipped to where their grid shows inside
     * the command tree, so a scrolled tree never draws past its edges, and
     * every draw takes a model of its own from ItemModelManager.
     */

    // Internal
    private ItemModelManager itemModelManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;
    private CommandConsolePanelSystem commandConsolePanelSystem;
    private CommandConsoleTileGridSystem commandConsoleTileGridSystem;

    // Render Target
    private FBOInstance iconFbo;

    // Resources
    private int itemMaterialID;
    private int vehicleMaterialID;

    // Scratch
    private Matrix4 projection;
    private Matrix4 transform;
    private MaskStruct mask;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.projection = new Matrix4();
        this.transform = new Matrix4();
        this.mask = new MaskStruct();
    }

    @Override
    protected void get() {
        this.itemModelManager = get(ItemModelManager.class);
        this.materialManager = get(MaterialManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
        this.commandConsolePanelSystem = get(CommandConsolePanelSystem.class);
        this.commandConsoleTileGridSystem = get(CommandConsoleTileGridSystem.class);
    }

    @Override
    protected void awake() {

        // Render Target
        this.iconFbo = fboManager.cloneFbo(RuntimeSetting.FBO_INVENTORY, context.getWindow());

        // Resources
        this.itemMaterialID = materialManager.getMaterialIDFromMaterialName(RuntimeSetting.MATERIAL_INVENTORY_ITEM);
        this.vehicleMaterialID = materialManager.getMaterialIDFromMaterialName(
                CommandConsoleSetting.MATERIAL_TILE_VEHICLE);
    }

    // Render \\

    @Override
    protected void render() {

        ObjectArrayList<CommandConsoleTileGridStruct<ItemDefinitionHandle>> itemGrids =
                commandConsoleTileGridSystem.getItemGrids();
        ObjectArrayList<CommandConsoleTileGridStruct<VehicleHandle>> vehicleGrids =
                commandConsoleTileGridSystem.getVehicleGrids();
        WindowInstance window = context.getWindow();

        if (itemGrids.isEmpty() && vehicleGrids.isEmpty())
            return;

        InventoryViewUtility.composeProjection(window.getWidth(), window.getHeight(), projection);
        renderManager.ensureFboRendered(iconFbo, window);

        ElementInstance tree = commandConsolePanelSystem.getCommandConsoleMenu()
                .getEntryPoint(CommandConsoleSetting.ENTRY_COMMAND_TREE);

        for (int i = 0; i < itemGrids.size(); i++)
            if (clipTo(itemGrids.get(i).getGridElement(), tree))
                pushItemGrid(itemGrids.get(i), window);

        for (int i = 0; i < vehicleGrids.size(); i++)
            if (clipTo(vehicleGrids.get(i).getGridElement(), tree))
                pushVehicleGrid(vehicleGrids.get(i), window);

        fboRenderSystem.pushFbo(iconFbo, RuntimeSetting.LAYER_INVENTORY, window);
    }

    private void pushItemGrid(CommandConsoleTileGridStruct<ItemDefinitionHandle> grid, WindowInstance window) {

        for (int i = 0; i < grid.getTileCount(); i++) {

            ElementInstance icon = grid.getIconElement(i);

            if (!isVisible(icon))
                continue;

            ItemDefinitionHandle item = grid.getEntry(i);

            InventoryViewUtility.composeIconMatrix(
                    icon.getComputedLeft() + icon.getComputedW() * 0.5f,
                    icon.getComputedTop() + icon.getComputedH() * 0.5f,
                    Math.min(icon.getComputedW(), icon.getComputedH()),
                    item.getShape(),
                    transform);

            pushMesh(item.getMeshHandle().getMeshData(), itemMaterialID, window);
        }
    }

    private void pushVehicleGrid(CommandConsoleTileGridStruct<VehicleHandle> grid, WindowInstance window) {

        for (int i = 0; i < grid.getTileCount(); i++) {

            ElementInstance icon = grid.getIconElement(i);

            if (!isVisible(icon))
                continue;

            VehicleHandle vehicle = grid.getEntry(i);

            InventoryViewUtility.composeIconMatrix(
                    icon.getComputedLeft() + icon.getComputedW() * 0.5f,
                    icon.getComputedTop() + icon.getComputedH() * 0.5f,
                    Math.min(icon.getComputedW(), icon.getComputedH()),
                    vehicle.getMinX(),
                    vehicle.getMinY(),
                    vehicle.getMinZ(),
                    vehicle.getMaxX() - vehicle.getMinX(),
                    vehicle.getMaxY() - vehicle.getMinY(),
                    vehicle.getMaxZ() - vehicle.getMinZ(),
                    transform);

            pushVehicle(vehicle, window);
        }
    }

    // Draw \\

    // The hull meshes and every moving part shown under way, each part at rest where it is modelled
    private void pushVehicle(VehicleHandle vehicle, WindowInstance window) {

        ObjectArrayList<MeshInstance> hullMeshes = vehicle.getHullMeshes();

        for (int i = 0; i < hullMeshes.size(); i++)
            pushMesh(hullMeshes.get(i).getMeshData(), vehicleMaterialID, window);

        for (int partIndex = 0; partIndex < vehicle.getPartCount(); partIndex++) {

            VehiclePartStruct part = vehicle.getPart(partIndex);

            if (part.getRole().isStatic() || !VehicleSpaceUtility.isPartShownUnderWay(part))
                continue;

            ObjectArrayList<MeshInstance> meshes = part.getMeshes();

            for (int i = 0; i < meshes.size(); i++)
                pushMesh(meshes.get(i).getMeshData(), vehicleMaterialID, window);
        }
    }

    private void pushMesh(MeshData meshData, int materialID, WindowInstance window) {

        ModelInstance model = itemModelManager.acquireModel(meshData, materialID);

        model.getMaterial().setUniform(RuntimeSetting.UNIFORM_INVENTORY_PROJECTION, projection);
        model.getMaterial().setUniform(RuntimeSetting.UNIFORM_INVENTORY_MODEL, transform);
        model.getMaterial().setUniform(RuntimeSetting.UNIFORM_INVENTORY_TINT, RuntimeSetting.INVENTORY_TINT_NONE);

        renderManager.pushRenderCall(
                model, iconFbo, CommandConsoleSetting.DEPTH_TILE_ICON, mask, window);
    }

    // Utility \\

    // Sets the mask to where the grid shows inside the tree, false when none of it does
    private boolean clipTo(ElementInstance grid, ElementInstance tree) {

        int left = (int) Math.max(grid.getComputedLeft(), tree.getComputedLeft());
        int top = (int) Math.max(grid.getComputedTop(), tree.getComputedTop());
        int right = (int) Math.min(
                grid.getComputedLeft() + grid.getComputedW(), tree.getComputedLeft() + tree.getComputedW());
        int bottom = (int) Math.min(
                grid.getComputedTop() + grid.getComputedH(), tree.getComputedTop() + tree.getComputedH());

        if (right <= left || bottom <= top)
            return false;

        mask.set(left, top, right - left, bottom - top);

        return true;
    }

    // Whether any of an icon lies inside the current mask
    private boolean isVisible(ElementInstance icon) {
        return icon.getComputedW() > 0f
                && icon.getComputedH() > 0f
                && icon.getComputedLeft() < mask.getX() + mask.getW()
                && icon.getComputedLeft() + icon.getComputedW() > mask.getX()
                && icon.getComputedTop() < mask.getY() + mask.getH()
                && icon.getComputedTop() + icon.getComputedH() > mask.getY();
    }
}
