package application.bootstrap.weatherpipeline.weathermanager;

import engine.root.EngineUtility;
import engine.util.mathematics.extras.NoiseUtility;

/*
 * Seamless torus-wrapped noise shared by every weather noise sampler. Bends
 * each wrapped axis onto its own circle in noise-space and reads one
 * continuous 4D OpenSimplex field across both circles at once, so the
 * result tiles exactly at the world seam no matter how large or small the
 * world itself is. wavelengthChunks is clamped so at least a handful of
 * full cycles always fit around the world, which is what keeps a small
 * biome-map-driven world from ever degenerating into a flat gradient.
 */
final class ToroidalNoiseUtility extends EngineUtility {

    private static final double MIN_CYCLES_AROUND_WORLD = 4.0;

    private ToroidalNoiseUtility() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    static float sample(
            long seed,
            double chunkX, double chunkZ,
            double worldWidthChunks, double worldHeightChunks,
            double wavelengthChunks,
            double phaseX, double phaseZ) {

        double minDimension = Math.min(worldWidthChunks, worldHeightChunks);
        double effectiveWavelength = Math.min(
                wavelengthChunks,
                Math.max(minDimension / MIN_CYCLES_AROUND_WORLD, 0.001));

        double angleX = (chunkX / worldWidthChunks) * (Math.PI * 2.0) + phaseX;
        double angleZ = (chunkZ / worldHeightChunks) * (Math.PI * 2.0) + phaseZ;

        double radiusX = worldWidthChunks / (Math.PI * 2.0 * effectiveWavelength);
        double radiusZ = worldHeightChunks / (Math.PI * 2.0 * effectiveWavelength);

        double x = Math.cos(angleX) * radiusX;
        double y = Math.sin(angleX) * radiusX;
        double z = Math.cos(angleZ) * radiusZ;
        double w = Math.sin(angleZ) * radiusZ;

        float raw = NoiseUtility.noise4_ImproveXY_ImproveZW(seed, x, y, z, w);

        return clamp01(raw * 0.5f + 0.5f);
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}