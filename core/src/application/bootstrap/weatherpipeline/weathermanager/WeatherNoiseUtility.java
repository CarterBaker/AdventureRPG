// WeatherNoiseUtility.java
package application.bootstrap.weatherpipeline.weathermanager;

import engine.root.EngineSetting;
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
     *
     * All shaping constants (frequencies, weights, the two hash-mixing
     * salts) live on EngineSetting's WEATHER_NOISE_* / WEATHER_HASH_SALT_*
     * fields — nothing tunable is declared locally in this file.
     */

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
                Math.max(worldWidthChunks / EngineSetting.WEATHER_NOISE_MIN_CYCLES_AROUND_WORLD, 0.001));

        double spatialAngle = (chunkX / worldWidthChunks) * (Math.PI * 2.0);
        double angle = spatialAngle + rotationPhase;

        double embeddingRadius = worldWidthChunks / (Math.PI * 2.0 * effectiveWavelength);
        double ex = Math.cos(angle) * embeddingRadius;
        double ey = Math.sin(angle) * embeddingRadius;

        double meander = Math.sin(spatialAngle * meanderWaveNumber + meanderPhase) * meanderAmplitudeChunks;
        double zWavelength = effectiveWavelength / EngineSetting.WEATHER_NOISE_CROSS_STREAM_COMPRESSION;
        double rawZ = chunkZ + driftZ + meander;

        float raw = seamlessZ(seed, ex, ey, rawZ, zWavelength, worldHeightChunks);

        return clamp01(raw * 0.5f + 0.5f);
    }

    /*
     * Wraps rawZ into [0, worldHeightChunks) and cross-fades the layered
     * noise there with the same noise taken one world-height back. At
     * z=0 the blend sits entirely on the direct sample; at
     * z=worldHeightChunks it sits entirely on the "one world-height back"
     * sample — and those two samples are literally the same noise call
     * (both land on ez=0), so the two edges of the world always read
     * identically.
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
                seed ^ EngineSetting.WEATHER_HASH_SALT_PRIMARY,
                ex * EngineSetting.WEATHER_NOISE_MACRO_FREQUENCY,
                ey * EngineSetting.WEATHER_NOISE_MACRO_FREQUENCY,
                ez * EngineSetting.WEATHER_NOISE_MACRO_FREQUENCY);
        float base = NoiseUtility.noise3_ImproveXY(seed, ex, ey, ez);
        float detail = NoiseUtility.noise3_ImproveXY(
                seed ^ EngineSetting.WEATHER_HASH_SALT_SECONDARY,
                ex * EngineSetting.WEATHER_NOISE_DETAIL_FREQUENCY,
                ey * EngineSetting.WEATHER_NOISE_DETAIL_FREQUENCY,
                ez * EngineSetting.WEATHER_NOISE_DETAIL_FREQUENCY);

        float combined = base + macro * EngineSetting.WEATHER_NOISE_MACRO_WEIGHT
                + detail * EngineSetting.WEATHER_NOISE_DETAIL_WEIGHT;

        return combined
                / (1f + EngineSetting.WEATHER_NOISE_MACRO_WEIGHT + EngineSetting.WEATHER_NOISE_DETAIL_WEIGHT);
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