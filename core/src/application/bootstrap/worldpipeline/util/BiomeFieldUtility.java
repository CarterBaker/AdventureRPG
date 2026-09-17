package application.bootstrap.worldpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.NoiseUtility;

public final class BiomeFieldUtility extends EngineUtility {

    /*
     * Pure geometry behind the biome field: the domain warp that breaks the
     * world PNG's straight pixel edges, the smooth four-tap reconstruction
     * kernel that turns a continuous pixel coordinate into weighted map
     * samples, and the jittered-cell kernel that scatters a biome's probable
     * variants into soft-edged patches. Every function here is a pure
     * function of (seed, position) with no chunk or lattice alignment
     * anywhere in it, which is what lets two adjacent chunks evaluate the
     * same world position and get bit-identical weights — the property the
     * old per-chunk 3x3 kernel did not have, and the reason biome borders
     * used to land as a cliff on the chunk grid.
     */

    public static final int MAP_SAMPLE_COUNT = 4;
    public static final int PATCH_SAMPLE_COUNT = 9;

    private BiomeFieldUtility() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    // Border Warp \\

    /*
     * Offset in map pixels applied to a pixel coordinate before it is
     * sampled. Two octaves: a broad one that bends a biome border into
     * peninsulas and bays, and a fine one that roughens the edge so the
     * border never reads as a drawn line.
     */
    public static double computeBorderWarpOffset(long seed, double pixelX, double pixelZ) {

        float broad = NoiseUtility.noise2(
                seed,
                pixelX * EngineSetting.BIOME_BORDER_WARP_FREQUENCY,
                pixelZ * EngineSetting.BIOME_BORDER_WARP_FREQUENCY);

        float fine = NoiseUtility.noise2(
                seed ^ EngineSetting.HASH_FINALIZER_MULTIPLIER_2,
                pixelX * EngineSetting.BIOME_BORDER_WARP_DETAIL_FREQUENCY,
                pixelZ * EngineSetting.BIOME_BORDER_WARP_DETAIL_FREQUENCY);

        return broad * EngineSetting.BIOME_BORDER_WARP_STRENGTH_PIXELS
                + fine * EngineSetting.BIOME_BORDER_WARP_DETAIL_STRENGTH_PIXELS;
    }

    // Map Reconstruction \\

    /*
     * Resolves a continuous pixel coordinate into the four map pixels
     * surrounding it and the weight each one holds there. Weights come from
     * a contrast-compressed smoothstep rather than raw bilinear: the middle
     * BIOME_BLEND_BAND_PIXELS of the span between two pixel centers carries
     * the entire transition and everything outside it reads as one pure
     * biome, so a hand-painted region stays exactly the biome it was painted
     * while its border dissolves over a band the author controls in pixels.
     * The curve has zero derivative at both ends of that band, so height
     * built on these weights has no crease where the blend begins or ends.
     */
    public static void computeMapSamples(
            double pixelX, double pixelZ,
            int mapWidth, int mapHeight,
            int[] outPixelX, int[] outPixelZ, float[] outWeights) {

        double latticeX = pixelX - 0.5;
        double latticeZ = pixelZ - 0.5;

        int baseX = floorToInt(latticeX);
        int baseZ = floorToInt(latticeZ);

        float weightX = contrast((float) (latticeX - baseX));
        float weightZ = contrast((float) (latticeZ - baseZ));

        int lowX = wrapIndex(baseX, mapWidth);
        int highX = wrapIndex(baseX + 1, mapWidth);
        int lowZ = wrapIndex(baseZ, mapHeight);
        int highZ = wrapIndex(baseZ + 1, mapHeight);

        outPixelX[0] = lowX;
        outPixelZ[0] = lowZ;
        outWeights[0] = (1f - weightX) * (1f - weightZ);

        outPixelX[1] = highX;
        outPixelZ[1] = lowZ;
        outWeights[1] = weightX * (1f - weightZ);

        outPixelX[2] = lowX;
        outPixelZ[2] = highZ;
        outWeights[2] = (1f - weightX) * weightZ;

        outPixelX[3] = highX;
        outPixelZ[3] = highZ;
        outWeights[3] = weightX * weightZ;
    }

