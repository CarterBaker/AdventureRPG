package application.bootstrap.weatherpipeline.weathermanager;

import engine.root.EngineUtility;
import engine.util.mathematics.extras.NoiseUtility;

final class WeatherNoiseUtility extends EngineUtility {

    /*
     * Continuous 2D weather noise field sampled per chunk coordinate.
     * Seamless across the world's X wrap and elongated along the same
     * axis the world rotates, so a single noise feature reads as a long
     * moving weather band rather than a round blob.
     */

    private static final double MIN_CYCLES_AROUND_WORLD = 4.0;
    private static final double CROSS_STREAM_COMPRESSION = 3.2;
    private static final double MACRO_FREQUENCY = 0.36;
    private static final float MACRO_WEIGHT = 0.55f;
    private static final double DETAIL_FREQUENCY = 3.2;
    private static final float DETAIL_WEIGHT = 0.26f;
    private static final long MACRO_SEED_MIX = 0x2545F4914F6CDD1DL;
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
            double driftZ,
            double meanderWaveNumber,
            double meanderAmplitudeChunks,
            double meanderPhase) {

        double effectiveWavelength = Math.min(
                wavelengthChunks,
                Math.max(worldWidthChunks / MIN_CYCLES_AROUND_WORLD, 0.001));

        double spatialAngle = (chunkX / worldWidthChunks) * (Math.PI * 2.0);
        double angle = spatialAngle + rotationPhase;

        double embeddingRadius = worldWidthChunks / (Math.PI * 2.0 * effectiveWavelength);
        double ex = Math.cos(angle) * embeddingRadius;
        double ey = Math.sin(angle) * embeddingRadius;

        double meander = Math.sin(spatialAngle * meanderWaveNumber + meanderPhase) * meanderAmplitudeChunks;
        double ez = (chunkZ + driftZ + meander) / (effectiveWavelength / CROSS_STREAM_COMPRESSION);

        float macro = NoiseUtility.noise3_ImproveXY(
                seed ^ MACRO_SEED_MIX, ex * MACRO_FREQUENCY, ey * MACRO_FREQUENCY, ez * MACRO_FREQUENCY);
        float base = NoiseUtility.noise3_ImproveXY(seed, ex, ey, ez);
        float detail = NoiseUtility.noise3_ImproveXY(
                seed ^ DETAIL_SEED_MIX, ex * DETAIL_FREQUENCY, ey * DETAIL_FREQUENCY, ez * DETAIL_FREQUENCY);

        float raw = base + macro * MACRO_WEIGHT + detail * DETAIL_WEIGHT;
        float normalized = raw / (1f + MACRO_WEIGHT + DETAIL_WEIGHT);

        return clamp01(normalized * 0.5f + 0.5f);
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}