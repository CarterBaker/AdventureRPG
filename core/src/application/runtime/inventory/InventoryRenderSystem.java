package application.runtime.inventory;

import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.itempipeline.container.ContainerSlotStruct;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemmodelmanager.ItemModelManager;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import application.runtime.menueventsmanager.menus.inventory.InventoryBranch;
import application.runtime.menueventsmanager.menus.inventory.InventoryHeldStruct;
import application.runtime.menueventsmanager.menus.inventory.InventorySessionStruct;
import application.runtime.menueventsmanager.menus.inventory.InventoryViewStruct;
import application.runtime.world.WorldSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InventoryRenderSystem extends SystemPackage {

    /*
     * Draws the 3D half of this window's open inventory. Each open container
     * is drawn in its panel through the panel's own camera, into the
     * inventory's own target over the menus: its shell — a pocket's box of
     * walls, or the container's own model — and what it holds. A container
     * opened where it lies is also drawn in the world target with the
     * terrain, open without its lid, showing what it holds only when the
     * space is inside its own model; a pocket is never seen in the world. A
     * carried item's landing preview, tinted by fit, goes over its panel, as
     * do equipment icons and a carried item following the cursor. All
     * transforms come from InventoryViewUtility.
     */

    // Internal
    private InventoryBranch inventoryBranch;
    private InputManager inputManager;
    private ItemModelManager itemModelManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;
    private WorldSystem worldSystem;

    // Render Target
    private FBOInstance inventoryFbo;

    // Resources
    private int itemMaterialID;
    private int worldMaterialID;

    // Scratch
    private Matrix4 iconProjection;
    private Matrix4 transform;
    private Matrix4 transformScratch;
    private Matrix4 worldContainer;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.iconProjection = new Matrix4();
        this.transform = new Matrix4();
        this.transformScratch = new Matrix4();
        this.worldContainer = new Matrix4();
    }

    @Override
    protected void get() {
        this.inventoryBranch = get(InventoryBranch.class);
        this.inputManager = get(InputManager.class);
        this.itemModelManager = get(ItemModelManager.class);
        this.materialManager = get(MaterialManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
        this.worldSystem = get(WorldSystem.class);
    }

    @Override
    protected void awake() {

        // Render Target
        this.inventoryFbo = fboManager.cloneFbo(RuntimeSetting.FBO_INVENTORY, context.getWindow());

        // Resources
        this.itemMaterialID = materialManager.getMaterialIDFromMaterialName(RuntimeSetting.MATERIAL_INVENTORY_ITEM);
        this.worldMaterialID = materialManager.getMaterialIDFromMaterialName(EngineSetting.EQUIPMENT_ITEM_MATERIAL);
    }

    // Render \\

    @Override
    protected void render() {

        InventorySessionStruct session = inventoryBranch.getSession();
        WindowInstance window = context.getWindow();

        if (session == null)
            return;

        InventoryViewUtility.composeProjection(window.getWidth(), window.getHeight(), iconProjection);
        renderManager.ensureFboRendered(inventoryFbo, window);

        for (InventoryViewStruct view : session.getViews()) {

            if (view.isShown())
                pushPanel(session, view, window);

            if (view.isInWorld() && view.isWorldPlaced())
                pushWorld(view, window);
        }

        pushSlots(session, window);
        pushHeld(session, window);

        fboRenderSystem.pushFbo(inventoryFbo, RuntimeSetting.LAYER_INVENTORY, window);
    }

    // Panels \\

    // The container through its panel's camera: its shell, what it holds, and where a carried item would land
    private void pushPanel(InventorySessionStruct session, InventoryViewStruct view, WindowInstance window) {

        Matrix4 containerMatrix = view.getContainerMatrix();
        Matrix4 viewProjection = view.getViewProjection();
        ItemDefinitionHandle containerItem = view.getContainerItem().getItemDefinitionHandle();

        InventoryViewUtility.composeShellMatrix(containerMatrix, containerItem, transform);
        pushOverlayMesh(resolveShellMesh(containerItem), viewProjection, RuntimeSetting.INVENTORY_TINT_NONE, window);

        ObjectArrayList<ContainerSlotStruct> slots = view.getContainerInstance().getSlots();

        for (int i = 0; i < slots.size(); i++) {

            ContainerSlotStruct slot = slots.get(i);
            ItemDefinitionHandle item = slot.getItemInstance().getItemDefinitionHandle();

            InventoryViewUtility.composeItemMatrix(
                    containerMatrix, item.getShape(), slot.getX(), slot.getY(), slot.getZ(), slot.getRotation(),
                    transform, transformScratch);
            pushOverlayMesh(
                    item.getMeshHandle().getMeshData(), viewProjection, RuntimeSetting.INVENTORY_TINT_NONE, window);
        }

        if (!session.isHolding() || session.getDropView() != view)
            return;

        InventoryHeldStruct held = session.getHeld();
        ItemDefinitionHandle item = held.getItemInstance().getItemDefinitionHandle();

        InventoryViewUtility.composeItemMatrix(
                containerMatrix, item.getShape(), session.getDropX(), session.getDropY(), session.getDropZ(),
                held.getRotation(), transform, transformScratch);
        pushOverlayMesh(
                item.getMeshHandle().getMeshData(),
                viewProjection,
                session.isDropValid() ? RuntimeSetting.INVENTORY_TINT_VALID : RuntimeSetting.INVENTORY_TINT_INVALID,
                window);
    }

    // A pocket shows its box of walls; a space inside the model shows the model, opened when it has a lid
    private MeshData resolveShellMesh(ItemDefinitionHandle containerItem) {

        if (containerItem.getContainerSpace().isPocket())
            return containerItem.getPocketMeshData();

        return containerItem.hasOpenMesh()
                ? containerItem.getOpenMeshData()
                : containerItem.getMeshHandle().getMeshData();
    }

    // World \\

    // Where it stands, a container is drawn open, and a space inside its own model shows what it holds
    private void pushWorld(InventoryViewStruct view, WindowInstance window) {

        ItemDefinitionHandle containerItem = view.getContainerItem().getItemDefinitionHandle();

        if (containerItem.hasOpenMesh()) {
            transform.set(view.getWorldItemMatrix());
            pushWorldMesh(containerItem.getOpenMeshData(), window);
        }

        if (containerItem.getContainerSpace().isPocket())
            return;

        InventoryViewUtility.composeWorldContainerMatrix(view.getWorldItemMatrix(), containerItem, worldContainer);

        ObjectArrayList<ContainerSlotStruct> slots = view.getContainerInstance().getSlots();

        for (int i = 0; i < slots.size(); i++) {

            ContainerSlotStruct slot = slots.get(i);
            ItemDefinitionHandle item = slot.getItemInstance().getItemDefinitionHandle();

            InventoryViewUtility.composeItemMatrix(
                    worldContainer, item.getShape(), slot.getX(), slot.getY(), slot.getZ(), slot.getRotation(),
                    transform, transformScratch);
            pushWorldMesh(item.getMeshHandle().getMeshData(), window);
        }
    }

    private void pushWorldMesh(MeshData meshData, WindowInstance window) {

        ModelInstance model = itemModelManager.acquireModel(meshData, worldMaterialID);

        model.getMaterial().setUniform(EngineSetting.UNIFORM_ITEM_MODEL, transform);
        renderManager.pushRenderCall(model, worldSystem.getWorldFbo(), EngineSetting.EQUIPMENT_RENDER_DEPTH, window);
    }

    // Slots \\

    private void pushSlots(InventorySessionStruct session, WindowInstance window) {

        if (!session.hasEquipment())
            return;

        InventoryHandle inventory = session.getInventory();

        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {

            ElementInstance slotElement = session.getSlotElement(equipmentSlot);

            if (!inventory.hasItem(equipmentSlot) || !hasArea(slotElement))
                continue;

            pushIcon(
                    inventory.getItem(equipmentSlot),
                    slotElement.getComputedLeft() + slotElement.getComputedW() * 0.5f,
                    slotElement.getComputedTop() + slotElement.getComputedH() * 0.5f,
                    Math.min(slotElement.getComputedW(), slotElement.getComputedH()),
                    inventory.isHidden(equipmentSlot) ? RuntimeSetting.INVENTORY_TINT_HIDDEN
                            : RuntimeSetting.INVENTORY_TINT_NONE,
                    window);
        }
    }

    // Held \\

    private void pushHeld(InventorySessionStruct session, WindowInstance window) {

        if (!session.isHolding() || session.hasDrop())
            return;

        pushIcon(
                session.getHeld().getItemInstance(),
                inputManager.getHoverMouseX(window),
                inputManager.getHoverMouseY(window),
                RuntimeSetting.INVENTORY_HELD_ICON_SIZE,
                RuntimeSetting.INVENTORY_TINT_NONE,
                window);
    }

    // Draw \\

    private void pushIcon(
            ItemInstance itemInstance,
            float centerX,
            float centerY,
            float size,
            Vector4 tint,
            WindowInstance window) {

        ItemDefinitionHandle item = itemInstance.getItemDefinitionHandle();

        InventoryViewUtility.composeIconMatrix(centerX, centerY, size, item.getShape(), transform);
        pushOverlayMesh(item.getMeshHandle().getMeshData(), iconProjection, tint, window);
    }

    private void pushOverlayMesh(MeshData meshData, Matrix4 projection, Vector4 tint, WindowInstance window) {

        ModelInstance model = itemModelManager.acquireModel(meshData, itemMaterialID);

        model.getMaterial().setUniform(RuntimeSetting.UNIFORM_INVENTORY_PROJECTION, projection);
        model.getMaterial().setUniform(RuntimeSetting.UNIFORM_INVENTORY_MODEL, transform);
        model.getMaterial().setUniform(RuntimeSetting.UNIFORM_INVENTORY_TINT, tint);

        renderManager.pushRenderCall(model, inventoryFbo, RuntimeSetting.INVENTORY_DRAW_DEPTH, window);
    }

    // Utility \\

    private boolean hasArea(ElementInstance element) {
        return element.getComputedW() > 0f && element.getComputedH() > 0f;
    }
}
