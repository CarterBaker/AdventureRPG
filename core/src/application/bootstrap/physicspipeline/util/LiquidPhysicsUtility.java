package application.bootstrap.physicspipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class LiquidPhysicsUtility extends EngineUtility {

    /*
     * Converts a liquid block's viscosity (Pa·s) into the real-seconds
     * interval its geometry is allowed to redraw at, via
     * EngineSetting.LIQUID_VISCOSITY_TO_FLOW_SECONDS, clamped between
     * EngineSetting.LIQUID_FLOW_INTERVAL_MIN_SECONDS and
     * LIQUID_FLOW_INTERVAL_MAX_SECONDS so a real-world viscosity value always
     * produces a workable engine tick cadence regardless of how small or
     * large that value is.
     */

    public static float getFlowIntervalSeconds(float viscosity) {

        float interval = viscosity * EngineSetting.LIQUID_VISCOSITY_TO_FLOW_SECONDS;

        return Math.max(
                EngineSetting.LIQUID_FLOW_INTERVAL_MIN_SECONDS,
                Math.min(EngineSetting.LIQUID_FLOW_INTERVAL_MAX_SECONDS, interval));
    }
}