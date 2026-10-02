package application.bootstrap.vehiclepipeline.vehicle;

import application.bootstrap.itempipeline.item.ItemInstance;
import engine.root.InstancePackage;

public class VehicleCargoInstance extends InstancePackage {

    /*
     * One item set down aboard a vehicle: the real item, contents and all, the
     * corner of its model grid in the vehicle's model sub-voxels, its packed
     * orientation, and the rough box around every cell its turned shape
     * claims, and whether it stands open. It lives in the vehicle's own
     * frame, so it stays exactly where it was set down wherever the vehicle
     * sails.
     */

    // Internal
    private ItemInstance itemInstance;

    // Placement
    private int cornerX;
    private int cornerY;
    private int cornerZ;
    private int orientation;

    // Bounds — model sub-voxels, minimum inclusive and maximum exclusive
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;

    // State
    private boolean open;

    // Constructor \\

    public void constructor(ItemInstance itemInstance, int cornerX, int cornerY, int cornerZ, int orientation) {

        // Internal
        this.itemInstance = itemInstance;

        // Placement
        this.cornerX = cornerX;
        this.cornerY = cornerY;
        this.cornerZ = cornerZ;
        this.orientation = orientation;
    }

    // Bounds \\

    public void setBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    // State \\

    public void setOpen(boolean open) {
        this.open = open;
    }

    // Accessible \\

    public ItemInstance getItemInstance() {
        return itemInstance;
    }

    public int getCornerX() {
        return cornerX;
    }

    public int getCornerY() {
        return cornerY;
    }

    public int getCornerZ() {
        return cornerZ;
    }

    public int getOrientation() {
        return orientation;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public boolean isOpen() {
        return open;
    }
}
