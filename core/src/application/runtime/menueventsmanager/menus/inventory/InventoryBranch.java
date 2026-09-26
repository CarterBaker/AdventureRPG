package application.runtime.menueventsmanager.menus.inventory;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.worldpipeline.worlditem.WorldItemInstance;
import application.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import engine.root.BranchPackage;
import engine.settings.KeyBindings;
import engine.util.mathematics.vectors.Vector3;

public class InventoryBranch extends BranchPackage {

    /*
     * Runs the inventory for this context's window. The inventory key opens
     * it while a player is in the world and no other menu holds input; using
     * a chest in the world opens it with that chest shown too. The equipment
     * panels are always shown, with the player standing in the open preview
     * window between them — the character is drawn in the world behind the
     * menus and framed there through the character preview, filling the
     * window as far as its own proportions allow — and turned by dragging.
     * The inventory key or Pause closes it once it has been on show a whole
     * frame, handing any carried item back to where it came from and the
     * camera back to the way it looked; a context torn down with the
     * inventory open still hands back whatever the cursor carried. Each
     * frame the drag branch settles what the cursor carries, the container
     * branch keeps the backpack and chest panels in step with what is worn,
     * and the equipment branch redraws whatever changed.
     */

    // Internal
    private MenuManager menuManager;
    private InputManager inputManager;
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

    // Preview \\

    public void rotatePreview(WindowInstance window) {

        if (getSession(window) == null)
            return;

        playerManager.rotateCharacterPreview(
                window.getWindowID(),
                inputManager.getRawInput(window).getDeltaX() * RuntimeSetting.INVENTORY_ROTATE_DEGREES_PER_PIXEL);
    }

    // Centres the character in the preview window at the largest scale its own proportions fit
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
                preview.getComputedW() * size.y / Math.max(size.x, size.z)) * RuntimeSetting.INVENTORY_PREVIEW_FILL;

        playerManager.frameCharacterPreview(
                window.getWindowID(),
                (preview.getComputedLeft() + preview.getComputedW() * 0.5f) / width * 2f - 1f,
                (preview.getComputedTop() + preview.getComputedH() * 0.5f) / height * 2f - 1f,
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
