package application.bootstrap.weatherpipeline.util;

import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public final class WeatherScaleUtility extends EngineUtility {

    /*
     * The world is a scaled planet. Its scale against a real planet is
     * measured from its own circumference, so every real-world figure the
     * weather pipeline is authored in — cloud altitudes, thicknesses and
     * sizes in kilometres, flow speed in kph — converts into blocks the same
     * way, and the sky curves with the planet's own radius.
     */

    // Scale \\

    public static double resolveWorldScaleRatio(WorldHandle worldHandle) {
        double circumferenceMeters = worldHandle.getWorldScale().x * (double) EngineSetting.BLOCK_SIZE;
        return circumferenceMeters / EngineSetting.WEATHER_REFERENCE_CIRCUMFERENCE_METERS;
    }

    public static float kilometersToBlocks(WorldHandle worldHandle, double kilometers) {
        return (float) (kilometers * EngineSetting.METERS_PER_KILOMETER * resolveWorldScaleRatio(worldHandle)
                / EngineSetting.BLOCK_SIZE);
    }

    public static double kphToBlocksPerSecond(WorldHandle worldHandle, double kph) {
        return kph * EngineSetting.KPH_TO_METERS_PER_SECOND * resolveWorldScaleRatio(worldHandle)
                / EngineSetting.BLOCK_SIZE;
    }

    // Planet \\

    public static float resolvePlanetRadiusBlocks(WorldHandle worldHandle) {
        return (float) (worldHandle.getWorldScale().x / (Math.PI * 2.0));
    }
}
