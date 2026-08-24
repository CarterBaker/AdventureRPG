package engine.util.mathematics.extras;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.vectors.Vector3;

public final class NaturalNoiseUtility extends EngineUtility {

    /*
     * CPU mirror of the near-terrain surface jitter StandardSurfaceShader.tes
     * bevels natural-looking terrain with, reproducing the exact hash formula
     * the shader evaluates so the GPU (via NaturalNoiseData) and every method
     * below always read the same values. sampleAxisJitter() and
     * sampleAxisJitterGradient() are the primitives BlockCollisionBranch
     * builds a natural block's collision wobble and wall-hug deflection from,
     * so a wall's visual bulge and its collision boundary can never drift
     * apart. getTier0MaxSqDistChunks()/getTier1MaxSqDistChunks() mirror
     * surface/includes/SurfaceTessellationTier.glsl so a caller can tell
     * whether a chunk offset falls inside the ring jitter is scoped to.
     */

    private NaturalNoiseUtility() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    // Lattice \\

    public static float hash(float x, float z) {
        float dot = x * EngineSetting.NATURAL_NOISE_HASH_DOT_X + z * EngineSetting.NATURAL_NOISE_HASH_DOT_Z;
        float sinValue = (float) Math.sin(dot) * EngineSetting.NATURAL_NOISE_HASH_SCALE;
        return sinValue - (float) Math.floor(sinValue);
    }

    public static float[] bakeLattice() {

        int period = EngineSetting.NATURAL_NOISE_LATTICE_PERIOD;
        float[] lattice = new float[period * period];

        for (int x = 0; x < period; x++)
            for (int z = 0; z < period; z++)
                lattice[x * period + z] = hash((float) x, (float) z);

        return lattice;
    }

    private static int wrapLatticeIndex(int value, int period) {
        int wrapped = value % period;
        return wrapped < 0 ? wrapped + period : wrapped;
    }

    public static float sampleSmooth(float px, float pz, float[] lattice) {

        int period = EngineSetting.NATURAL_NOISE_LATTICE_PERIOD;

        float ix = (float) Math.floor(px);
        float iz = (float) Math.floor(pz);
        float fx = px - ix;
        float fz = pz - iz;

        fx = fx * fx * (3f - 2f * fx);
        fz = fz * fz * (3f - 2f * fz);

        int x0 = wrapLatticeIndex((int) ix, period);
        int x1 = wrapLatticeIndex((int) ix + 1, period);
        int z0 = wrapLatticeIndex((int) iz, period);
        int z1 = wrapLatticeIndex((int) iz + 1, period);

        float h00 = lattice[x0 * period + z0];
        float h10 = lattice[x1 * period + z0];
        float h01 = lattice[x0 * period + z1];
        float h11 = lattice[x1 * period + z1];

        float top = h00 + (h10 - h00) * fx;
        float bottom = h01 + (h11 - h01) * fx;

        return top + (bottom - top) * fz;
    }

    // Jitter \\

    public static float sampleAxisJitter(double worldX, double worldZ, float[] lattice, int axis) {

        float seedX = (float) worldX * EngineSetting.NATURAL_NOISE_SEED_SCALE;
        float seedZ = (float) worldZ * EngineSetting.NATURAL_NOISE_SEED_SCALE;

        if (axis == EngineSetting.AXIS_X)
            return (sampleSmooth(
                    seedX + EngineSetting.NATURAL_NOISE_OFFSET_X_X,
                    seedZ + EngineSetting.NATURAL_NOISE_OFFSET_X_Z, lattice) - 0.5f)
                    * EngineSetting.NATURAL_NOISE_JITTER_HORIZONTAL_BLOCKS;

        if (axis == EngineSetting.AXIS_Y)
            return (sampleSmooth(
                    seedX + EngineSetting.NATURAL_NOISE_OFFSET_Y_X,
                    seedZ + EngineSetting.NATURAL_NOISE_OFFSET_Y_Z, lattice) - 0.5f)
                    * EngineSetting.NATURAL_NOISE_JITTER_VERTICAL_BLOCKS;

        if (axis == EngineSetting.AXIS_Z)
            return (sampleSmooth(
                    seedX + EngineSetting.NATURAL_NOISE_OFFSET_Z_X,
                    seedZ + EngineSetting.NATURAL_NOISE_OFFSET_Z_Z, lattice) - 0.5f)
                    * EngineSetting.NATURAL_NOISE_JITTER_HORIZONTAL_BLOCKS;

        throw new IllegalArgumentException("axis must be AXIS_X, AXIS_Y, or AXIS_Z: " + axis);
    }

    public static void sampleJitter(double worldX, double worldZ, float[] lattice, Vector3 out) {
        out.set(
                sampleAxisJitter(worldX, worldZ, lattice, EngineSetting.AXIS_X),
                sampleAxisJitter(worldX, worldZ, lattice, EngineSetting.AXIS_Y),
                sampleAxisJitter(worldX, worldZ, lattice, EngineSetting.AXIS_Z));
    }

    public static float sampleAxisJitterGradient(
            double worldX, double worldZ, float[] lattice, int axis, int tangentAxis) {

        float probe = EngineSetting.NATURAL_NOISE_COLLISION_GRADIENT_PROBE_BLOCKS;

        double probeX = tangentAxis == EngineSetting.AXIS_X ? probe : 0.0;
        double probeZ = tangentAxis == EngineSetting.AXIS_Z ? probe : 0.0;

        if (probeX == 0.0 && probeZ == 0.0)
            throw new IllegalArgumentException("tangentAxis must be AXIS_X or AXIS_Z: " + tangentAxis);

        float back = sampleAxisJitter(worldX - probeX, worldZ - probeZ, lattice, axis);
        float forward = sampleAxisJitter(worldX + probeX, worldZ + probeZ, lattice, axis);

        return (forward - back) / (2f * probe);
    }

    // Tessellation Tier \\

    public static float getTier1MaxSqDistChunks(float renderDistance, float chunkSize) {
        float halfD = renderDistance * 0.5f - 0.5f;
        float marginChunks = EngineSetting.NATURAL_NOISE_DISTANT_RISE_MARGIN_BLOCKS
                / (chunkSize * (float) Math.sqrt(2.0));
        float farHalfD = Math.max(halfD - marginChunks, 1f);
        return farHalfD * farHalfD * 2f;
    }

    public static float getTier0MaxSqDistChunks(float nearTessellationRadius, float renderDistance, float chunkSize) {
        float tier1 = getTier1MaxSqDistChunks(renderDistance, chunkSize);
        float r = Math.max(nearTessellationRadius, 1f);
        return Math.min(2f * r * r + 0.5f, tier1);
    }
}