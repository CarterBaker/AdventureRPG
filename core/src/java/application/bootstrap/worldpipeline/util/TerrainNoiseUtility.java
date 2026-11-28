package application.bootstrap.worldpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.NoiseUtility;
import engine.util.mathematics.extras.SeamlessAxisNoiseUtility;

public final class TerrainNoiseUtility extends EngineUtility {

    /*
     * Fractal noise for terrain, read at a surface point so it has no seam on
     * any world. On a torus X rides a circle in 3D noise space and Z wraps
     * through SeamlessAxisNoiseUtility; on a globe the point's place on the
     * ball is itself the 3D noise position, which no edge interrupts. Results
     * depend only on seed and place. Ridged fractals fold each octave into
     * sharp crests weighted by the one before, and volume noise samples the
     * ground in three dimensions, the height running along the place's up: a
     * torus blends toward the one-period-back sample only within a thin margin
     * of each seam so it wraps on both axes.
     */

    static float sampleFractal(
            long seed,
            SurfacePointStruct point,
            double baseWavelengthBlocks,
            int octaves, float persistence, float lacunarity) {

        float amplitude = 1f;
        float amplitudeSum = 0f;
        float sum = 0f;
        double wavelength = baseWavelengthBlocks;

        for (int octave = 0; octave < octaves; octave++) {

            long octaveSeed = seed ^ (EngineSetting.TERRAIN_OCTAVE_HASH_SALT * (octave * 2L + 1L));

            sum += sampleSingle(octaveSeed, point, wavelength) * amplitude;
            amplitudeSum += amplitude;

            amplitude *= persistence;
            wavelength /= lacunarity;
        }

        return amplitudeSum > 0f ? sum / amplitudeSum : 0f;
    }

    // In 0 to 1, each octave folded into crests and weighted by the crest above it, so ridges branch like veins
    static float sampleRidged(
            long seed,
            SurfacePointStruct point,
            double baseWavelengthBlocks,
            int octaves, float gain, float lacunarity) {

        float amplitude = 1f;
        float amplitudeSum = 0f;
        float weight = 1f;
        float sum = 0f;
        double wavelength = baseWavelengthBlocks;

        for (int octave = 0; octave < octaves; octave++) {

            long octaveSeed = seed ^ (EngineSetting.TERRAIN_OCTAVE_HASH_SALT * (octave * 2L + 1L));
            float crest = 1f - Math.abs(sampleSingle(octaveSeed, point, wavelength));

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
            SurfacePointStruct point,
            double worldY,
            double wavelengthBlocks,
            float verticalScale) {

        if (point.isGlobe()) {

            double lift = worldY * verticalScale;

            return NoiseUtility.noise3_Fallback(
                    seed,
                    (point.getNoiseX() + point.getDirection().x * lift) / wavelengthBlocks,
                    (point.getNoiseY() + point.getDirection().y * lift) / wavelengthBlocks,
                    (point.getNoiseZ() + point.getDirection().z * lift) / wavelengthBlocks);
        }

        double worldWidthBlocks = point.getWrapWidth();
        double worldHeightBlocks = point.getWrapHeight();
        double wrappedX = wrapIntoRange(point.getX(), worldWidthBlocks);
        double wrappedZ = wrapIntoRange(point.getZ(), worldHeightBlocks);
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

    private static float sampleSingle(long seed, SurfacePointStruct point, double wavelengthBlocks) {

        double effectiveWavelength = Math.max(wavelengthBlocks, EngineSetting.DIVISION_EPSILON);

        if (point.isGlobe())
            return NoiseUtility.noise3_Fallback(
                    seed,
                    point.getNoiseX() / effectiveWavelength,
                    point.getNoiseY() / effectiveWavelength,
                    point.getNoiseZ() / effectiveWavelength);

        double embeddingRadius = point.getWrapWidth() / (Math.PI * 2.0 * effectiveWavelength);

        double ex = point.getCosAngle() * embeddingRadius;
        double ey = point.getSinAngle() * embeddingRadius;

        return SeamlessAxisNoiseUtility.sample3D(
                point.getZ(), effectiveWavelength, point.getWrapHeight(), EngineSetting.NOISE_SEAM_BLEND_WAVELENGTHS,
                seed, ex, ey);
    }
}
