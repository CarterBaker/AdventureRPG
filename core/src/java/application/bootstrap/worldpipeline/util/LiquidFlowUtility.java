package application.bootstrap.worldpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class LiquidFlowUtility extends EngineUtility {

    /*
     * The one place the way a column's water runs becomes the code its water
     * faces carry to the water shader: STILL for water that stands, one of
     * LIQUID_FLOW_DIRECTIONS directions round from east toward south for
     * water that runs, CHURN for water a fall pours onto from beside, and
     * FALL for the upright face water pours out through. A
     * direction is the nearest of the ring to the way given, so a river's
     * bends turn its faces' codes a step at a time and long straight reaches
     * still merge into wide faces. It is also the one place the foot of a
     * fall is packed into the word its subchunk keeps for the particles
     * thrown up there: the cell poured onto, the pool face's extents, the
     * drop and the side the water pours from.
     */

    // Settings
    public static final byte STILL = EngineSetting.LIQUID_FLOW_STILL;
    public static final byte CHURN = EngineSetting.LIQUID_FLOW_CHURN;
    public static final byte FALL = EngineSetting.LIQUID_FLOW_FALL;

    // Code \\

    // The code of water running the way given in the world's x and z, still when it runs nowhere
    public static byte toCode(float flowX, float flowZ) {

        if (flowX == 0f && flowZ == 0f)
            return STILL;

        double turns = Math.atan2(flowZ, flowX) / (Math.PI * 2.0);
        int direction = (int) Math.floorMod(
                Math.round(turns * EngineSetting.LIQUID_FLOW_DIRECTIONS), (long) EngineSetting.LIQUID_FLOW_DIRECTIONS);

        return (byte) (direction + 1);
    }

    public static boolean isRunning(byte code) {
        return code > STILL && code <= EngineSetting.LIQUID_FLOW_DIRECTIONS;
    }

    // Fall Foot \\

    // The foot of a fall as one word: its cell in the subchunk, the pool face's extents along x and z, the drop held
    // to LIQUID_FALL_MAX_BLOCKS and the lateral side (a Direction3Vector index) the water pours from; never
    // LIQUID_FALL_FOOT_NONE, since the drop is always at least one block
    public static int packFoot(int x, int y, int z, int sizeX, int sizeZ, int drop, int side) {
        return x
                | (y << EngineSetting.LIQUID_FALL_FOOT_Y_SHIFT)
                | (z << EngineSetting.LIQUID_FALL_FOOT_Z_SHIFT)
                | ((sizeX - 1) << EngineSetting.LIQUID_FALL_FOOT_SIZE_X_SHIFT)
                | ((sizeZ - 1) << EngineSetting.LIQUID_FALL_FOOT_SIZE_Z_SHIFT)
                | (clampDrop(drop) << EngineSetting.LIQUID_FALL_FOOT_DROP_SHIFT)
                | (side << EngineSetting.LIQUID_FALL_FOOT_SIDE_SHIFT);
    }

    public static int clampDrop(int drop) {
        return Math.max(1, Math.min(drop, EngineSetting.LIQUID_FALL_MAX_BLOCKS));
    }

    public static int getFootX(int foot) {
        return foot & EngineSetting.LIQUID_FALL_FOOT_CELL_MASK;
    }

    public static int getFootY(int foot) {
        return (foot >>> EngineSetting.LIQUID_FALL_FOOT_Y_SHIFT) & EngineSetting.LIQUID_FALL_FOOT_CELL_MASK;
    }

    public static int getFootZ(int foot) {
        return (foot >>> EngineSetting.LIQUID_FALL_FOOT_Z_SHIFT) & EngineSetting.LIQUID_FALL_FOOT_CELL_MASK;
    }

    public static int getFootSizeX(int foot) {
        return ((foot >>> EngineSetting.LIQUID_FALL_FOOT_SIZE_X_SHIFT) & EngineSetting.LIQUID_FALL_FOOT_CELL_MASK) + 1;
    }

    public static int getFootSizeZ(int foot) {
        return ((foot >>> EngineSetting.LIQUID_FALL_FOOT_SIZE_Z_SHIFT) & EngineSetting.LIQUID_FALL_FOOT_CELL_MASK) + 1;
    }

    public static int getFootDrop(int foot) {
        return (foot >>> EngineSetting.LIQUID_FALL_FOOT_DROP_SHIFT) & EngineSetting.LIQUID_FALL_MAX_BLOCKS;
    }

    public static int getFootSide(int foot) {
        return (foot >>> EngineSetting.LIQUID_FALL_FOOT_SIDE_SHIFT) & EngineSetting.LIQUID_FALL_FOOT_SIDE_MASK;
    }
}
