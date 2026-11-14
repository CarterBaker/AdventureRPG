package application.bootstrap.worldpipeline.tree;

import engine.root.StructPackage;

public class TreeLogStruct extends StructPackage {

    /*
     * One size of log a felled species splits into: the item it drops, the
     * thinnest wood it is cut from in sub-voxels of radius, and the length of
     * wood one log takes up. The item's ID is resolved once at load.
     */

    private final String itemName;
    private final int itemID;
    private final int minRadiusSubVoxels;
    private final float lengthBlocks;

    public TreeLogStruct(
            String itemName,
            int itemID,
            int minRadiusSubVoxels,
            float lengthBlocks) {

        this.itemName = itemName;
        this.itemID = itemID;
        this.minRadiusSubVoxels = minRadiusSubVoxels;
        this.lengthBlocks = lengthBlocks;
    }

    // Accessible \\

    public String getItemName() {
        return itemName;
    }

    public int getItemID() {
        return itemID;
    }

    public int getMinRadiusSubVoxels() {
        return minRadiusSubVoxels;
    }

    public float getLengthBlocks() {
        return lengthBlocks;
    }
}
