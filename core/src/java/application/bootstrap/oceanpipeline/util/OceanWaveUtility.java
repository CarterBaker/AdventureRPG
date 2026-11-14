package application.bootstrap.oceanpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public final class OceanWaveUtility extends EngineUtility {

    /*
     * CPU mirror of liquid/includes/OceanSurface.glsl, kept formula for
     * formula so a gameplay query and a tessellated vertex at the same position
     * agree on the sea. Sea state is weather turbulence, scaled by a drifting
     * periodic noise and by how exposed the water is; it splits into short chop
     * and long swell amplitudes. Every wave is a crest-sharpened sine with a
     * zero mean, so the tide stays the mean surface. The noise is the shader's
     * integer PCG gradient noise, reproduced with the same 32-bit arithmetic.
     */

    // Sea State \\

    public static float resolveSeaState(float turbulence, float noiseMultiplier, float exposure) {
        return (EngineSetting.OCEAN_SEA_STATE_CALM + turbulence) * noiseMultiplier * exposure;
    }

    public static float resolveChopAmplitudeBlocks(float seaState) {
        return Math.min(
                seaState * EngineSetting.OCEAN_WAVE_CHOP_AMPLITUDE_PER_SEA_STATE,
                EngineSetting.OCEAN_WAVE_CHOP_MAX_AMPLITUDE_BLOCKS);
    }

    public static float resolveSwellAmplitudeBlocks(float seaState) {
        return clamp(
                (seaState - EngineSetting.OCEAN_WAVE_SWELL_SEA_STATE_START)
                        * EngineSetting.OCEAN_WAVE_SWELL_AMPLITUDE_PER_SEA_STATE,
                0f,
                EngineSetting.OCEAN_WAVE_SWELL_MAX_AMPLITUDE_BLOCKS);
    }

    public static float resolveExposure(float openWaterFraction) {
        return smoothstep(
                EngineSetting.OCEAN_EXPOSURE_START_FRACTION,
                EngineSetting.OCEAN_EXPOSURE_FULL_FRACTION,
                openWaterFraction);
    }

    public static float resolveWhitecap(float seaState, float crest) {
        return smoothstep(
                EngineSetting.OCEAN_WHITECAP_SEA_STATE_START,
                EngineSetting.OCEAN_WHITECAP_SEA_STATE_FULL,
                seaState)
                * smoothstep(EngineSetting.OCEAN_WHITECAP_CREST_START, 1f, crest);
    }

    // Wave Shape \\

    public static float computeShapeMean(float sharpness) {

        double halfSharpness = sharpness * 0.5;
        double term = 1.0;
        double besselSum = 1.0;

        for (int m = 1; m < EngineSetting.OCEAN_WAVE_SHAPE_MEAN_TERMS; m++) {
            term *= (halfSharpness * halfSharpness) / ((double) m * m);
            besselSum += term;
        }

        return (float) (Math.exp(-sharpness) * besselSum);
    }

    public static float shape(float theta, float sharpness, float shapeMean) {
        return ((float) Math.exp(sharpness * ((float) Math.sin(theta) - 1f)) - shapeMean) / (1f - shapeMean);
    }

    public static float shapeSlope(float theta, float sharpness, float shapeMean) {
        return sharpness * (float) Math.cos(theta)
                * (float) Math.exp(sharpness * ((float) Math.sin(theta) - 1f)) / (1f - shapeMean);
    }

    // Sea Noise \\

    public static float sampleSeaNoiseMultiplier(float latticeX, float latticeZ, int periodX, int periodZ) {

        float coarse = periodicGradientNoise2D(latticeX, latticeZ, periodX, periodZ,
                EngineSetting.OCEAN_SEA_NOISE_SEED);
        float detail = periodicGradientNoise2D(latticeX * 2f, latticeZ * 2f, periodX * 2, periodZ * 2,
                EngineSetting.OCEAN_SEA_NOISE_SEED + EngineSetting.OCEAN_NOISE_OCTAVE_SEED_STEP);

        float noise = (coarse + detail * EngineSetting.OCEAN_SEA_NOISE_DETAIL_WEIGHT)
                / (1f + EngineSetting.OCEAN_SEA_NOISE_DETAIL_WEIGHT);
        float noise01 = clamp(noise * EngineSetting.OCEAN_SEA_NOISE_CONTRAST + 0.5f, 0f, 1f);

        return EngineSetting.OCEAN_SEA_NOISE_MIN
                + (EngineSetting.OCEAN_SEA_NOISE_MAX - EngineSetting.OCEAN_SEA_NOISE_MIN) * noise01;
    }

    public static float periodicGradientNoise2D(float x, float z, int periodX, int periodZ, int seed) {

        float cellX = (float) Math.floor(x);
        float cellZ = (float) Math.floor(z);
        float fx = x - cellX;
        float fz = z - cellZ;
        float ux = fade(fx);
        float uz = fade(fz);

        int x0 = Math.floorMod((int) cellX, periodX);
        int x1 = Math.floorMod((int) cellX + 1, periodX);
        int z0 = Math.floorMod((int) cellZ, periodZ);
        int z1 = Math.floorMod((int) cellZ + 1, periodZ);

        float n00 = gradientDot(x0, z0, seed, fx, fz);
        float n10 = gradientDot(x1, z0, seed, fx - 1f, fz);
        float n01 = gradientDot(x0, z1, seed, fx, fz - 1f);
        float n11 = gradientDot(x1, z1, seed, fx - 1f, fz - 1f);

        float nx0 = n00 + (n10 - n00) * ux;
        float nx1 = n01 + (n11 - n01) * ux;

        return nx0 + (nx1 - nx0) * uz;
    }

    private static float gradientDot(int latticeX, int latticeZ, int seed, float dx, float dz) {

        int multiplier = EngineSetting.OCEAN_NOISE_PCG_MULTIPLIER;
        int shift = EngineSetting.OCEAN_NOISE_PCG_SHIFT;

        int hx = latticeX + seed;
        int hz = latticeZ + seed * EngineSetting.OCEAN_NOISE_PCG_SEED_MULTIPLIER;

        hx = hx * multiplier + EngineSetting.OCEAN_NOISE_PCG_INCREMENT;
        hz = hz * multiplier + EngineSetting.OCEAN_NOISE_PCG_INCREMENT;
        hx += hz * multiplier;
        hz += hx * multiplier;
        hx ^= hx >>> shift;
        hz ^= hz >>> shift;
        hx += hz * multiplier;
        hz += hx * multiplier;
        hx ^= hx >>> shift;
        hz ^= hz >>> shift;

        float scale = (float) EngineSetting.OCEAN_NOISE_GRADIENT_SCALE;
        float gradientX = (float) (hx & EngineSetting.OCEAN_NOISE_UINT_MASK) * scale - 1f;
        float gradientZ = (float) (hz & EngineSetting.OCEAN_NOISE_UINT_MASK) * scale - 1f;

        return gradientX * dx + gradientZ * dz;
    }

    // Utility \\

    private static float fade(float t) {
        return t * t * t * (t * (t * 6f - 15f) + 10f);
    }

    public static float smoothstep(float edge0, float edge1, float x) {

        float t = clamp((x - edge0) / (edge1 - edge0), 0f, 1f);

        return t * t * (3f - 2f * t);
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