    // Probable Variant Patches \\

    /*
     * Scatters a biome's probable variants into coherent patches instead of
     * rolling them per chunk. The patch lattice is an integer subdivision of
     * the map pixel grid, so it wraps exactly where the world wraps, and each
     * cell's center is jittered off-grid so patches never read as squares.
     * Returned weights are normalized and every cell whose kernel reaches
     * this position contributes, so a patch boundary is a gradient between
     * two rolled variants rather than an edge — the same reason the map
     * samples above are blended rather than selected.
     */
    public static int computePatchSamples(
            long seed,
            double pixelX, double pixelZ,
            int mapWidth, int mapHeight,
            long[] outCellHash, float[] outWeights) {

        int cellsPerPixel = EngineSetting.BIOME_PATCH_CELLS_PER_PIXEL;

        double cellSpaceX = pixelX * cellsPerPixel;
        double cellSpaceZ = pixelZ * cellsPerPixel;

        int cellCountX = mapWidth * cellsPerPixel;
        int cellCountZ = mapHeight * cellsPerPixel;

        int baseX = floorToInt(cellSpaceX);
        int baseZ = floorToInt(cellSpaceZ);

        float radius = EngineSetting.BIOME_PATCH_KERNEL_RADIUS_CELLS;
        float jitter = EngineSetting.BIOME_PATCH_CELL_JITTER;

        int count = 0;
        float weightSum = 0f;

        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {

                int cellX = baseX + dx;
                int cellZ = baseZ + dz;

                long cellHash = hashCell(seed, wrapIndex(cellX, cellCountX), wrapIndex(cellZ, cellCountZ));

                double centerX = cellX + 0.5 + (hash01(cellHash) - 0.5f) * jitter;
                double centerZ = cellZ + 0.5
                        + (hash01(cellHash ^ EngineSetting.HASH_FINALIZER_MULTIPLIER_1) - 0.5f) * jitter;

                double offsetX = cellSpaceX - centerX;
                double offsetZ = cellSpaceZ - centerZ;
                double distance = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);

                if (distance >= radius)
                    continue;

                float falloff = (float) Math.pow(1.0 - distance / radius, EngineSetting.BIOME_PATCH_FALLOFF_POWER);

                if (falloff <= 0f)
                    continue;

                outCellHash[count] = cellHash;
                outWeights[count] = falloff;
                weightSum += falloff;
                count++;
            }
        }

        if (count == 0 || weightSum <= 0f)
            return 0;

        float inverse = 1f / weightSum;

        for (int i = 0; i < count; i++)
            outWeights[i] *= inverse;

        return count;
    }

    // Hashing \\

    public static long hashCell(long seed, int cellX, int cellZ) {
        return seed
                ^ (cellX * EngineSetting.HASH_FINALIZER_MULTIPLIER_1)
                ^ (cellZ * EngineSetting.HASH_FINALIZER_MULTIPLIER_2);
    }

    public static float hash01(long value) {

        long hash = value;

        hash ^= (hash >>> 33);
        hash *= EngineSetting.HASH_FINALIZER_MULTIPLIER_1;
        hash ^= (hash >>> 33);
        hash *= EngineSetting.HASH_FINALIZER_MULTIPLIER_2;
        hash ^= (hash >>> 33);

        return (float) ((hash >>> 11) / (double) (1L << 53));
    }

    // Math \\

    private static float contrast(float t) {

        float compressed = (t - 0.5f) / EngineSetting.BIOME_BLEND_BAND_PIXELS + 0.5f;

        if (compressed <= 0f)
            return 0f;

        if (compressed >= 1f)
            return 1f;

        return compressed * compressed * (3f - 2f * compressed);
    }

    private static int floorToInt(double value) {

        int truncated = (int) value;

        return value < truncated ? truncated - 1 : truncated;
    }

    private static int wrapIndex(int value, int range) {

        int wrapped = value % range;

        return wrapped < 0 ? wrapped + range : wrapped;
    }
}