package application.bootstrap.shipwrightpipeline.util;

import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class ShipRopeUtility extends EngineUtility {

    /*
     * The one way a run of rope is laid into a ship's part: two ribbons a
     * rope's breadth wide crossed along the line between its ends, flat quads
     * the rope's texture is drawn on and never sub-voxels, so a stay or a
     * cable reads as rope from any side for four faces. A rope hanging
     * steeper than it runs is crossed upright, one running flatter lies its
     * second ribbon flat.
     */

    // Rope \\

    public static void addRope(VehiclePartStruct part, int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {

        int half = EngineSetting.SHIP_ROPE_HALF_WIDTH;
        boolean upright = Math.abs(toY - fromY) >= Math.abs(toX - fromX);
        int spreadX = upright ? half : 0;
        int spreadY = upright ? 0 : half;

        part.addQuad(EngineSetting.SUB_VOXEL_FACE_POSITIVE_Z,
                fromX - spreadX, fromY + spreadY, fromZ,
                toX - spreadX, toY + spreadY, toZ,
                toX + spreadX, toY - spreadY, toZ,
                fromX + spreadX, fromY - spreadY, fromZ);
        part.addQuad(upright ? EngineSetting.SUB_VOXEL_FACE_POSITIVE_X : EngineSetting.SUB_VOXEL_FACE_POSITIVE_Y,
                fromX, fromY, fromZ - half,
                toX, toY, toZ - half,
                toX, toY, toZ + half,
                fromX, fromY, fromZ + half);
    }
}
