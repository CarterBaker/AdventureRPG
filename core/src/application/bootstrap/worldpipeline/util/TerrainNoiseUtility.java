package application.bootstrap.worldpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.SeamlessAxisNoiseUtility;

public final class TerrainNoiseUtility extends EngineUtility {

    /*
     * Fractal noise for terrain, wrapped seamlessly around the world: X rides a
     * circle in 3D noise space and Z wraps through SeamlessAxisNoiseUtility.
     * The caller precomputes the circle position once per column, and results
     * depend only on seed and position.
     */

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

        double effectiveWavelength = Math.max(wavelengthBlocks, EngineSetting.DIVISION_EPSILON);
        double embeddingRadius = worldWidthBlocks / (Math.PI * 2.0 * effectiveWavelength);

        double ex = cosAngle * embeddingRadius;
        double ey = sinAngle * embeddingRadius;

        return SeamlessAxisNoiseUtility.sample3D(
                worldZ, effectiveWavelength, worldHeightBlocks, EngineSetting.NOISE_SEAM_BLEND_WAVELENGTHS,
                seed, ex, ey);
    }
}