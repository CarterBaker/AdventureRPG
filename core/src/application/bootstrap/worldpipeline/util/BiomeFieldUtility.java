package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.biome.ProbableBiomeStruct;
import application.bootstrap.worldpipeline.biome.ProbablePatchStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.NoiseUtility;

public final class BiomeFieldUtility extends EngineUtility {

    /*
     * Pure geometry behind the biome field: the domain warp that breaks map
     * pixel edges, the four-tap reconstruction kernel, and the scatter that
     * shapes probable biome patches. World-tiling cells each hold a random
     * number of patches: a body with arms, bent by a broad warp and broken by
     * two octaves of edge noise into natural, irregular outlines, merging
     * where they overlap. Everything depends only on seed and position, so
     * neighboring chunks agree exactly.
     */

    public static final int MAP_SAMPLE_COUNT = EngineSetting.BIOME_MAP_SAMPLE_COUNT;

    private static final double BLOCKS_PER_PIXEL = EngineSetting.CHUNKS_PER_PIXEL * (double) EngineSetting.CHUNK_SIZE;
    private static final float PATCH_REACH = 1f
            + EngineSetting.BIOME_PROBABLE_EDGE_BAND
            + EngineSetting.BIOME_PROBABLE_EDGE_NOISE_STRENGTH;
    private static final double SHAPE_BOUND_RADIUS = 0.5;
    private static final double SHAPE_EXTENT_RADIUS = SHAPE_BOUND_RADIUS
            + EngineSetting.BIOME_PROBABLE_SHAPE_WARP_STRENGTH;
    private static final double SHAPE_MAX_CAPSULE_RADIUS = Math.max(
            EngineSetting.BIOME_PROBABLE_SHAPE_BODY_MAX_RADIUS,
            EngineSetting.BIOME_PROBABLE_SHAPE_ARM_MAX_RADIUS * EngineSetting.BIOME_PROBABLE_SHAPE_ARM_MAX_TAPER);
    private static final double SHAPE_REACH_RADIUS = SHAPE_BOUND_RADIUS
            + SHAPE_MAX_CAPSULE_RADIUS * (PATCH_REACH - 1f)
            + EngineSetting.BIOME_PROBABLE_SHAPE_WARP_STRENGTH;
    private static final float[] SHAPE_AREA_BY_ARMS = measureShapeAreas();

    // Border Warp \\

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

    // Probable Biome Scatter \\

    // Mean patch area for diameters drawn log-uniformly between the two sizes and arm counts drawn evenly
    public static double computeMeanPatchAreaBlocks(
            float minSizeBlocks,
            float maxSizeBlocks,
            int minArms,
            int maxArms) {

        double minSq = (double) minSizeBlocks * minSizeBlocks;
        double maxSq = (double) maxSizeBlocks * maxSizeBlocks;

        double meanDiameterSq = maxSizeBlocks > minSizeBlocks
                ? (maxSq - minSq) / (2.0 * Math.log((double) maxSizeBlocks / minSizeBlocks))
                : minSq;

        double shapeArea = 0.0;

        for (int arms = minArms; arms <= maxArms; arms++)
            shapeArea += SHAPE_AREA_BY_ARMS[arms];

        return meanDiameterSq * shapeArea / (maxArms - minArms + 1);
    }

    // Patches per square block whose overlapping union covers the share
    public static double computePatchDensity(float share, double meanPatchAreaBlocks) {

        if (share <= 0f)
            return 0.0;

        return -Math.log(1.0 - share) / meanPatchAreaBlocks;
    }

    // At least a patch's reach, and wider for sparse patches so most positions only touch one cell
    public static double computeScatterCellSizeBlocks(double patchDensity, float maxSizeBlocks) {

        double reachBlocks = maxSizeBlocks * SHAPE_REACH_RADIUS;

        if (patchDensity <= 0.0)
            return reachBlocks;

        return Math.max(reachBlocks, Math.sqrt(EngineSetting.BIOME_PROBABLE_MAX_CELL_OCCUPANCY / patchDensity));
    }

