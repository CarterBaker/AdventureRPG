package application.bootstrap.weatherpipeline.util;

import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public final class WeatherScaleUtility extends EngineUtility {

    /*
     * The world is a scaled planet. Its scale against a real planet is
     * measured from its own circumference, so the weather pipeline's real-world
     * flow speeds in kph convert into blocks the same way. Clouds are sized
     * against the world's terrain instead: a cloud's breadth and thickness
     * take a fixed number of blocks per kilometre, large enough to read as
     * real clouds over the world, while altitudes keep the real spacing
     * between archetypes at a twentieth of real scale, above a floor high
     * enough that only mountains reach into the clouds.
     */

    // Scale \\

    public static double resolveWorldScaleRatio(WorldHandle worldHandle) {
        double circumferenceMeters = worldHandle.getWorldScale().x * (double) EngineSetting.BLOCK_SIZE;
        return circumferenceMeters / EngineSetting.WEATHER_REFERENCE_CIRCUMFERENCE_METERS;
    }

    public static double kphToBlocksPerSecond(WorldHandle worldHandle, double kph) {
        return kph * EngineSetting.KPH_TO_METERS_PER_SECOND * resolveWorldScaleRatio(worldHandle)
                / EngineSetting.BLOCK_SIZE;
    }

    // Clouds \\

    public static float cloudKilometersToBlocks(double kilometers) {
        return (float) (kilometers * EngineSetting.CLOUD_BLOCKS_PER_KILOMETER);
    }

    // Height above sea level of a cloud base authored at an altitude in kilometres.
    public static float cloudAltitudeToBlocks(double kilometers) {
        return (float) (EngineSetting.CLOUD_ALTITUDE_FLOOR_BLOCKS
                + kilometers * EngineSetting.CLOUD_ALTITUDE_BLOCKS_PER_KILOMETER);
    }
}
