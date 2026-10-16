package application.bootstrap.shipwrightpipeline.shipwright;

import engine.root.StructPackage;

public class ShipMastStruct extends StructPackage {

    /*
     * One mast of a ship blueprint: the octant along the length its aft face
     * stands on, on the centre line, how many octants it rises above the deck
     * it passes through, whether a crow's nest is built round it and whether
     * it is rigged, its shrouds run down to either side and a stay run forward.
     * How many sails it carries follows from its height.
     */

    // Placement
    private int x;
    private int height;

    // Fittings
    private boolean crowsNest;
    private boolean rigging;

    // Constructor \\

    public ShipMastStruct(int x, int height, boolean crowsNest, boolean rigging) {

        // Placement
        this.x = x;
        this.height = height;

        // Fittings
        this.crowsNest = crowsNest;
        this.rigging = rigging;
    }

    public ShipMastStruct copy() {
        return new ShipMastStruct(x, height, crowsNest, rigging);
    }

    // Management \\

    public void setX(int x) {
        this.x = x;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setCrowsNest(boolean crowsNest) {
        this.crowsNest = crowsNest;
    }

    public void setRigging(boolean rigging) {
        this.rigging = rigging;
    }

    // Accessible \\

    public int getX() {
        return x;
    }

    public int getHeight() {
        return height;
    }

    public boolean hasCrowsNest() {
        return crowsNest;
    }

    public boolean isRigged() {
        return rigging;
    }
}