    public static void computeScatterCoverage(
            long seed,
            ProbableBiomeStruct probableBiome,
            double pixelX, double pixelZ,
            int mapWidth, int mapHeight,
            ProbablePatchStruct outPatch) {

        outPatch.reset();

        if (probableBiome.getPatchDensity() <= 0.0) {
            outPatch.setCoverage(probableBiome.isInverted() ? 1f : 0f);
            return;
        }

        double worldWidthBlocks = mapWidth * BLOCKS_PER_PIXEL;
        double worldHeightBlocks = mapHeight * BLOCKS_PER_PIXEL;

        int cellCountX = computeScatterCellCount(worldWidthBlocks, probableBiome.getCellSizeBlocks());
        int cellCountZ = computeScatterCellCount(worldHeightBlocks, probableBiome.getCellSizeBlocks());

        double cellWidthBlocks = worldWidthBlocks / cellCountX;
        double cellHeightBlocks = worldHeightBlocks / cellCountZ;

        double meanPatchesPerCell = probableBiome.getPatchDensity() * cellWidthBlocks * cellHeightBlocks;
        double emptyCellChance = Math.exp(-meanPatchesPerCell);

        double blockX = pixelX * BLOCKS_PER_PIXEL;
        double blockZ = pixelZ * BLOCKS_PER_PIXEL;

        double maxReachBlocks = probableBiome.getMaxSizeBlocks() * SHAPE_REACH_RADIUS;

        int firstCellX = floorToInt((blockX - maxReachBlocks) / cellWidthBlocks);
        int lastCellX = floorToInt((blockX + maxReachBlocks) / cellWidthBlocks);
        int firstCellZ = floorToInt((blockZ - maxReachBlocks) / cellHeightBlocks);
        int lastCellZ = floorToInt((blockZ + maxReachBlocks) / cellHeightBlocks);

        long scatterSeed = seed ^ mix64(probableBiome.getBiomeID());

        float uncovered = 1f;
        float bestMembership = 0f;

        for (int cellZ = firstCellZ; cellZ <= lastCellZ; cellZ++) {
            for (int cellX = firstCellX; cellX <= lastCellX; cellX++) {

                long draw = mix64(hashCell(scatterSeed, wrapIndex(cellX, cellCountX), wrapIndex(cellZ, cellCountZ)));
                int patchCount = drawPatchCount(draw, meanPatchesPerCell, emptyCellChance);

                for (int patch = 0; patch < patchCount; patch++) {

                    long patchHash = mix64(draw + patch + 1);
                    long patchDraw = mix64(patchHash);
                    double offsetX = blockX - (cellX + (double) toUnit(patchDraw)) * cellWidthBlocks;
                    patchDraw = mix64(patchDraw);
                    double offsetZ = blockZ - (cellZ + (double) toUnit(patchDraw)) * cellHeightBlocks;

                    if (offsetX * offsetX + offsetZ * offsetZ >= maxReachBlocks * maxReachBlocks)
                        continue;

                    patchDraw = mix64(patchDraw);
                    double diameter = probableBiome.getMinSizeBlocks()
                            * Math.exp(probableBiome.getSizeLogRatio() * toUnit(patchDraw));

                    double shapeX = offsetX / diameter;
                    double shapeZ = offsetZ / diameter;

                    if (shapeX * shapeX + shapeZ * shapeZ >= SHAPE_REACH_RADIUS * SHAPE_REACH_RADIUS)
                        continue;

                    patchDraw = mix64(patchDraw);
                    int arms = drawArms(patchDraw, probableBiome.getMinArms(), probableBiome.getMaxArms());
                    long shapeHash = patchHash ^ EngineSetting.BIOME_PROBABLE_SHAPE_SEED;

                    float shape = computeShapeMetric(shapeHash, arms, shapeX, shapeZ);

                    if (shape >= PATCH_REACH)
                        continue;

                    float noise = computeEdgeNoise(shapeHash, shapeX, shapeZ);

                    float membership = computeOutlineMembership(shape, noise);

                    if (membership <= 0f)
                        continue;

                    uncovered *= 1f - membership;

                    if (membership > bestMembership) {
                        bestMembership = membership;
                        outPatch.set(0f, patchHash, shape, noise);
                    }
                }
            }
        }

        if (probableBiome.isInverted())
            outPatch.set(uncovered, 0L, ProbablePatchStruct.NO_PATCH_SHAPE, 0f);
        else
            outPatch.setCoverage(1f - uncovered);
    }

