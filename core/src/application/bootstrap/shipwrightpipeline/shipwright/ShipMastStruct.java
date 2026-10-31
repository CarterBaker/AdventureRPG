package application.bootstrap.shipwrightpipeline.shipwright;

import engine.root.StructPackage;

public class ShipMastStruct extends StructPackage {

    /*
     * One mast of a ship blueprint: the octant along the length its aft face
     * stands on, on the centre line. Everything else about it — how tall it
     * rises, how many sails it carries, its crow's nest and its rigging —
     * follows from the hull it is stepped in and where along it it stands.
     */

    // Placement
    private int x;

    // Constructor \\

    public ShipMastStruct(int x) {

        // Placement
        this.x = x;
    }

    public ShipMastStruct copy() {
        return new ShipMastStruct(x);
    }

    // Management \\

    public void setX(int x) {
        this.x = x;
    }

    // Accessible \\

    public int getX() {
        return x;
    }
}
