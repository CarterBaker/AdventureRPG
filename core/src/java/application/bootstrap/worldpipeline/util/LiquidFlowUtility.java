package application.bootstrap.worldpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class LiquidFlowUtility extends EngineUtility {

    /*
     * The one place the way a column's water runs becomes the code its water
     * faces carry to the water shader: STILL for water that stands, one of
     * LIQUID_FLOW_DIRECTIONS directions round from east toward south for
     * water that runs, and CHURN for water a fall pours onto from beside. A
     * direction is the nearest of the ring to the way given, so a river's
     * bends turn its faces' codes a step at a time and long straight reaches
     * still merge into wide faces.
     */

    // Settings
    public static final byte STILL = EngineSetting.LIQUID_FLOW_STILL;
    public static final byte CHURN = EngineSetting.LIQUID_FLOW_CHURN;

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
        return code != STILL && code != CHURN;
    }
}
