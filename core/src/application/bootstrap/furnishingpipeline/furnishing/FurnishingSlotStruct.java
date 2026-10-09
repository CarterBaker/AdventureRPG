package application.bootstrap.furnishingpipeline.furnishing;

import engine.root.StructPackage;

public class FurnishingSlotStruct extends StructPackage {

    /*
     * One place an owner comes furnished: the furnishing table that may stand
     * there, by name and resolved ID, the corner of the chosen item's model
     * grid in its owner's sub-voxels, its packed orientation, and the chance
     * the place is furnished at all. A vehicle and a structure list their
     * places alike and draw one item from the table for each place furnished.
     */

    // Table
    private final String furnishingName;
    private final short furnishingID;

    // Placement
    private final int cornerX;
    private final int cornerY;
    private final int cornerZ;
    private final int orientation;

    // Chance
    private final float chance;

    // Constructor \\

    public FurnishingSlotStruct(
            String furnishingName,
            short furnishingID,
            int cornerX,
            int cornerY,
            int cornerZ,
            int orientation,
            float chance) {

        // Table
        this.furnishingName = furnishingName;
        this.furnishingID = furnishingID;

        // Placement
        this.cornerX = cornerX;
        this.cornerY = cornerY;
        this.cornerZ = cornerZ;
        this.orientation = orientation;

        // Chance
        this.chance = chance;
    }

    // Accessible \\

    public String getFurnishingName() {
        return furnishingName;
    }

    public short getFurnishingID() {
        return furnishingID;
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
