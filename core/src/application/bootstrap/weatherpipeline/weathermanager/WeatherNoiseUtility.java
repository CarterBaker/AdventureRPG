// WeatherNoiseUtility.java
package application.bootstrap.weatherpipeline.weathermanager;

import engine.root.EngineUtility;
import engine.util.mathematics.extras.NoiseUtility;

final class WeatherNoiseUtility extends EngineUtility {

    /*
     * Continuous 2D weather noise field sampled per chunk coordinate.
     * World X wraps by riding the same trick that already makes it scroll
     * with the world's rotation: chunkX is embedded as a point on a
     * circle before sampling, so a full lap always lands back on the same
     * value. World Y has no rotation to embed onto, so it wraps a cheaper
     * way instead — right at the seam, the noise cross-fades into the
     * same reading it gives on the far side of that seam, so crossing it
     * never pops. Both axes stay inside ordinary 3D noise.
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
            double worldHeightChunks,
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
        double zWavelength = effectiveWavelength / CROSS_STREAM_COMPRESSION;
        double rawZ = chunkZ + driftZ + meander;

        float raw = seamlessZ(seed, ex, ey, rawZ, zWavelength, worldHeightChunks);

        return clamp01(raw * 0.5f + 0.5f);
    }

    /*
     * Wraps rawZ into [0, worldHeightChunks) and cross-fades the layered
     * noise there with the same noise taken one world-height back. At
     * z=0 the fade sits entirely on the "one world-height back" sample;
     * at z=worldHeightChunks it sits entirely on the direct sample — and
     * those two samples are literally the same noise call (both land on
     * ez=0), so the two edges of the world always read identically.
     */
    private static float seamlessZ(
            long seed, double ex, double ey, double rawZ, double zWavelength, double worldHeightChunks) {

        if (worldHeightChunks <= 0.0)
            return layeredNoise(seed, ex, ey, rawZ / zWavelength);

        double z = wrapIntoRange(rawZ, worldHeightChunks);

        float direct = layeredNoise(seed, ex, ey, z / zWavelength);
        float oneWorldBack = layeredNoise(seed, ex, ey, (z - worldHeightChunks) / zWavelength);

        float t = (float) (z / worldHeightChunks);

        return direct * (1f - t) + oneWorldBack * t;
    }

    private static float layeredNoise(long seed, double ex, double ey, double ez) {

        float macro = NoiseUtility.noise3_ImproveXY(
                seed ^ MACRO_SEED_MIX, ex * MACRO_FREQUENCY, ey * MACRO_FREQUENCY, ez * MACRO_FREQUENCY);
        float base = NoiseUtility.noise3_ImproveXY(seed, ex, ey, ez);
        float detail = NoiseUtility.noise3_ImproveXY(
                seed ^ DETAIL_SEED_MIX, ex * DETAIL_FREQUENCY, ey * DETAIL_FREQUENCY, ez * DETAIL_FREQUENCY);

        float combined = base + macro * MACRO_WEIGHT + detail * DETAIL_WEIGHT;

        return combined / (1f + MACRO_WEIGHT + DETAIL_WEIGHT);
    }

    private static double wrapIntoRange(double value, double range) {
        double wrapped = value % range;
        if (wrapped < 0)
            wrapped += range;
        return wrapped;
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}