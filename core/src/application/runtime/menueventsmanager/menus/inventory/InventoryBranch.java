package application.runtime.menueventsmanager.menus.inventory;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.worldpipeline.worlditem.WorldItemInstance;
import application.bootstrap.worldpipeline.worlditemmanager.WorldItemPlacementSystem;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import application.runtime.RuntimeSetting;
import application.runtime.inventory.InventoryViewUtility;
import engine.input.Input;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.settings.KeyBindings;
import engine.util.mathematics.vectors.Vector3;

public class InventoryBranch extends BranchPackage {

    /*
     * Runs the inventory for this context's window. Opens on the inventory key
     * or when a chest is used, shows the equipment board around the framed
     * character preview — turned and panned by dragging, zoomed with the
     * wheel — and closes on the key or Pause, returning any carried item and
     * the camera. Each frame the drag, container and equipment branches
     * settle and redraw what changed. A new item handed in while the cursor
     * is over the open inventory lands where it points.
     */

    // Internal
    private MenuManager menuManager;
    private InputManager inputManager;
    private WindowManager windowManager;
    private PlayerManager playerManager;
    private WorldItemPlacementSystem worldItemPlacementSystem;
    private InventoryEquipmentBranch inventoryEquipmentBranch;
    private InventoryContainerBranch inventoryContainerBranch;
    private InventoryDragBranch inventoryDragBranch;

