package application.runtime.menueventsmanager.menus.inventory;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

public class InventorySessionStruct extends StructPackage {

    /*
     * One window's open inventory: the player it shows, the equipment menu
     * and the slot element for every equipment slot, a view for each
     * container that can be shown, the chest opened with it if any, the
     * item carried under the cursor, the drop that item would make where the
     * cursor is, the item under the cursor, and the item whose details are
     * on show. The shown revisions say which state of the inventory and the
     * backpack's contents the slots and statistics show. The camera
     * direction the inventory opened with is handed back when it closes.
     */

    // Window
    private final WindowInstance window;
    private final EntityInstance player;
    private final Vector3 cameraDirection;

    // Equipment
    private final MenuInstance equipmentMenu;
    private final ElementInstance[] slot2Element;
    private int shownRevision;
    private int shownContentRevision;

    // Containers
    private final InventoryViewStruct[] views;
    private final ItemInstance chestItem;

    // Drag
    private InventoryDragMode dragMode;
    private InventoryHeldStruct held;

    // Drop — the container view and place the held item would land in
    private InventoryViewStruct dropView;
    private int dropX;
    private int dropY;
    private int dropZ;
    private boolean dropValid;

    // Details
    private ItemInstance hoveredItem;
    private ItemInstance detailedItem;
    private boolean detailsShown;

    // Close
    private boolean closeArmed;

    // Constructor \\

    public InventorySessionStruct(
            WindowInstance window,
            EntityInstance player,
            Vector3 cameraDirection,
            MenuInstance equipmentMenu,
            ItemInstance chestItem) {

        // Window
        this.window = window;
        this.player = player;
        this.cameraDirection = new Vector3(cameraDirection);

        // Equipment
        this.equipmentMenu = equipmentMenu;
        this.slot2Element = new ElementInstance[EquipmentSlot.values().length];
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
        this.shownContentRevision = EngineSetting.INDEX_NOT_FOUND;

        // Containers
        this.views = new InventoryViewStruct[InventoryContainer.values().length];

        for (InventoryContainer inventoryContainer : InventoryContainer.values())
            this.views[inventoryContainer.ordinal()] = new InventoryViewStruct(inventoryContainer);

        this.chestItem = chestItem;

        // Drag
        this.dragMode = InventoryDragMode.NONE;
    }

    // Drop \\

    public void setDrop(InventoryViewStruct dropView, int dropX, int dropY, int dropZ, boolean dropValid) {
        this.dropView = dropView;
        this.dropX = dropX;
        this.dropY = dropY;
        this.dropZ = dropZ;
        this.dropValid = dropValid;
    }

    public void clearDrop() {
        this.dropView = null;
        this.dropValid = false;
    }

    // Accessible \\

    public WindowInstance getWindow() {
        return window;
    }

    public EntityInstance getPlayer() {
        return player;
    }

    public InventoryHandle getInventory() {
        return player.getInventoryHandle();
    }

    public Vector3 getCameraDirection() {
        return cameraDirection;
    }

    public MenuInstance getEquipmentMenu() {
        return equipmentMenu;
    }

    public ElementInstance getSlotElement(EquipmentSlot equipmentSlot) {
        return slot2Element[equipmentSlot.ordinal()];
    }

    public void setSlotElement(EquipmentSlot equipmentSlot, ElementInstance slotElement) {
        slot2Element[equipmentSlot.ordinal()] = slotElement;
    }

    public int getShownRevision() {
        return shownRevision;
    }

    public void setShownRevision(int shownRevision) {
        this.shownRevision = shownRevision;
    }

    public int getShownContentRevision() {
        return shownContentRevision;
    }

    public void setShownContentRevision(int shownContentRevision) {
        this.shownContentRevision = shownContentRevision;
    }

    public InventoryViewStruct getView(InventoryContainer inventoryContainer) {
        return views[inventoryContainer.ordinal()];
    }

    public InventoryViewStruct[] getViews() {
        return views;
    }

    public boolean hasChest() {
        return chestItem != null;
    }

    public ItemInstance getChestItem() {
        return chestItem;
    }

    public InventoryDragMode getDragMode() {
        return dragMode;
    }

    public void setDragMode(InventoryDragMode dragMode) {
        this.dragMode = dragMode;
    }

    public boolean isHolding() {
        return held != null;
    }

    public InventoryHeldStruct getHeld() {
        return held;
    }

    public void setHeld(InventoryHeldStruct held) {
        this.held = held;
    }

    public boolean hasDrop() {
        return dropView != null;
    }

    public InventoryViewStruct getDropView() {
        return dropView;
    }

    public int getDropX() {
        return dropX;
    }

    public int getDropY() {
        return dropY;
    }

    public int getDropZ() {
        return dropZ;
    }

    public boolean isDropValid() {
        return dropValid;
    }

    public ItemInstance getHoveredItem() {
        return hoveredItem;
    }

    public void setHoveredItem(ItemInstance hoveredItem) {
        this.hoveredItem = hoveredItem;
    }

    public ItemInstance getDetailedItem() {
        return detailedItem;
    }

    public boolean isDetailsShown() {
        return detailsShown;
    }

    public void setDetailedItem(ItemInstance detailedItem) {
        this.detailedItem = detailedItem;
        this.detailsShown = true;
    }

    public void invalidateDetails() {
        this.detailsShown = false;
    }

    public boolean isCloseArmed() {
        return closeArmed;
    }

    public void setCloseArmed(boolean closeArmed) {
        this.closeArmed = closeArmed;
    }
}
