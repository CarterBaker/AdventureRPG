package engine.util.mathematics.extras;

import java.util.SplittableRandom;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.vectors.Vector3;

public final class NaturalNoiseUtility extends EngineUtility {

    /*
     * CPU copy of includes/NaturalNoiseData.glsl and the natural fields in
     * surface/includes/NearTerrainNoise.glsl, kept formula for formula so a
     * physics query and a tessellated vertex at the same position agree. The
     * lattice is baked once from a fixed seed, four independent channels per
     * cell, and read back as periodic quintic value noise. Its period is a
     * whole number of chunks, so it wraps with every world and never moves as
     * the player crosses a chunk. Also holds the near tessellation density
     * radius the shader scopes the detail ring to.
     */

    // Settings
    private static final int PERIOD = EngineSetting.NATURAL_NOISE_LATTICE_PERIOD;
    private static final int CHANNELS = EngineSetting.NATURAL_NOISE_CHANNELS;
    private static final int PLANE_XZ = 0;
    private static final int PLANE_ZY = CHANNELS;
    private static final int PLANE_XY = CHANNELS * 2;

    // Lattice \\

    public static float[] bakeLattice() {

        SplittableRandom random = new SplittableRandom(EngineSetting.NATURAL_NOISE_SEED);
        float[] lattice = new float[EngineSetting.NATURAL_NOISE_LATTICE_SIZE * CHANNELS];

        for (int i = 0; i < lattice.length; i++)
            lattice[i] = (float) random.nextDouble(-1.0, 1.0);

        return lattice;
    }

    // All four channels at once, periodic on both lattice axes, written into out from offset
    public static void sample(float[] lattice, double latticeA, double latticeB, float[] out, int offset) {

        double cellA = Math.floor(latticeA);
        double cellB = Math.floor(latticeB);
        float weightA = fade((float) (latticeA - cellA));
        float weightB = fade((float) (latticeB - cellB));

        int a0 = Math.floorMod((long) cellA, PERIOD);
        int a1 = Math.floorMod((long) cellA + 1, PERIOD);
        int b0 = Math.floorMod((long) cellB, PERIOD);
        int b1 = Math.floorMod((long) cellB + 1, PERIOD);

        int i00 = (a0 * PERIOD + b0) * CHANNELS;
        int i10 = (a1 * PERIOD + b0) * CHANNELS;
        int i01 = (a0 * PERIOD + b1) * CHANNELS;
        int i11 = (a1 * PERIOD + b1) * CHANNELS;

        for (int channel = 0; channel < CHANNELS; channel++) {

            float low = mix(lattice[i00 + channel], lattice[i10 + channel], weightA);
            float high = mix(lattice[i01 + channel], lattice[i11 + channel], weightA);

            out[offset + channel] = mix(low, high, weightB);
        }
    }

    // Lattice coordinate of a world block coordinate on a wrapping axis, folded into one period first so world
    // coordinates of any size keep full precision
    public static double toWrappedLattice(double worldBlocks) {

        double period = EngineSetting.NATURAL_NOISE_PERIOD_BLOCKS;
        double wrapped = worldBlocks - Math.floor(worldBlocks / period) * period;

        return wrapped / EngineSetting.NATURAL_NOISE_CELL_BLOCKS;
    }

    public static double toLattice(double worldBlocks) {
        return worldBlocks / EngineSetting.NATURAL_NOISE_CELL_BLOCKS;
    }

    // Fields \\

    // The near-ring detail vector and the edge warp at a world position, both pure functions of it. The detail
    // is unit-ranged and split against a face normal by its consumer; the warp is in blocks, the full offset a
    // natural vertex at that position is drawn at. Scratch holds one set of channels per projection plane.
    public static void sampleFields(
            float[] lattice,
            double worldX, double worldY, double worldZ,
            float[] scratch,
            Vector3 outDetail,
            Vector3 outWarp) {

        double latticeX = toWrappedLattice(worldX);
        double latticeY = toLattice(worldY);
        double latticeZ = toWrappedLattice(worldZ);
        double planeZYA = latticeZ + EngineSetting.NATURAL_NOISE_PLANE_OFFSET_CELLS;
        double planeXYB = latticeY + EngineSetting.NATURAL_NOISE_PLANE_OFFSET_CELLS;

        samplePlanes(lattice, latticeX, latticeY, latticeZ, planeZYA, planeXYB, 1.0, scratch);

        outDetail.set(
                (scratch[PLANE_XZ] + scratch[PLANE_ZY]) * 0.5f,
                (scratch[PLANE_XZ + 1] + scratch[PLANE_XY]) * 0.5f,
                (scratch[PLANE_ZY + 1] + scratch[PLANE_XY + 1]) * 0.5f);

        float primaryX = (scratch[PLANE_XZ + 2] + scratch[PLANE_ZY + 2]) * 0.5f;
        float primaryY = (scratch[PLANE_XZ + 3] + scratch[PLANE_XY + 2]) * 0.5f;
        float primaryZ = (scratch[PLANE_ZY + 3] + scratch[PLANE_XY + 3]) * 0.5f;

        samplePlanes(lattice, latticeX, latticeY, latticeZ, planeZYA, planeXYB, 2.0, scratch);

        float fineX = (scratch[PLANE_XZ + 2] + scratch[PLANE_ZY + 2]) * 0.5f;
        float fineY = (scratch[PLANE_XZ + 3] + scratch[PLANE_XY + 2]) * 0.5f;
        float fineZ = (scratch[PLANE_ZY + 3] + scratch[PLANE_XY + 3]) * 0.5f;

        float share = EngineSetting.NATURAL_EDGE_WARP_DETAIL_SHARE;
        float normalize = 1f / (1f + share);

        outWarp.set(
                (primaryX + fineX * share) * normalize * EngineSetting.NATURAL_EDGE_WARP_HORIZONTAL_BLOCKS,
                (primaryY + fineY * share) * normalize * EngineSetting.NATURAL_EDGE_WARP_VERTICAL_BLOCKS,
                (primaryZ + fineZ * share) * normalize * EngineSetting.NATURAL_EDGE_WARP_HORIZONTAL_BLOCKS);
    }

    // The three projections of a position, at frequency times the lattice, into scratch
    private static void samplePlanes(
            float[] lattice,
            double latticeX, double latticeY, double latticeZ,
            double planeZYA, double planeXYB,
            double frequency,
            float[] scratch) {

        sample(lattice, latticeX * frequency, latticeZ * frequency, scratch, PLANE_XZ);
        sample(lattice, planeZYA * frequency, latticeY * frequency, scratch, PLANE_ZY);
        sample(lattice, latticeX * frequency, planeXYB * frequency, scratch, PLANE_XY);
    }

    // Utility \\

    private static float fade(float t) {
        return t * t * t * (t * (t * 6f - 15f) + 10f);
    }

    private static float mix(float a, float b, float t) {
        return a + (b - a) * t;
    }

    // Tessellation Tier \\

    // Squared distance in blocks within which a chunk's center takes the near tessellation density
    public static float getNearDensityMaxSqDistBlocks(float detailRadius) {
        float densityRadius = Math.max(detailRadius, 0f) + EngineSetting.NATURAL_NOISE_DETAIL_DENSITY_MARGIN_BLOCKS;
        return densityRadius * densityRadius;
    }
}
