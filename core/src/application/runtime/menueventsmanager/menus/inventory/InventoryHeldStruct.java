package application.runtime.menueventsmanager.menus.inventory;

import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.itempipeline.container.ContainerInstance;
import application.bootstrap.itempipeline.item.ItemInstance;
import engine.root.StructPackage;

public class InventoryHeldStruct extends StructPackage {

    /*
     * The item a player is carrying under the cursor, and where it came from
     * — an equipment slot, or a container and the exact place it rested — so
     * a drop that lands nowhere puts it back. The item has already left its
     * source, so the space it took is free while it is carried. A grab
     * offset keeps an item picked out of a container view under the same
     * point of the cursor while it moves, until it is turned.
     */

    // Item
    private final ItemInstance itemInstance;
    private int rotation;

    // Source — a slot, or a container and its place in it
    private final EquipmentSlot sourceSlot;
    private final ContainerInstance sourceContainer;
    private final int sourceX;
    private final int sourceY;
    private final int sourceZ;
    private final int sourceRotation;

    // Grab
    private boolean grabbed;
    private float grabOffsetX;
    private float grabOffsetZ;

    // Constructor \\

    public InventoryHeldStruct(ItemInstance itemInstance, EquipmentSlot sourceSlot) {

        // Item
        this.itemInstance = itemInstance;

        // Source
        this.sourceSlot = sourceSlot;
        this.sourceContainer = null;
        this.sourceX = 0;
        this.sourceY = 0;
        this.sourceZ = 0;
        this.sourceRotation = 0;
    }

    public InventoryHeldStruct(
            ItemInstance itemInstance,
            ContainerInstance sourceContainer,
            int sourceX,
            int sourceY,
            int sourceZ,
            int sourceRotation) {

        // Item
        this.itemInstance = itemInstance;
        this.rotation = sourceRotation;

        // Source
        this.sourceSlot = null;
        this.sourceContainer = sourceContainer;
        this.sourceX = sourceX;
        this.sourceY = sourceY;
        this.sourceZ = sourceZ;
        this.sourceRotation = sourceRotation;
    }

    // Rotation \\

    public void turn() {
        this.rotation++;
        this.grabbed = false;
    }

    // Grab \\

    public void setGrabOffset(float grabOffsetX, float grabOffsetZ) {
        this.grabbed = true;
        this.grabOffsetX = grabOffsetX;
        this.grabOffsetZ = grabOffsetZ;
    }

    // Accessible \\

    public ItemInstance getItemInstance() {
        return itemInstance;
    }

    public int getRotation() {
        return rotation;
    }

    public boolean isFromSlot() {
        return sourceSlot != null;
    }

    public EquipmentSlot getSourceSlot() {
        return sourceSlot;
    }

    public ContainerInstance getSourceContainer() {
        return sourceContainer;
    }

    public int getSourceX() {
        return sourceX;
    }

    public int getSourceY() {
        return sourceY;
    }

    public int getSourceZ() {
        return sourceZ;
    }

    public int getSourceRotation() {
        return sourceRotation;
    }

    public boolean isGrabbed() {
        return grabbed;
    }

    public float getGrabOffsetX() {
        return grabOffsetX;
    }

    public float getGrabOffsetZ() {
        return grabOffsetZ;
    }
}