    // A core inside the host patch, its outline the host's shrunk by a drawn scale
    public static void computeCenterCoverage(
            ProbableBiomeStruct probableBiome,
            long hostHash,
            float hostShape,
            float hostNoise,
            ProbablePatchStruct outPatch) {

        outPatch.reset();

        if (hostShape == ProbablePatchStruct.NO_PATCH_SHAPE)
            return;

        long coreHash = mix64(hostHash ^ mix64(probableBiome.getBiomeID()) ^ EngineSetting.BIOME_PROBABLE_CORE_SEED);

        if (toUnit(coreHash) >= probableBiome.getChance())
            return;

        float scale = lerp(
                probableBiome.getMinCoreScale(), probableBiome.getMaxCoreScale(), toUnit(mix64(coreHash)));

        float coreShape = hostShape / scale;
        float membership = computeOutlineMembership(coreShape, hostNoise);

        if (membership > 0f)
            outPatch.set(membership, coreHash, coreShape, hostNoise);
    }

    private static int computeScatterCellCount(double worldExtentBlocks, double cellSizeBlocks) {
        return Math.max(1, (int) (worldExtentBlocks / cellSizeBlocks));
    }

    // Poisson-distributed, so patches cluster and thin out the way scattered features do
    private static int drawPatchCount(long draw, double meanPatchesPerCell, double emptyCellChance) {

        float roll = toUnit(draw);
        double probability = emptyCellChance;
        double cumulative = probability;
        int count = 0;

        while (roll > cumulative && count < EngineSetting.BIOME_PROBABLE_MAX_PATCHES_PER_CELL) {
            count++;
            probability *= meanPatchesPerCell / count;
            cumulative += probability;
        }

        return count;
    }

    private static int drawArms(long draw, int minArms, int maxArms) {

        int range = maxArms - minArms + 1;

        return minArms + Math.min(range - 1, (int) (toUnit(draw) * range));
    }

    private static float computeOutlineMembership(float shape, float noise) {

        float edge = shape - noise * EngineSetting.BIOME_PROBABLE_EDGE_NOISE_STRENGTH;
        float band = EngineSetting.BIOME_PROBABLE_EDGE_BAND;

        return 1f - smoothstep((edge - (1f - band)) / (2f * band));
    }

    // Patch Shape \\

    // Below 1 inside the outline of a patch one block across, its skeleton bent by a broad warp
    private static float computeShapeMetric(long shapeHash, int arms, double x, double z) {

        long warpSeed = shapeHash ^ EngineSetting.BIOME_PROBABLE_SHAPE_WARP_SEED;
        double warpX = x * EngineSetting.BIOME_PROBABLE_SHAPE_WARP_FREQUENCY;
        double warpZ = z * EngineSetting.BIOME_PROBABLE_SHAPE_WARP_FREQUENCY;

        double warpedX = x + NoiseUtility.noise2(warpSeed, warpX, warpZ)
                * EngineSetting.BIOME_PROBABLE_SHAPE_WARP_STRENGTH;
        double warpedZ = z + NoiseUtility.noise2(warpSeed ^ EngineSetting.HASH_FINALIZER_MULTIPLIER_1, warpX, warpZ)
                * EngineSetting.BIOME_PROBABLE_SHAPE_WARP_STRENGTH;

        return computeSkeletonMetric(shapeHash, arms, warpedX, warpedZ);
    }

    // Two octaves breaking the outline, broad lobes with finer ragged detail
    private static float computeEdgeNoise(long shapeHash, double x, double z) {

        long noiseSeed = shapeHash ^ EngineSetting.BIOME_PROBABLE_EDGE_NOISE_SEED;

        float broad = NoiseUtility.noise2(
                noiseSeed,
                x * EngineSetting.BIOME_PROBABLE_EDGE_NOISE_FREQUENCY,
                z * EngineSetting.BIOME_PROBABLE_EDGE_NOISE_FREQUENCY);

        float detail = NoiseUtility.noise2(
                noiseSeed ^ EngineSetting.HASH_FINALIZER_MULTIPLIER_2,
                x * EngineSetting.BIOME_PROBABLE_EDGE_DETAIL_FREQUENCY,
                z * EngineSetting.BIOME_PROBABLE_EDGE_DETAIL_FREQUENCY);

        return (broad + detail * EngineSetting.BIOME_PROBABLE_EDGE_DETAIL_WEIGHT)
                / (1f + EngineSetting.BIOME_PROBABLE_EDGE_DETAIL_WEIGHT);
    }