    // State
    private InventorySessionStruct session;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.inputManager = get(InputManager.class);
        this.windowManager = get(WindowManager.class);
        this.playerManager = get(PlayerManager.class);
        this.worldItemPlacementSystem = get(WorldItemPlacementSystem.class);
        this.inventoryEquipmentBranch = get(InventoryEquipmentBranch.class);
        this.inventoryContainerBranch = get(InventoryContainerBranch.class);
        this.inventoryDragBranch = get(InventoryDragBranch.class);
    }

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();

        if (session == null) {
            updateClosed(window);
            return;
        }

        boolean closePressed = inputManager.bindingClicked(KeyBindings.INVENTORY, window)
                || inputManager.bindingClicked(KeyBindings.PAUSE, window);

        if (closePressed && session.isCloseArmed() && !session.isHolding()) {
            closeMenu();
            return;
        }

        inventoryDragBranch.update(session);
        inventoryContainerBranch.update(session);
        inventoryEquipmentBranch.update(session);
        zoomPreview(session);
        framePreview(session);

        session.setCloseArmed(session.getEquipmentMenu().isVisible());
    }

    @Override
    protected void dispose() {

        if (session != null)
            inventoryDragBranch.cancel(session);
    }

    // Open / Close \\

    private void updateClosed(WindowInstance window) {

        if (!canOpen(window))
            return;

        if (inputManager.bindingClicked(KeyBindings.INVENTORY, window)) {
            openMenu(window, null);
            return;
        }

        if (!inputManager.bindingClicked(KeyBindings.SECONDARY, window))
            return;

        WorldItemInstance targetItem = playerManager.getTargetItemForWindow(window.getWindowID());

        if (targetItem != null && targetItem.getItemDefinitionHandle().isContainer())
            openMenu(window, worldItemPlacementSystem.resolveItemInstance(targetItem));
    }

    private boolean canOpen(WindowInstance window) {

        int windowID = window.getWindowID();

        return playerManager.hasPlayerForWindow(windowID)
                && !playerManager.isFreeCameraForWindow(windowID)
                && !playerManager.isCharacterPreview(windowID)
                && !window.getMenuListHandle().isInputLocked();
    }

    private void openMenu(WindowInstance window, ItemInstance chestItem) {

        int windowID = window.getWindowID();
        MenuInstance equipmentMenu = menuManager.openMenu(RuntimeSetting.MENU_INVENTORY_EQUIPMENT, window);

        this.session = new InventorySessionStruct(
                window,
                playerManager.getPlayerForWindow(windowID),
                playerManager.getCameraForWindow(windowID).getDirection(),
                equipmentMenu,
                chestItem);

        playerManager.beginCharacterPreview(windowID);

        inventoryEquipmentBranch.populate(session);
        inventoryContainerBranch.update(session);
    }

    public void closeMenu() {

        if (session == null)
            return;

        int windowID = session.getWindow().getWindowID();

        inventoryDragBranch.cancel(session);
        inventoryContainerBranch.closeAll(session);
        menuManager.closeMenu(session.getEquipmentMenu());

        playerManager.endCharacterPreview(windowID);
        playerManager.getCameraForWindow(windowID).setDirection(session.getCameraDirection());

        this.session = null;
    }

    // Receive \\

    // Lands a new item where the cursor points in this window's open inventory, false when it lands nowhere
    public boolean receiveItem(WindowInstance window, ItemInstance itemInstance) {

        InventorySessionStruct session = getSession(window);

        if (session == null || !windowManager.getHoveredWindows().contains(window))
            return false;

        return inventoryDragBranch.receive(
                session,
                itemInstance,
                inputManager.getHoverMouseX(window),
                inputManager.getHoverMouseY(window));
    }

    // Preview \\

    // Dragging across the preview turns the character; dragging up or down pans while zoomed in
    public void rotatePreview(WindowInstance window) {

        InventorySessionStruct session = getSession(window);

        if (session == null)
            return;

        Input rawInput = inputManager.getRawInput(window);

        playerManager.rotateCharacterPreview(
                window.getWindowID(),
                rawInput.getDeltaX() * RuntimeSetting.INVENTORY_ROTATE_DEGREES_PER_PIXEL);
        session.setPreviewFocus(clampFocus(session, session.getPreviewFocus()
                + rawInput.getDeltaY() * RuntimeSetting.INVENTORY_PREVIEW_PAN_PER_PIXEL / session.getPreviewZoom()));
    }

    // The mouse wheel over the preview zooms in on the character
    private void zoomPreview(InventorySessionStruct session) {

        WindowInstance window = session.getWindow();
        float wheel = inputManager.getRawInput(window).getScrollY();
        ElementInstance preview = session.getEquipmentMenu().getEntryPoint(RuntimeSetting.ENTRY_INVENTORY_PREVIEW);

        if (wheel == 0f || !InventoryViewUtility.isInside(
                preview, inputManager.getHoverMouseX(window), inputManager.getHoverMouseY(window)))
            return;

        float zoom = session.getPreviewZoom() * (float) Math.pow(RuntimeSetting.INVENTORY_PREVIEW_ZOOM_STEP, wheel);

        session.setPreviewZoom(Math.max(
                RuntimeSetting.INVENTORY_PREVIEW_ZOOM_MIN,
                Math.min(RuntimeSetting.INVENTORY_PREVIEW_ZOOM_MAX, zoom)));
        session.setPreviewFocus(clampFocus(session, session.getPreviewFocus()));
    }

    // Keeps the framed share of the character on the character itself at the current zoom
    private float clampFocus(InventorySessionStruct session, float focus) {

        float center = EngineSetting.CHARACTER_PREVIEW_CENTER_HEIGHT;
        float slack = 1f - 1f / session.getPreviewZoom();

        return Math.max(center - slack * center, Math.min(center + slack * (1f - center), focus));
    }

    // Centres the character's focus in the preview window at the largest scale its proportions fit, times the zoom
    private void framePreview(InventorySessionStruct session) {

        WindowInstance window = session.getWindow();
        ElementInstance preview = session.getEquipmentMenu().getEntryPoint(RuntimeSetting.ENTRY_INVENTORY_PREVIEW);
        Vector3 size = session.getPlayer().getSize();
        float width = Math.max(1f, window.getWidth());
        float height = Math.max(1f, window.getHeight());

        if (preview.getComputedW() <= 0f || preview.getComputedH() <= 0f)
            return;

        float characterHeight = Math.min(
                preview.getComputedH(),
                preview.getComputedW() * size.y / Math.max(size.x, size.z))
                * RuntimeSetting.INVENTORY_PREVIEW_FILL * session.getPreviewZoom();
        float centerY = preview.getComputedTop() + preview.getComputedH() * 0.5f
                - (session.getPreviewFocus() - EngineSetting.CHARACTER_PREVIEW_CENTER_HEIGHT) * characterHeight;

        playerManager.frameCharacterPreview(
                window.getWindowID(),
                (preview.getComputedLeft() + preview.getComputedW() * 0.5f) / width * 2f - 1f,
                centerY / height * 2f - 1f,
                characterHeight / height);
    }

    // Accessible \\

    public InventorySessionStruct getSession() {
        return session;
    }

    public InventorySessionStruct getSession(WindowInstance window) {
        return session != null && session.getWindow() == window ? session : null;
    }
}
