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
     * Runs the inventory for this context's window. The inventory key shows
     * the equipment board around the framed character preview — turned and
     * panned by dragging, zoomed with the wheel — with the worn bag open in
     * its panel beside it. Activating a chest or bag that lies in the world
     * opens it where it lies instead, once nothing rests in its lid's way, the
     * camera left where it was: its panel on the right, the worn bag's on the
     * left. Either closes on the inventory key or Pause, and a container also
     * on being activated again or leaving the world, returning any
     * carried item and the camera. Each frame the drag, container and
     * equipment branches settle and redraw what changed. A new item handed in
     * while the cursor is over the open inventory lands where it points.
     * findOpenableContainer() is the one place that decides which container
     * the activate binding would open.
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

        if (session.hasChest() && !worldItemPlacementSystem.isPlaced(session.getChestWorldItem())) {
            closeMenu();
            return;
        }

        boolean closePressed = inputManager.bindingClicked(KeyBindings.INVENTORY, window)
                || inputManager.bindingClicked(KeyBindings.PAUSE, window)
                || session.hasChest() && inputManager.bindingClicked(KeyBindings.ACTIVATE, window);

        if (closePressed && session.isCloseArmed() && !session.isHolding()) {
            closeMenu();
            return;
        }

        inventoryContainerBranch.update(session);
        inventoryDragBranch.update(session);
        inventoryEquipmentBranch.update(session);

        if (session.hasEquipment()) {
            zoomPreview(session);
            framePreview(session);
        }

        session.setCloseArmed(session.getSceneMenu().isVisible());
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

        if (!inputManager.bindingClicked(KeyBindings.ACTIVATE, window))
            return;

        WorldItemInstance container = findOpenableContainer(window);

        if (container != null)
            openMenu(window, container);
    }

    // The container the player faces and could open where it lies — null when it faces none or nothing is open
    // to it now
    public WorldItemInstance findOpenableContainer(WindowInstance window) {

        if (session != null || !canOpen(window))
            return null;

        WorldItemInstance targetItem = playerManager.getTargetItemForWindow(window.getWindowID());

        return targetItem != null && worldItemPlacementSystem.canOpen(targetItem) ? targetItem : null;
    }

    private boolean canOpen(WindowInstance window) {
        return playerManager.isPlayingForWindow(window.getWindowID());
    }

    // The scene menu opens first, so its drag surface lies under every other inventory menu
    private void openMenu(WindowInstance window, WorldItemInstance chestWorldItem) {

        int windowID = window.getWindowID();
        MenuInstance sceneMenu = menuManager.openMenu(RuntimeSetting.MENU_INVENTORY_SCENE, window);
        MenuInstance equipmentMenu = chestWorldItem == null
                ? menuManager.openMenu(RuntimeSetting.MENU_INVENTORY_EQUIPMENT, window)
                : null;

        this.session = new InventorySessionStruct(
                window,
                playerManager.getPlayerForWindow(windowID),
                playerManager.getCameraForWindow(windowID).getDirection(),
                sceneMenu,
                equipmentMenu,
                chestWorldItem);

        if (session.hasEquipment()) {
            playerManager.beginCharacterPreview(windowID);
            inventoryEquipmentBranch.populate(session);
        }

        inventoryContainerBranch.update(session);
    }

    public void closeMenu() {

        if (session == null)
            return;

        int windowID = session.getWindow().getWindowID();

        inventoryDragBranch.cancel(session);
        inventoryContainerBranch.closeAll(session);

        if (session.hasEquipment()) {
            menuManager.closeMenu(session.getEquipmentMenu());
            playerManager.endCharacterPreview(windowID);
            playerManager.getCameraForWindow(windowID).setDirection(session.getCameraDirection());
        }

        menuManager.closeMenu(session.getSceneMenu());

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

        if (session == null || !session.hasEquipment())
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