    // A body with arms that continue, bend, branch and cross, each arm a tapered capsule
    private static float computeSkeletonMetric(long shapeHash, int arms, double x, double z) {

        long draw = mix64(shapeHash);
        float bodyRadius = lerp(
                EngineSetting.BIOME_PROBABLE_SHAPE_BODY_MIN_RADIUS,
                EngineSetting.BIOME_PROBABLE_SHAPE_BODY_MAX_RADIUS,
                toUnit(draw));

        float metric = (float) (Math.sqrt(x * x + z * z) / bodyRadius);

        double armStartX = 0.0;
        double armStartZ = 0.0;
        double armDirectionX = 0.0;
        double armDirectionZ = 0.0;
        double armLength = 0.0;
        float armStartRadius = bodyRadius;
        float armEndRadius = bodyRadius;

        for (int arm = 0; arm < arms; arm++) {

            draw = mix64(draw);
            float mode = toUnit(draw);

            draw = mix64(draw);
            double randomX = toUnit(draw) * 2.0 - 1.0;
            draw = mix64(draw);
            double randomZ = toUnit(draw) * 2.0 - 1.0;
            draw = mix64(draw);
            float position = toUnit(draw);

            double startX;
            double startZ;
            double directionX;
            double directionZ;
            float attachRadius;

            if (arm == 0 || mode < EngineSetting.BIOME_PROBABLE_SHAPE_ARM_CENTER_CHANCE) {

                startX = 0.0;
                startZ = 0.0;
                directionX = randomX;
                directionZ = randomZ;
                attachRadius = bodyRadius;
            } else if (mode < EngineSetting.BIOME_PROBABLE_SHAPE_ARM_CENTER_CHANCE
                    + EngineSetting.BIOME_PROBABLE_SHAPE_ARM_CONTINUE_CHANCE) {

                double bend = randomX * EngineSetting.BIOME_PROBABLE_SHAPE_ARM_MAX_BEND;

                startX = armStartX + armDirectionX * armLength;
                startZ = armStartZ + armDirectionZ * armLength;
                directionX = armDirectionX - armDirectionZ * bend;
                directionZ = armDirectionZ + armDirectionX * bend;
                attachRadius = armEndRadius;
            } else if (mode < EngineSetting.BIOME_PROBABLE_SHAPE_ARM_CENTER_CHANCE
                    + EngineSetting.BIOME_PROBABLE_SHAPE_ARM_CONTINUE_CHANCE
                    + EngineSetting.BIOME_PROBABLE_SHAPE_ARM_BRANCH_CHANCE) {

                float along = lerp(EngineSetting.BIOME_PROBABLE_SHAPE_ARM_BRANCH_MIN_POSITION, 1f, position);
                double side = randomZ < 0.0 ? -1.0 : 1.0;

                startX = armStartX + armDirectionX * armLength * along;
                startZ = armStartZ + armDirectionZ * armLength * along;
                directionX = -armDirectionZ * side;
                directionZ = armDirectionX * side;
                attachRadius = lerp(armStartRadius, armEndRadius, along);
            } else {

                startX = armStartX;
                startZ = armStartZ;
                directionX = -armDirectionX;
                directionZ = -armDirectionZ;
                attachRadius = armStartRadius;
            }

            double directionLength = Math.sqrt(directionX * directionX + directionZ * directionZ);

            if (directionLength <= 0.0)
                continue;

            directionX /= directionLength;
            directionZ /= directionLength;

            draw = mix64(draw);
            float startRadius = Math.min(attachRadius, lerp(
                    EngineSetting.BIOME_PROBABLE_SHAPE_ARM_MIN_RADIUS,
                    EngineSetting.BIOME_PROBABLE_SHAPE_ARM_MAX_RADIUS,
                    toUnit(draw)));

            draw = mix64(draw);
            float endRadius = startRadius * lerp(
                    EngineSetting.BIOME_PROBABLE_SHAPE_ARM_MIN_TAPER,
                    EngineSetting.BIOME_PROBABLE_SHAPE_ARM_MAX_TAPER,
                    toUnit(draw));

            draw = mix64(draw);
            double length = fitArmLength(
                    startX, startZ, directionX, directionZ,
                    lerp(EngineSetting.BIOME_PROBABLE_SHAPE_ARM_MIN_LENGTH,
                            EngineSetting.BIOME_PROBABLE_SHAPE_ARM_MAX_LENGTH,
                            toUnit(draw)),
                    Math.max(startRadius, endRadius));

            metric = Math.min(metric, computeCapsuleMetric(
                    x, z, startX, startZ, directionX, directionZ, length, startRadius, endRadius));

            armStartX = startX;
            armStartZ = startZ;
            armDirectionX = directionX;
            armDirectionZ = directionZ;
            armLength = length;
            armStartRadius = startRadius;
            armEndRadius = endRadius;
        }

        return metric;
    }

