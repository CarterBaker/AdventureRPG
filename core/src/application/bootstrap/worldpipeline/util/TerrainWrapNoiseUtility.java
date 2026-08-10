package application.bootstrap.worldpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.SeamlessAxisNoiseUtility;

public final class TerrainWrapNoiseUtility extends EngineUtility {

    /*
     * Fractal (multi-octave) noise sampling for terrain, wrapped seamlessly
     * across the world the same way WeatherNoiseUtility wraps weather: world
     * X rides a circle embedded in 3D noise space so a full lap always lands
     * back on the same value, and world Z wraps through the shared
     * SeamlessAxisNoiseUtility, which blends only within a thin margin of
     * the true seam rather than across the entire world, so every octave
     * away from that seam costs exactly one noise sample. The circle position
     * for world X depends only on worldX and worldWidthBlocks — never on
     * wavelength — so the caller computes cos/sin exactly once per column and
     * passes it in here, instead of every octave of every fractal layer
     * repeating the same trig. Terrain never drifts or rotates over time —
     * every input here is a fixed world position, so the result is fully
     * deterministic from seed and coordinate alone. sampleSingle() calls
     * SeamlessAxisNoiseUtility.sample3D() directly rather than through a
     * closure, avoiding a per-octave allocation on this hot path.
     */

    private TerrainWrapNoiseUtility() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    static float sampleFractal(
            long seed,
            double cosAngle, double sinAngle, double worldZ,
            double worldWidthBlocks, double worldHeightBlocks,
            double baseWavelengthBlocks,
            int octaves, float persistence, float lacunarity) {

        float amplitude = 1f;
        float amplitudeSum = 0f;
        float sum = 0f;
        double wavelength = baseWavelengthBlocks;

        for (int octave = 0; octave < octaves; octave++) {

            long octaveSeed = seed ^ (EngineSetting.TERRAIN_OCTAVE_HASH_SALT * (octave * 2L + 1L));

            sum += sampleSingle(octaveSeed, cosAngle, sinAngle, worldZ, worldWidthBlocks, worldHeightBlocks, wavelength)
                    * amplitude;
            amplitudeSum += amplitude;

            amplitude *= persistence;
            wavelength /= lacunarity;
        }

        return amplitudeSum > 0f ? sum / amplitudeSum : 0f;
    }

    private static float sampleSingle(
            long seed, double cosAngle, double sinAngle, double worldZ,
            double worldWidthBlocks, double worldHeightBlocks, double wavelengthBlocks) {

        double effectiveWavelength = Math.max(wavelengthBlocks, 0.001);
        double embeddingRadius = worldWidthBlocks / (Math.PI * 2.0 * effectiveWavelength);

        double ex = cosAngle * embeddingRadius;
        double ey = sinAngle * embeddingRadius;

        return SeamlessAxisNoiseUtility.sample3D(
                worldZ, effectiveWavelength, worldHeightBlocks, EngineSetting.NOISE_SEAM_BLEND_WAVELENGTHS,
                seed, ex, ey);
    }
}