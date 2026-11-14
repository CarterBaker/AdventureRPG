package application.bootstrap.weatherpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.NoiseUtility;
import engine.util.mathematics.extras.SeamlessAxisNoiseUtility;

public final class WeatherNoiseUtility extends EngineUtility {

    /*
     * The static weather image. A pure function of a noise-space position in
     * chunks, seamless across both world axes: X is embedded on a circle so a
     * full lap lands on the same value, Z wraps through the shared
     * SeamlessAxisNoiseUtility. Nothing here moves — the weather pipeline
     * scrolls this image across the world by offsetting where it is read.
     */

    // Sample \\

    public static float sample(
            long seed,
            double noiseChunkX,
            double noiseChunkZ,
            double worldWidthChunks,
            double worldHeightChunks,
            double wavelengthChunks) {

        double effectiveWavelength = Math.min(
                wavelengthChunks,
                Math.max(
                        worldWidthChunks / EngineSetting.WEATHER_NOISE_MIN_CYCLES_AROUND_WORLD,
                        EngineSetting.DIVISION_EPSILON));

        double angle = (noiseChunkX / worldWidthChunks) * (Math.PI * 2.0);
        double embeddingRadius = worldWidthChunks / (Math.PI * 2.0 * effectiveWavelength);
        double ex = Math.cos(angle) * embeddingRadius;
        double ey = Math.sin(angle) * embeddingRadius;

        double zWavelength = effectiveWavelength / EngineSetting.WEATHER_NOISE_CROSS_STREAM_COMPRESSION;

        float raw = SeamlessAxisNoiseUtility.sample(
                noiseChunkZ, zWavelength, worldHeightChunks, EngineSetting.NOISE_SEAM_BLEND_WAVELENGTHS,
                ez -> layeredNoise(seed, ex, ey, ez));

        return clamp01(raw * 0.5f + 0.5f);
    }

    // Internal \\

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

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
