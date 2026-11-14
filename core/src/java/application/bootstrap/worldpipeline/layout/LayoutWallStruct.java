package application.bootstrap.worldpipeline.layout;

import application.bootstrap.worldpipeline.architecture.ArchitectureWallStruct;
import application.bootstrap.worldpipeline.road.RoadPathStruct;
import engine.root.StructPackage;

public class LayoutWallStruct extends StructPackage {

    /*
     * One curtain wall a layout raises: its centreline planned against the
     * land like a road, so its foot rolls with the ground, the wall it is
     * built as, and the stretches along it, by distance along the
     * centreline, left open as gates where a road passes through.
     */

    // Wall
    private final RoadPathStruct centerline;
    private final ArchitectureWallStruct wall;

    // Gates — distance along the centreline, starts and ends paired
    private final float[] gateStarts;
    private final float[] gateEnds;

    // Constructor \\

    public LayoutWallStruct(
            RoadPathStruct centerline,
            ArchitectureWallStruct wall,
            float[] gateStarts,
            float[] gateEnds) {

        // Wall
        this.centerline = centerline;
        this.wall = wall;

        // Gates
        this.gateStarts = gateStarts;
        this.gateEnds = gateEnds;
    }

    // Accessible \\

    public RoadPathStruct getCenterline() {
        return centerline;
    }

    public ArchitectureWallStruct getWall() {
        return wall;
    }

    // True when a distance along the centreline falls in a gate
    public boolean isGate(float along) {

        for (int i = 0; i < gateStarts.length; i++)
            if (along >= gateStarts[i] && along <= gateEnds[i])
                return true;

        return false;
    }
}
