package application.bootstrap.worldpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.NoiseUtility;
import engine.util.mathematics.extras.SeamlessAxisNoiseUtility;

public final class TerrainWrapNoiseUtility extends EngineUtility {

    /*
     * Fractal (multi-octave) noise sampling for terrain, wrapped seamlessly
     * across the world the same way WeatherNoiseUtility wraps weather: world
     * X rides a circle embedded in 3D noise space so a full lap always lands
     * back on the same value, and world Z wraps through the shared
     * SeamlessAxisNoiseUtility, which blends only within a thin margin of
     * the true seam rather than across the entire world, so every octave
     * away from that seam costs exactly one noise sample. Terrain never
     * drifts or rotates over time — every input here is a fixed world
     * position, so the result is fully deterministic from seed and
     * coordinate alone.
     */

    private TerrainWrapNoiseUtility() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    static float sampleFractal(
            long seed,
            double worldX, double worldZ,
            double worldWidthBlocks, double worldHeightBlocks,
            double baseWavelengthBlocks,
            int octaves, float persistence, float lacunarity) {

        float amplitude = 1f;
        float amplitudeSum = 0f;
        float sum = 0f;
        double wavelength = baseWavelengthBlocks;

        for (int octave = 0; octave < octaves; octave++) {

            long octaveSeed = seed ^ (EngineSetting.TERRAIN_OCTAVE_HASH_SALT * (octave * 2L + 1L));

            sum += sampleSingle(octaveSeed, worldX, worldZ, worldWidthBlocks, worldHeightBlocks, wavelength)
                    * amplitude;
            amplitudeSum += amplitude;

            amplitude *= persistence;
            wavelength /= lacunarity;
        }

        return amplitudeSum > 0f ? sum / amplitudeSum : 0f;
    }

    private static float sampleSingle(
            long seed, double worldX, double worldZ,
            double worldWidthBlocks, double worldHeightBlocks, double wavelengthBlocks) {

        double effectiveWavelength = Math.max(wavelengthBlocks, 0.001);

        double spatialAngle = (worldX / worldWidthBlocks) * (Math.PI * 2.0);
        double embeddingRadius = worldWidthBlocks / (Math.PI * 2.0 * effectiveWavelength);

        double ex = Math.cos(spatialAngle) * embeddingRadius;
        double ey = Math.sin(spatialAngle) * embeddingRadius;

        return SeamlessAxisNoiseUtility.sample(
                worldZ, effectiveWavelength, worldHeightBlocks, EngineSetting.NOISE_SEAM_BLEND_WAVELENGTHS,
                ez -> NoiseUtility.noise3_ImproveXY(seed, ex, ey, ez));
    }
}