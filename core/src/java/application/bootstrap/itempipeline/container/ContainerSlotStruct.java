package application.bootstrap.itempipeline.container;

import application.bootstrap.itempipeline.item.ItemInstance;
import engine.root.StructPackage;

public class ContainerSlotStruct extends StructPackage {

    /*
     * One item resting inside a container: the item, the sub-voxel its
     * rotated shape's minimum corner sits on, and its quarter turns about the
     * vertical axis. Moving an item replaces its slot rather than editing it.
     */

    // Item
    private final ItemInstance itemInstance;

    // Placement
    private final int x;
    private final int y;
    private final int z;
    private final int rotation;

    // Constructor \\

    public ContainerSlotStruct(ItemInstance itemInstance, int x, int y, int z, int rotation) {

        // Item
        this.itemInstance = itemInstance;

        // Placement
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotation = rotation;
    }

    // Accessible \\

    public ItemInstance getItemInstance() {
        return itemInstance;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getRotation() {
        return rotation;
    }
}
