package application.bootstrap.weatherpipeline.weathermanager;

import engine.root.EngineUtility;
import engine.util.mathematics.extras.NoiseUtility;

/*
 * Continuous 2D weather noise field. chunkX wraps seamlessly around the
 * world by walking a circle in 2D noise space as the planet rotates.
 * chunkZ is never wrapped — it only shifts that circle's radius, giving
 * the field a slow, non-repeating vertical drift instead of a second
 * toroidal axis. A second, higher-frequency octave on the same circle
 * stretches the result into long current-like strands.
 */
final class WeatherNoiseUtility extends EngineUtility {

    private static final double MIN_CYCLES_AROUND_WORLD = 4.0;
    private static final double DETAIL_FREQUENCY = 2.4;
    private static final float DETAIL_WEIGHT = 0.35f;
    private static final long DETAIL_SEED_MIX = 0x9E3779B97F4A7C15L;

    private WeatherNoiseUtility() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    static float sample(
            long seed,
            double chunkX, double chunkZ,
            double worldWidthChunks,
            double wavelengthChunks,
            double rotationPhase,
            double driftZ) {

        double effectiveWavelength = Math.min(
                wavelengthChunks,
                Math.max(worldWidthChunks / MIN_CYCLES_AROUND_WORLD, 0.001));

        double angle = (chunkX / worldWidthChunks) * (Math.PI * 2.0) + rotationPhase;
        double radius = (worldWidthChunks / (Math.PI * 2.0 * effectiveWavelength))
                + ((chunkZ + driftZ) / effectiveWavelength);

        double x = Math.cos(angle) * radius;
        double y = Math.sin(angle) * radius;

        float base = NoiseUtility.noise2(seed, x, y);
        float detail = NoiseUtility.noise2(seed ^ DETAIL_SEED_MIX, x * DETAIL_FREQUENCY, y * DETAIL_FREQUENCY);

        float raw = base + detail * DETAIL_WEIGHT;

        return clamp01(raw * 0.5f + 0.5f);
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}