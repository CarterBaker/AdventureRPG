package application.bootstrap.vehiclepipeline.vehicle;

import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class VehicleCargoSlotStruct extends StructPackage {

    /*
     * One place a vehicle comes furnished: the table of items that may stand
     * there, the corner of the chosen item's model grid in the vehicle's
     * model sub-voxels, its packed orientation, and the chance the place is
     * furnished at all. Each vehicle that enters the world draws one item
     * from the table for every place it is furnished at.
     */

    // Table
    private final String tableName;
    private final ObjectArrayList<String> itemNames;

    // Placement
    private final int cornerX;
    private final int cornerY;
    private final int cornerZ;
    private final int orientation;

    // Chance
    private final float chance;

    // Constructor \\

    public VehicleCargoSlotStruct(
            String tableName,
            ObjectArrayList<String> itemNames,
            int cornerX,
            int cornerY,
            int cornerZ,
            int orientation,
            float chance) {

        // Table
        this.tableName = tableName;
        this.itemNames = itemNames;

        // Placement
        this.cornerX = cornerX;
        this.cornerY = cornerY;
        this.cornerZ = cornerZ;
        this.orientation = orientation;

        // Chance
        this.chance = chance;
    }

    // Accessible \\

    public String getTableName() {
        return tableName;
    }

    public int getItemCount() {
        return itemNames.size();
    }

    public String getItemName(int index) {
        return itemNames.get(index);
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

    public float getChance() {
        return chance;
    }
}
