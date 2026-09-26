package application.runtime.inventory;

import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.itempipeline.container.ContainerInstance;
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
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InventoryRenderSystem extends SystemPackage {

    /*
     * Draws the 3D half of this window's open inventory into its own target
     * over the menus: each container's shell and resting items, a carried
     * item's landing preview tinted by fit, equipment icons, and a carried item
     * following the cursor. All transforms come from InventoryViewUtility.
     */

    // Internal
    private InventoryBranch inventoryBranch;
    private InputManager inputManager;
    private ItemModelManager itemModelManager;
    private MeshManager meshManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;

    // Render Target
    private FBOInstance inventoryFbo;

    // Resources
    private int itemMaterialID;
    private int shellMaterialID;
    private MeshHandle shellMesh;

    // Scratch
    private Matrix4 projection;
    private Matrix4 transform;
    private Matrix4 transformScratch;
    private Vector2 cellsScratch;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.projection = new Matrix4();
        this.transform = new Matrix4();
        this.transformScratch = new Matrix4();
        this.cellsScratch = new Vector2();
    }

    @Override
    protected void get() {
        this.inventoryBranch = get(InventoryBranch.class);
        this.inputManager = get(InputManager.class);
        this.itemModelManager = get(ItemModelManager.class);
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
    }

    @Override
    protected void awake() {

        // Render Target
        this.inventoryFbo = fboManager.cloneFbo(RuntimeSetting.FBO_INVENTORY, context.getWindow());

        // Resources
        this.itemMaterialID = materialManager.getMaterialIDFromMaterialName(RuntimeSetting.MATERIAL_INVENTORY_ITEM);
        this.shellMaterialID = materialManager.getMaterialIDFromMaterialName(RuntimeSetting.MATERIAL_INVENTORY_SHELL);
        this.shellMesh = meshManager.getMeshHandleFromMeshName(RuntimeSetting.MESH_INVENTORY_SHELL);
    }

    // Render \\

    @Override
    protected void render() {

        InventorySessionStruct session = inventoryBranch.getSession();
        WindowInstance window = context.getWindow();

        if (session == null)
            return;

        InventoryViewUtility.composeProjection(window.getWidth(), window.getHeight(), projection);
        renderManager.ensureFboRendered(inventoryFbo, window);

        for (InventoryViewStruct view : session.getViews())
            if (view.isOpen() && hasArea(view.getViewElement()))
                pushView(session, view, window);

        pushSlots(session, window);
        pushHeld(session, window);

        fboRenderSystem.pushFbo(inventoryFbo, RuntimeSetting.LAYER_INVENTORY, window);
    }

    // Views \\

    private void pushView(InventorySessionStruct session, InventoryViewStruct view, WindowInstance window) {

        ContainerInstance containerInstance = view.getContainerInstance();
        Matrix4 viewMatrix = view.getViewMatrix();

        for (int face = 0; face < InventoryViewUtility.getShellFaceCount(); face++)
            if (InventoryViewUtility.isShellFaceVisible(viewMatrix, face))
                pushShellFace(containerInstance, viewMatrix, face, window);

        ObjectArrayList<ContainerSlotStruct> slots = containerInstance.getSlots();

        for (int i = 0; i < slots.size(); i++) {

            ContainerSlotStruct slot = slots.get(i);
            ItemDefinitionHandle item = slot.getItemInstance().getItemDefinitionHandle();

            InventoryViewUtility.composeItemMatrix(
                    viewMatrix, item.getShape(), slot.getX(), slot.getY(), slot.getZ(), slot.getRotation(),
                    transform, transformScratch);
            pushItem(item, transform, RuntimeSetting.INVENTORY_TINT_NONE, window);
        }

        if (!session.isHolding() || session.getDropView() != view)
            return;

        InventoryHeldStruct held = session.getHeld();
        ItemDefinitionHandle item = held.getItemInstance().getItemDefinitionHandle();

        InventoryViewUtility.composeItemMatrix(
                viewMatrix, item.getShape(), session.getDropX(), session.getDropY(), session.getDropZ(),
                held.getRotation(), transform, transformScratch);
        pushItem(
                item,
                transform,
                session.isDropValid() ? RuntimeSetting.INVENTORY_TINT_VALID : RuntimeSetting.INVENTORY_TINT_INVALID,
                window);
    }

    private void pushShellFace(
            ContainerInstance containerInstance,
            Matrix4 viewMatrix,
            int face,
            WindowInstance window) {

        ModelInstance model = itemModelManager.acquireModel(shellMesh, shellMaterialID);

        InventoryViewUtility.composeShellMatrix(viewMatrix, containerInstance, face, transform);
        model.getMaterial().setUniform(RuntimeSetting.UNIFORM_INVENTORY_PROJECTION, projection);
        model.getMaterial().setUniform(RuntimeSetting.UNIFORM_INVENTORY_MODEL, transform);
        model.getMaterial().setUniform(
                RuntimeSetting.UNIFORM_INVENTORY_CELLS,
                InventoryViewUtility.resolveShellCells(containerInstance, face, cellsScratch));

        renderManager.pushRenderCall(model, inventoryFbo, RuntimeSetting.INVENTORY_DRAW_DEPTH, window);
    }

    // Slots \\

    private void pushSlots(InventorySessionStruct session, WindowInstance window) {

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
        pushItem(item, transform, tint, window);
    }

    private void pushItem(ItemDefinitionHandle item, Matrix4 modelMatrix, Vector4 tint, WindowInstance window) {

        ModelInstance model = itemModelManager.acquireModel(item.getMeshHandle(), itemMaterialID);

        model.getMaterial().setUniform(RuntimeSetting.UNIFORM_INVENTORY_PROJECTION, projection);
        model.getMaterial().setUniform(RuntimeSetting.UNIFORM_INVENTORY_MODEL, modelMatrix);
        model.getMaterial().setUniform(RuntimeSetting.UNIFORM_INVENTORY_TINT, tint);

        renderManager.pushRenderCall(model, inventoryFbo, RuntimeSetting.INVENTORY_DRAW_DEPTH, window);
    }

    // Utility \\

    private boolean hasArea(ElementInstance element) {
        return element.getComputedW() > 0f && element.getComputedH() > 0f;
    }
}