    // Shortens an arm so its far end, widened by its radius, stays inside the patch's bounding circle
    private static double fitArmLength(
            double startX, double startZ,
            double directionX, double directionZ,
            double length,
            float radius) {

        double boundRadius = SHAPE_BOUND_RADIUS - radius;
        double along = startX * directionX + startZ * directionZ;
        double discriminant = along * along - (startX * startX + startZ * startZ - boundRadius * boundRadius);

        if (boundRadius <= 0.0 || discriminant < 0.0)
            return 0.0;

        return Math.max(0.0, Math.min(length, Math.sqrt(discriminant) - along));
    }

    private static float computeCapsuleMetric(
            double x, double z,
            double startX, double startZ,
            double directionX, double directionZ,
            double length,
            float startRadius,
            float endRadius) {

        double offsetX = x - startX;
        double offsetZ = z - startZ;
        double t = length > 0.0
                ? Math.max(0.0, Math.min(1.0, (offsetX * directionX + offsetZ * directionZ) / length))
                : 0.0;

        double nearestX = offsetX - directionX * length * t;
        double nearestZ = offsetZ - directionZ * length * t;

        return (float) (Math.sqrt(nearestX * nearestX + nearestZ * nearestZ)
                / lerp(startRadius, endRadius, (float) t));
    }

    // Mean area inside the outline of patches one block across, by arm count, measured once over sample shapes
    private static float[] measureShapeAreas() {

        int resolution = EngineSetting.BIOME_PROBABLE_SHAPE_AREA_RESOLUTION;
        int samples = EngineSetting.BIOME_PROBABLE_SHAPE_AREA_SAMPLES;
        double step = 2.0 * SHAPE_EXTENT_RADIUS / resolution;

        float[] areas = new float[EngineSetting.BIOME_PROBABLE_SHAPE_MAX_ARMS + 1];

        for (int arms = 0; arms < areas.length; arms++) {

            int inside = 0;

            for (int sample = 0; sample < samples; sample++) {

                long shapeHash = mix64(sample ^ EngineSetting.BIOME_PROBABLE_SHAPE_SEED);

                for (int row = 0; row < resolution; row++)
                    for (int column = 0; column < resolution; column++)
                        if (computeShapeMetric(
                                shapeHash, arms,
                                (column + 0.5) * step - SHAPE_EXTENT_RADIUS,
                                (row + 0.5) * step - SHAPE_EXTENT_RADIUS) < 1f)
                            inside++;
            }

            areas[arms] = (float) (inside * step * step / samples);
        }

        return areas;
    }

    // Shore Buffer \\

    public static float computeShoreBufferFraction(float oceanWeight) {

        return smoothstep(oceanWeight / EngineSetting.BIOME_SHORE_BUFFER_FULL_OCEAN_SHARE);
    }

    // Hashing \\

    public static long hashCell(long seed, int cellX, int cellZ) {
        return seed
                ^ (cellX * EngineSetting.HASH_FINALIZER_MULTIPLIER_1)
                ^ (cellZ * EngineSetting.HASH_FINALIZER_MULTIPLIER_2);
    }

    public static float hash01(long value) {
        return toUnit(mix64(value));
    }

    private static float toUnit(long hash) {
        return (float) ((hash >>> 11) / (double) (1L << 53));
    }

    private static long mix64(long value) {

        long hash = value;

        hash ^= (hash >>> 33);
        hash *= EngineSetting.HASH_FINALIZER_MULTIPLIER_1;
        hash ^= (hash >>> 33);
        hash *= EngineSetting.HASH_FINALIZER_MULTIPLIER_2;
        hash ^= (hash >>> 33);

        return hash;
    }

    // Math \\

    private static float contrast(float t) {

        return smoothstep((t - 0.5f) / EngineSetting.BIOME_BLEND_BAND_PIXELS + 0.5f);
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    private static float smoothstep(float t) {

        if (t <= 0f)
            return 0f;

        if (t >= 1f)
            return 1f;

        return t * t * (3f - 2f * t);
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