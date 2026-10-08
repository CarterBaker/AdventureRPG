package application.bootstrap.worldpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.NoiseUtility;
import engine.util.mathematics.extras.SeamlessAxisNoiseUtility;

public final class TerrainNoiseUtility extends EngineUtility {

    /*
     * Fractal noise for terrain, wrapped seamlessly around the world: X rides a
     * circle in 3D noise space and Z wraps through SeamlessAxisNoiseUtility.
     * The caller precomputes the circle position once per column, and results
     * depend only on seed and position. Ridged fractals fold each octave into
     * sharp crests weighted by the one before, and volume noise samples the
     * world in three dimensions, blending toward the one-period-back sample
     * only within a thin margin of each seam so it wraps on both axes.
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

    // In 0 to 1, each octave folded into crests and weighted by the crest above it, so ridges branch like veins
    static float sampleRidged(
            long seed,
            double cosAngle, double sinAngle, double worldZ,
            double worldWidthBlocks, double worldHeightBlocks,
            double baseWavelengthBlocks,
            int octaves, float gain, float lacunarity) {

        float amplitude = 1f;
        float amplitudeSum = 0f;
        float weight = 1f;
        float sum = 0f;
        double wavelength = baseWavelengthBlocks;

        for (int octave = 0; octave < octaves; octave++) {

            long octaveSeed = seed ^ (EngineSetting.TERRAIN_OCTAVE_HASH_SALT * (octave * 2L + 1L));
            float crest = 1f - Math.abs(sampleSingle(
                    octaveSeed, cosAngle, sinAngle, worldZ, worldWidthBlocks, worldHeightBlocks, wavelength));

            crest *= crest * weight;
            weight = crest;

            sum += crest * amplitude;
            amplitudeSum += amplitude;

            amplitude *= gain;
            wavelength /= lacunarity;
        }

        return amplitudeSum > 0f ? sum / amplitudeSum : 0f;
    }

    public static float sampleVolume(
            long seed,
            double worldX, double worldY, double worldZ,
            double worldWidthBlocks, double worldHeightBlocks,
            double wavelengthBlocks, float verticalScale) {

        double wrappedX = wrapIntoRange(worldX, worldWidthBlocks);
        double wrappedZ = wrapIntoRange(worldZ, worldHeightBlocks);
        double scaledY = worldY * verticalScale / wavelengthBlocks;
        double margin = wavelengthBlocks * EngineSetting.NOISE_SEAM_BLEND_WAVELENGTHS;

        float seamX = computeSeamWeight(wrappedX, worldWidthBlocks, margin);
        float seamZ = computeSeamWeight(wrappedZ, worldHeightBlocks, margin);

        float direct = sampleVolumePoint(seed, wrappedX, scaledY, wrappedZ, wavelengthBlocks);

        if (seamX == 0f && seamZ == 0f)
            return direct;

        float acrossX = seamX > 0f
                ? sampleVolumePoint(seed, wrappedX - worldWidthBlocks, scaledY, wrappedZ, wavelengthBlocks)
                : direct;
        float acrossZ = seamZ > 0f
                ? sampleVolumePoint(seed, wrappedX, scaledY, wrappedZ - worldHeightBlocks, wavelengthBlocks)
                : direct;
        float acrossBoth = seamX > 0f && seamZ > 0f
                ? sampleVolumePoint(
                        seed, wrappedX - worldWidthBlocks, scaledY, wrappedZ - worldHeightBlocks, wavelengthBlocks)
                : (seamX > 0f ? acrossX : acrossZ);

        float near = direct + (acrossX - direct) * seamX;
        float far = acrossZ + (acrossBoth - acrossZ) * seamX;

        return near + (far - near) * seamZ;
    }

    private static float sampleVolumePoint(
            long seed, double worldX, double scaledY, double worldZ, double wavelengthBlocks) {
        return NoiseUtility.noise3_ImproveXZ(seed, worldX / wavelengthBlocks, scaledY, worldZ / wavelengthBlocks);
    }

    // Eases from 0 a margin before the seam to 1 at it
    private static float computeSeamWeight(double axis, double period, double margin) {

        double clampedMargin = Math.min(margin, period * 0.5);
        double distanceFromSeam = period - axis;

        if (period <= 0.0 || clampedMargin <= 0.0 || distanceFromSeam >= clampedMargin)
            return 0f;

        float t = (float) (1.0 - distanceFromSeam / clampedMargin);

        return t * t * (3f - 2f * t);
    }

    private static double wrapIntoRange(double value, double range) {

        if (range <= 0.0)
            return value;

        double wrapped = value % range;

        return wrapped < 0.0 ? wrapped + range : wrapped;
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