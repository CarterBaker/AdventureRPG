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
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.settings.KeyBindings;
import engine.util.mathematics.vectors.Vector3;

public class InventoryDragBranch extends BranchPackage {

    /*
     * Carries items under the cursor. Picking an item up takes it out of its
     * slot or container at once, so the space it filled is free while it
     * moves. Over a container view the carried item shows where it would
     * land: it is dropped in from the top at the cursor's point on the floor
     * and falls until it rests, and the Rotate Item key turns it a quarter.
     * Letting go over a slot wears or holds it — trading places with an item
     * already there when that item fits where the carried one came from —
     * over a view drops it where shown, over a list packs it wherever it
     * fits, and anywhere else returns it. An item is never lost: whatever
     * cannot land goes back where it came from. The item under the cursor is
     * found here too, for the details panel.
     */

    // Internal
    private InputManager inputManager;
    private InventoryContainerBranch inventoryContainerBranch;

    // Scratch
    private Vector3 floorPoint;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.floorPoint = new Vector3();
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

        if (!inputManager.getRawInput(window).isMouseDown(0)) {

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

        if (inventoryContainerBranch.pickFloor(view, x, y, floorPoint))
            held.setGrabOffset(slot.getX() - floorPoint.x, slot.getZ() - floorPoint.z);

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

        if (!session.isHolding())
            return;

        for (InventoryViewStruct view : session.getViews())
            if (view.isOpen() && InventoryViewUtility.isInside(view.getViewElement(), x, y)) {
                resolveViewDrop(session, view, x, y);
                return;
            }
    }

    // The carried item enters at the cursor's point on the floor and falls until it rests
    private void resolveViewDrop(InventorySessionStruct session, InventoryViewStruct view, float x, float y) {

        if (!inventoryContainerBranch.pickFloor(view, x, y, floorPoint))
            return;

        InventoryHeldStruct held = session.getHeld();
        ItemInstance itemInstance = held.getItemInstance();
        ItemShapeStruct shape = itemInstance.getItemDefinitionHandle().getShape();
        ContainerInstance containerInstance = view.getContainerInstance();
        int rotation = held.getRotation();
        int sizeX = shape.getRotatedSizeX(rotation);
        int sizeZ = shape.getRotatedSizeZ(rotation);

        float cornerX = held.isGrabbed() ? floorPoint.x + held.getGrabOffsetX() : floorPoint.x - sizeX * 0.5f;
        float cornerZ = held.isGrabbed() ? floorPoint.z + held.getGrabOffsetZ() : floorPoint.z - sizeZ * 0.5f;
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

        EquipmentSlot equipmentSlot = findSlotAt(session, x, y);

        if (equipmentSlot != null) {

            if (!equipInto(session, held, equipmentSlot))
                returnToSource(session, held);

            return;
        }

        if (session.hasDrop()) {

            if (session.isDropValid())
                session.getDropView().getContainerInstance().place(
                        held.getItemInstance(),
                        session.getDropX(),
                        session.getDropY(),
                        session.getDropZ(),
                        held.getRotation());
            else
                returnToSource(session, held);

            return;
        }

        InventoryViewStruct listView = findListAt(session, x, y);

        if (listView == null || listView.getContainerInstance().autoPlace(held.getItemInstance()) == null)
            returnToSource(session, held);
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

        for (EquipmentSlot equipmentSlot : EquipmentSlot.values())
            if (InventoryViewUtility.isInside(session.getSlotElement(equipmentSlot), x, y))
                return equipmentSlot;

        return null;
    }

    private InventoryViewStruct findListAt(InventorySessionStruct session, float x, float y) {

        for (InventoryViewStruct view : session.getViews())
            if (view.isOpen() && InventoryViewUtility.isInside(view.getListElement(), x, y))
                return view;

        return null;
    }

    private ItemInstance resolveHoveredItem(InventorySessionStruct session, float x, float y) {

        if (session.isHolding())
            return session.getHeld().getItemInstance();

        EquipmentSlot equipmentSlot = findSlotAt(session, x, y);

        if (equipmentSlot != null)
            return session.getInventory().getItem(equipmentSlot);

        for (InventoryViewStruct view : session.getViews()) {

            if (!view.isOpen())
                continue;

            if (InventoryViewUtility.isInside(view.getListElement(), x, y))
                return findRowItemAt(view, x, y);

            ContainerSlotStruct slot = inventoryContainerBranch.pickSlot(view, x, y);

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
