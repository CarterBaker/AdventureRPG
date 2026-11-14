package application.bootstrap.shipwrightpipeline.shipwright;

import engine.root.StructPackage;

public class ShipFurnitureStruct extends StructPackage {

    /*
     * One piece of furniture set down on a deck of a ship blueprint: the
     * furnishing table it is drawn from and the packed cell it stands on,
     * level, octant along the length and across the full breadth, and the
     * quarter turn it faces. Immutable; moving or turning one replaces it.
     */

    // Identity
    private final String furnishingName;

    // Placement — packed through ShipCellUtility
    private final int cell;

    // Constructor \\

    public ShipFurnitureStruct(String furnishingName, int cell) {

        // Identity
        this.furnishingName = furnishingName;

        // Placement
        this.cell = cell;
    }

    // Accessible \\

    public String getFurnishingName() {
        return furnishingName;
    }

    public int getCell() {
        return cell;
    }
}
