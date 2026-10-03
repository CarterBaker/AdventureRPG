package application.bootstrap.mappipeline.map;

import application.bootstrap.worldpipeline.grid.GridInstance;
import engine.root.StructPackage;

public class MapMarkerStruct extends StructPackage {

    /*
     * Where one player stands on the world map: the streaming grid that
     * follows them, their position in world blocks, unwrapped, and the
     * direction they face on the ground, a unit vector in x and z. The
     * heading is kept while the player's camera cannot be read, so a marker
     * never snaps back to north.
     */

    // Grid
    private GridInstance grid;

    // Position
    private double x;
    private double z;

    // Heading
    private float headingX;
    private float headingZ;

    // Constructor \\

    public MapMarkerStruct() {
        this.headingZ = -1f;
    }

    // Management \\

    public void setPosition(GridInstance grid, double x, double z) {
        this.grid = grid;
        this.x = x;
        this.z = z;
    }

    public void setHeading(float headingX, float headingZ) {
        this.headingX = headingX;
        this.headingZ = headingZ;
    }

    // Accessible \\

    public GridInstance getGrid() {
        return grid;
    }

    public double getX() {
        return x;
    }

    public double getZ() {
        return z;
    }

    public float getHeadingX() {
        return headingX;
    }

    public float getHeadingZ() {
        return headingZ;
    }
}
