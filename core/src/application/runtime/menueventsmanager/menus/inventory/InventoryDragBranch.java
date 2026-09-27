package application.runtime.menueventsmanager.menus.inventory;

import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.itempipeline.container.ContainerInstance;
import application.bootstrap.itempipeline.container.ContainerSlotStruct;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemShapeStruct;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.inventory.InventoryViewUtility;
import engine.input.Buttons;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.settings.KeyBindings;
import engine.util.mathematics.vectors.Vector3;

public class InventoryDragBranch extends BranchPackage {

    /*
     * Carries items under the cursor, the same way in every container's
     * panel. A picked item leaves its place at once; pointed into an open
     * container it previews where it would land and can be turned a quarter;
     * dropping over a slot, container or list places it, trading with an
     * item that fits the vacated place, and anything that cannot land goes
     * back. A new item handed in from outside lands as if carried to the
     * cursor and let go. Also finds the item under the cursor for the
     * details panel.
     */

    // Internal
    private InputManager inputManager;
    private InventoryContainerBranch inventoryContainerBranch;

    // Scratch
    private Vector3 aimPoint;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.aimPoint = new Vector3();
    }

    @Override
    protected void get() {
        this.inputManager = get(InputManager.class);
        this.inventoryContainerBranch = get(InventoryContainerBranch.class);
    }

    // Update \\

    void update(InventorySessionStruct session) {

        WindowInstance window = session.getWindow();
        float x = inputManager.getHoverMouseX(window);
        float y = inputManager.getHoverMouseY(window);

        if (session.isHolding() && inputManager.bindingClicked(KeyBindings.ROTATE_ITEM, window))
            session.getHeld().turn();

        resolveDrop(session, x, y);

        if (!inputManager.getRawInput(window).isMouseDown(Buttons.LEFT)) {

            if (session.isHolding())
                drop(session, x, y);

            session.clearDrop();
            session.setDragMode(InventoryDragMode.NONE);
        }

        session.setHoveredItem(resolveHoveredItem(session, x, y));
    }

    // Pick Up \\

    void pickUpFromSlot(InventorySessionStruct session, EquipmentSlot equipmentSlot) {

        InventoryHandle inventory = session.getInventory();

        if (!inventory.hasItem(equipmentSlot)) {
            session.setDragMode(InventoryDragMode.SPENT);
            return;
        }

        hold(session, new InventoryHeldStruct(inventory.unequip(equipmentSlot), equipmentSlot));
    }

    void pickUpFromView(
            InventorySessionStruct session,
            InventoryViewStruct view,
            ContainerSlotStruct slot,
            float x,
            float y) {

        InventoryHeldStruct held = takeFromContainer(view.getContainerInstance(), slot);

        if (inventoryContainerBranch.pickPoint(session, view, x, y, aimPoint))
            held.setGrabOffset(slot.getX() - aimPoint.x, slot.getZ() - aimPoint.z);

        hold(session, held);
    }

    void pickUpFromList(InventorySessionStruct session, InventoryViewStruct view, ContainerSlotStruct slot) {
        hold(session, takeFromContainer(view.getContainerInstance(), slot));
    }

    private InventoryHeldStruct takeFromContainer(ContainerInstance containerInstance, ContainerSlotStruct slot) {

        containerInstance.remove(slot);

        return new InventoryHeldStruct(
                slot.getItemInstance(),
                containerInstance,
                slot.getX(),
                slot.getY(),
                slot.getZ(),
                slot.getRotation());
    }

    private void hold(InventorySessionStruct session, InventoryHeldStruct held) {
        session.setHeld(held);
        session.setDragMode(InventoryDragMode.CARRY);
    }

    // Drop Preview \\

    private void resolveDrop(InventorySessionStruct session, float x, float y) {

        session.clearDrop();

        if (!session.isHolding() || findListAt(session, x, y) != null)
            return;

        for (InventoryViewStruct view : session.getViews())
            if (inventoryContainerBranch.pointsInto(session, view, x, y)) {
                resolveViewDrop(session, view, x, y);
                return;
            }
    }

    // The carried item enters above the point the cursor aims at and falls until it rests
    private void resolveViewDrop(InventorySessionStruct session, InventoryViewStruct view, float x, float y) {

        if (!inventoryContainerBranch.pickPoint(session, view, x, y, aimPoint))
            return;

        InventoryHeldStruct held = session.getHeld();
        ItemInstance itemInstance = held.getItemInstance();
        ItemShapeStruct shape = itemInstance.getItemDefinitionHandle().getShape();
        ContainerInstance containerInstance = view.getContainerInstance();
        int rotation = held.getRotation();
        int sizeX = shape.getRotatedSizeX(rotation);
        int sizeZ = shape.getRotatedSizeZ(rotation);

        float cornerX = held.isGrabbed() ? aimPoint.x + held.getGrabOffsetX() : aimPoint.x - sizeX * 0.5f;
        float cornerZ = held.isGrabbed() ? aimPoint.z + held.getGrabOffsetZ() : aimPoint.z - sizeZ * 0.5f;
        int dropX = Math.max(0, Math.min(containerInstance.getSizeX() - sizeX, Math.round(cornerX)));
        int dropZ = Math.max(0, Math.min(containerInstance.getSizeZ() - sizeZ, Math.round(cornerZ)));
        int dropY = containerInstance.accepts(itemInstance)
                ? containerInstance.findRestingY(itemInstance, dropX, dropZ, rotation)
                : EngineSetting.INDEX_NOT_FOUND;
        boolean valid = dropY != EngineSetting.INDEX_NOT_FOUND;

        if (!valid)
            dropY = Math.max(0, containerInstance.getSizeY() - shape.getSizeY());

        session.setDrop(view, dropX, dropY, dropZ, valid);
    }

    // Drop \\

    private void drop(InventorySessionStruct session, float x, float y) {

        InventoryHeldStruct held = session.getHeld();
        session.setHeld(null);

        if (!land(session, held, x, y))
            returnToSource(session, held);
    }

    // Puts the carried item wherever the cursor lets go of it, false when it lands nowhere
    private boolean land(InventorySessionStruct session, InventoryHeldStruct held, float x, float y) {

        EquipmentSlot equipmentSlot = findSlotAt(session, x, y);

        if (equipmentSlot != null)
            return equipInto(session, held, equipmentSlot);

        if (session.hasDrop()) {

            if (!session.isDropValid())
                return false;

            session.getDropView().getContainerInstance().place(
                    held.getItemInstance(),
                    session.getDropX(),
                    session.getDropY(),
                    session.getDropZ(),
                    held.getRotation());

            return true;
        }

        InventoryViewStruct listView = findListAt(session, x, y);

        return listView != null && listView.getContainerInstance().autoPlace(held.getItemInstance()) != null;
    }

    // Receive \\

    // A new item lands where the cursor points as if carried there and let go, false when it lands nowhere
    boolean receive(InventorySessionStruct session, ItemInstance itemInstance, float x, float y) {

        if (session.isHolding())
            return false;

        InventoryHeldStruct held = new InventoryHeldStruct(itemInstance);

        session.setHeld(held);
        resolveDrop(session, x, y);
        session.setHeld(null);

        boolean landed = land(session, held, x, y);

        session.clearDrop();

        return landed;
    }

    // Wears the carried item, trading places with an item already worn there when it fits where this one came from
    private boolean equipInto(
            InventorySessionStruct session,
            InventoryHeldStruct held,
            EquipmentSlot equipmentSlot) {

        InventoryHandle inventory = session.getInventory();
        ItemInstance itemInstance = held.getItemInstance();
        ItemInstance occupant = inventory.unequip(equipmentSlot);

        if (!inventory.canEquip(equipmentSlot, itemInstance)) {
            restore(inventory, equipmentSlot, occupant);
            return false;
        }

        if (occupant != null && !placeAtSource(session, held, occupant)) {
            restore(inventory, equipmentSlot, occupant);
            return false;
        }

        inventory.equip(equipmentSlot, itemInstance);

        return true;
    }

    private void restore(InventoryHandle inventory, EquipmentSlot equipmentSlot, ItemInstance occupant) {

        if (occupant != null)
            inventory.equip(equipmentSlot, occupant);
    }

    private boolean placeAtSource(
            InventorySessionStruct session,
            InventoryHeldStruct held,
            ItemInstance itemInstance) {

        if (!held.hasSource()) {

            ContainerInstance backpack = session.getInventory().getBackpackContainer();

            return backpack != null && backpack.autoPlace(itemInstance) != null;
        }

        if (held.isFromSlot()) {

            InventoryHandle inventory = session.getInventory();

            if (!inventory.canEquip(held.getSourceSlot(), itemInstance))
                return false;

            inventory.equip(held.getSourceSlot(), itemInstance);
            return true;
        }

        ContainerInstance containerInstance = held.getSourceContainer();

        if (containerInstance.accepts(itemInstance) && containerInstance.fits(
                itemInstance, held.getSourceX(), held.getSourceY(), held.getSourceZ(), held.getSourceRotation())) {
            containerInstance.place(
                    itemInstance, held.getSourceX(), held.getSourceY(), held.getSourceZ(), held.getSourceRotation());
            return true;
        }

        return containerInstance.autoPlace(itemInstance) != null;
    }

    // The place an item was picked up from stays free while it is carried, so it always fits back
    private void returnToSource(InventorySessionStruct session, InventoryHeldStruct held) {

        if (placeAtSource(session, held, held.getItemInstance()) || session.getInventory().give(held.getItemInstance()))
            return;

        throwException("Carried item '" + held.getItemInstance().getItemDefinitionHandle().getItemName()
                + "' could not be returned to where it was picked up.");
    }

    void cancel(InventorySessionStruct session) {

        if (session.isHolding()) {
            InventoryHeldStruct held = session.getHeld();
            session.setHeld(null);
            returnToSource(session, held);
        }

        session.clearDrop();
        session.setDragMode(InventoryDragMode.NONE);
    }

    // Targets \\

    private EquipmentSlot findSlotAt(InventorySessionStruct session, float x, float y) {

        if (!session.hasEquipment())
            return null;

        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES)
            if (InventoryViewUtility.isInside(session.getSlotElement(equipmentSlot), x, y))
                return equipmentSlot;

        return null;
    }

    private InventoryViewStruct findListAt(InventorySessionStruct session, float x, float y) {

        for (InventoryViewStruct view : session.getViews())
            if (view.isOpen() && view.hasList() && view.getListMenu().isVisible()
                    && InventoryViewUtility.isInside(view.getListElement(), x, y))
                return view;

        return null;
    }

    private ItemInstance resolveHoveredItem(InventorySessionStruct session, float x, float y) {

        if (session.isHolding())
            return session.getHeld().getItemInstance();

        EquipmentSlot equipmentSlot = findSlotAt(session, x, y);

        if (equipmentSlot != null)
            return session.getInventory().getItem(equipmentSlot);

        InventoryViewStruct listView = findListAt(session, x, y);

        if (listView != null)
            return findRowItemAt(listView, x, y);

        for (InventoryViewStruct view : session.getViews()) {

            ContainerSlotStruct slot = inventoryContainerBranch.pickSlot(session, view, x, y);

            if (slot != null)
                return slot.getItemInstance();
        }

        return null;
    }

    private ItemInstance findRowItemAt(InventoryViewStruct view, float x, float y) {

        for (int i = 0; i < view.getRowElements().size(); i++) {

            ElementInstance row = view.getRowElements().get(i);

            if (InventoryViewUtility.isInside(row, x, y))
                return view.getRowItems().get(i);
        }

        return null;
    }
}
