package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.biome.ProbableBiomeStruct;
import application.bootstrap.worldpipeline.biome.ProbableEdgeStruct;
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
     * where they overlap, unless they are exclusive: then a patch holds no
     * share within the reach of one of lower hash and returns across a divide
     * beyond it, so each stays a body of its own and the two part on ground
     * neither holds. Each entry's edge decides how wide the fade across the
     * outline runs, and a sharpness mask drawn per patch narrows it along
     * stretches of the outline into cliffs that fade back into rolling
     * ground. Everything depends only on seed and position, so neighboring
     * chunks agree exactly.
     */

    public static final int MAP_SAMPLE_COUNT = EngineSetting.BIOME_MAP_SAMPLE_COUNT;

    private static final double BLOCKS_PER_PIXEL = EngineSetting.CHUNKS_PER_PIXEL * (double) EngineSetting.CHUNK_SIZE;
    private static final double SHAPE_BOUND_RADIUS = 0.5;
    private static final double SHAPE_EXTENT_RADIUS = SHAPE_BOUND_RADIUS
            + EngineSetting.BIOME_PROBABLE_SHAPE_WARP_STRENGTH;
    private static final double SHAPE_MAX_CAPSULE_RADIUS = Math.max(
            EngineSetting.BIOME_PROBABLE_SHAPE_BODY_MAX_RADIUS,
            EngineSetting.BIOME_PROBABLE_SHAPE_ARM_MAX_RADIUS * EngineSetting.BIOME_PROBABLE_SHAPE_ARM_MAX_TAPER);
    private static final float[] SHAPE_AREA_BY_ARMS = measureShapeAreas();

    // A patch's draws follow its hash in this order
    private static final int DRAW_CENTER_X = 1;
    private static final int DRAW_CENTER_Z = 2;
    private static final int DRAW_DIAMETER = 3;
    private static final int DRAW_ARMS = 4;

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

    // How far past its outline, in its own outline metric, a patch with this edge still holds a share
    public static float computePatchReach(ProbableEdgeStruct edge) {
        return 1f + edge.getMaxBand() + edge.getRoughness();
    }

    // The radius around its center, in patch diameters, beyond which a patch of that reach holds nothing
    public static double computeShapeReachRadius(float patchReach) {
        return SHAPE_BOUND_RADIUS
                + SHAPE_MAX_CAPSULE_RADIUS * (patchReach - 1f)
                + EngineSetting.BIOME_PROBABLE_SHAPE_WARP_STRENGTH;
    }

    // At least a patch's reach, and wider for sparse patches so most positions only touch one cell
    public static double computeScatterCellSizeBlocks(
            double patchDensity,
            float maxSizeBlocks,
            double shapeReachRadius) {

        double reachBlocks = maxSizeBlocks * shapeReachRadius;

        if (patchDensity <= 0.0)
            return reachBlocks;

        return Math.max(reachBlocks, Math.sqrt(EngineSetting.BIOME_PROBABLE_MAX_CELL_OCCUPANCY / patchDensity));
    }

    // Exclusive patches never share ground with another of their biome, so each stays a body of its own
    public static void computeScatterCoverage(
            long seed,
            ProbableBiomeStruct probableBiome,
            double pixelX, double pixelZ,
            int mapWidth, int mapHeight,
            boolean exclusive,
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

        double shapeReachRadius = probableBiome.getShapeReachRadius();
        double maxReachBlocks = probableBiome.getMaxSizeBlocks() * shapeReachRadius;

        int firstCellX = floorToInt((blockX - maxReachBlocks) / cellWidthBlocks);
        int lastCellX = floorToInt((blockX + maxReachBlocks) / cellWidthBlocks);
        int firstCellZ = floorToInt((blockZ - maxReachBlocks) / cellHeightBlocks);
        int lastCellZ = floorToInt((blockZ + maxReachBlocks) / cellHeightBlocks);

        long scatterSeed = seed ^ mix64(probableBiome.getNameSeed());
        ProbableEdgeStruct edge = probableBiome.getEdge();

        float uncovered = 1f;
        float bestMembership = 0f;
        float membershipSum = 0f;
        float crestSum = 0f;

        for (int cellZ = firstCellZ; cellZ <= lastCellZ; cellZ++) {
            for (int cellX = firstCellX; cellX <= lastCellX; cellX++) {

                long draw = drawCell(scatterSeed, cellX, cellZ, cellCountX, cellCountZ);
                int patchCount = drawPatchCount(draw, meanPatchesPerCell, emptyCellChance);

                for (int patch = 0; patch < patchCount; patch++) {

                    long patchHash = mix64(draw + patch + 1);
                    double centerX = drawPatchCoordinate(cellX, patchHash, DRAW_CENTER_X, cellWidthBlocks);
                    double centerZ = drawPatchCoordinate(cellZ, patchHash, DRAW_CENTER_Z, cellHeightBlocks);
                    double offsetX = blockX - centerX;
                    double offsetZ = blockZ - centerZ;

                    if (offsetX * offsetX + offsetZ * offsetZ >= maxReachBlocks * maxReachBlocks)
                        continue;

                    double diameter = drawPatchDiameter(probableBiome, patchHash);

                    double shapeX = offsetX / diameter;
                    double shapeZ = offsetZ / diameter;

                    if (shapeX * shapeX + shapeZ * shapeZ >= shapeReachRadius * shapeReachRadius)
                        continue;

                    int arms = drawPatchArms(probableBiome, patchHash);
                    long shapeHash = patchHash ^ EngineSetting.BIOME_PROBABLE_SHAPE_SEED;

                    float shape = computeShapeMetric(shapeHash, arms, shapeX, shapeZ);

                    if (shape >= probableBiome.getPatchReach())
                        continue;

                    float noise = computeEdgeNoise(shapeHash, shapeX, shapeZ);
                    float edgeMetric = shape - noise * edge.getRoughness();
                    float sharpness = computeSharpness(shapeHash, edge, shapeX, shapeZ);
                    float membership = computeOutlineMembership(edgeMetric, edge, sharpness);

                    if (exclusive && membership > 0f)
                        membership *= computeExclusiveShare(
                                scatterSeed, probableBiome,
                                cellCountX, cellCountZ, cellWidthBlocks, cellHeightBlocks,
                                meanPatchesPerCell, emptyCellChance,
                                blockX, blockZ, patchHash);

                    if (membership <= 0f)
                        continue;

                    uncovered *= 1f - membership;
                    membershipSum += membership;
                    crestSum += membership * computeCrest(edgeMetric, edge, sharpness);

                    if (membership > bestMembership) {
                        bestMembership = membership;
                        outPatch.set(0f, patchHash, shape, noise, shapeX, shapeZ, centerX, centerZ);
                    }
                }
            }
        }

        if (probableBiome.isInverted()) {
            outPatch.set(uncovered, 0L, ProbablePatchStruct.NO_PATCH_SHAPE, 0f, 0.0, 0.0, 0.0, 0.0);
            return;
        }

        outPatch.setCoverage(1f - uncovered);

        if (membershipSum > 0f)
            outPatch.setCrest(crestSum / membershipSum);
    }

    // A core inside the host patch, its outline the host's shrunk by a drawn scale, sharing the host's center
    public static void computeCenterCoverage(
            ProbableBiomeStruct probableBiome,
            ProbablePatchStruct hostPatch,
            ProbablePatchStruct outPatch) {

        outPatch.reset();

        if (!hostPatch.hasPatch())
            return;

        long coreHash = mix64(hostPatch.getPatchHash()
                ^ mix64(probableBiome.getNameSeed())
                ^ EngineSetting.BIOME_PROBABLE_CORE_SEED);

        if (toUnit(coreHash) >= probableBiome.getChance())
            return;

        float scale = lerp(
                probableBiome.getMinCoreScale(), probableBiome.getMaxCoreScale(), toUnit(mix64(coreHash)));

        ProbableEdgeStruct edge = probableBiome.getEdge();
        float coreShape = hostPatch.getPatchShape() / scale;
        double coreX = hostPatch.getPatchX() / scale;
        double coreZ = hostPatch.getPatchZ() / scale;
        float edgeMetric = coreShape - hostPatch.getPatchNoise() * edge.getRoughness();
        float sharpness = computeSharpness(coreHash, edge, coreX, coreZ);
        float membership = computeOutlineMembership(edgeMetric, edge, sharpness);

        if (membership <= 0f)
            return;

        outPatch.set(
                membership, coreHash, coreShape, hostPatch.getPatchNoise(),
                coreX, coreZ, hostPatch.getCenterX(), hostPatch.getCenterZ());
        outPatch.setCrest(computeCrest(edgeMetric, edge, sharpness));
    }

    // The share a patch keeps against every patch of lower hash: none within their reach, returning across the
    // divide beyond it
    private static float computeExclusiveShare(
            long scatterSeed,
            ProbableBiomeStruct probableBiome,
            int cellCountX, int cellCountZ,
            double cellWidthBlocks, double cellHeightBlocks,
            double meanPatchesPerCell, double emptyCellChance,
            double blockX, double blockZ,
            long patchHash) {

        ProbableEdgeStruct edge = probableBiome.getEdge();
        double reachRadius = probableBiome.getExclusiveReachRadius();
        double maxReachBlocks = probableBiome.getMaxSizeBlocks() * reachRadius;
        float divideStart = 1f + edge.getMaxBand();

        int firstCellX = floorToInt((blockX - maxReachBlocks) / cellWidthBlocks);
        int lastCellX = floorToInt((blockX + maxReachBlocks) / cellWidthBlocks);
        int firstCellZ = floorToInt((blockZ - maxReachBlocks) / cellHeightBlocks);
        int lastCellZ = floorToInt((blockZ + maxReachBlocks) / cellHeightBlocks);

        float share = 1f;

        for (int cellZ = firstCellZ; cellZ <= lastCellZ; cellZ++) {
            for (int cellX = firstCellX; cellX <= lastCellX; cellX++) {

                long draw = drawCell(scatterSeed, cellX, cellZ, cellCountX, cellCountZ);
                int patchCount = drawPatchCount(draw, meanPatchesPerCell, emptyCellChance);

                for (int patch = 0; patch < patchCount; patch++) {

                    long otherHash = mix64(draw + patch + 1);

                    if (otherHash >= patchHash)
                        continue;

                    double offsetX = blockX - drawPatchCoordinate(cellX, otherHash, DRAW_CENTER_X, cellWidthBlocks);
                    double offsetZ = blockZ - drawPatchCoordinate(cellZ, otherHash, DRAW_CENTER_Z, cellHeightBlocks);

                    if (offsetX * offsetX + offsetZ * offsetZ >= maxReachBlocks * maxReachBlocks)
                        continue;

                    double diameter = drawPatchDiameter(probableBiome, otherHash);
                    double shapeX = offsetX / diameter;
                    double shapeZ = offsetZ / diameter;

                    if (shapeX * shapeX + shapeZ * shapeZ >= reachRadius * reachRadius)
                        continue;

                    long shapeHash = otherHash ^ EngineSetting.BIOME_PROBABLE_SHAPE_SEED;
                    float shape = computeShapeMetric(
                            shapeHash, drawPatchArms(probableBiome, otherHash), shapeX, shapeZ);
                    float edgeMetric = shape - computeEdgeNoise(shapeHash, shapeX, shapeZ) * edge.getRoughness();

                    share *= smoothstep((edgeMetric - divideStart) / EngineSetting.BIOME_PROBABLE_EXCLUSIVE_DIVIDE);

                    if (share <= 0f)
                        return 0f;
                }
            }
        }

        return share;
    }

    private static int computeScatterCellCount(double worldExtentBlocks, double cellSizeBlocks) {
        return Math.max(1, (int) (worldExtentBlocks / cellSizeBlocks));
    }

    private static long drawCell(long scatterSeed, int cellX, int cellZ, int cellCountX, int cellCountZ) {
        return mix64(hashCell(scatterSeed, wrapIndex(cellX, cellCountX), wrapIndex(cellZ, cellCountZ)));
    }

    // The draw a patch takes at its place in the order, mixed on from its hash
    private static long drawPatch(long patchHash, int order) {

        long draw = patchHash;

        for (int i = 0; i < order; i++)
            draw = mix64(draw);

        return draw;
    }

    private static double drawPatchCoordinate(int cell, long patchHash, int order, double cellSizeBlocks) {
        return (cell + (double) toUnit(drawPatch(patchHash, order))) * cellSizeBlocks;
    }

    // Diameters drawn log-uniformly between the entry's two sizes
    private static double drawPatchDiameter(ProbableBiomeStruct probableBiome, long patchHash) {
        return probableBiome.getMinSizeBlocks()
                * Math.exp(probableBiome.getSizeLogRatio() * toUnit(drawPatch(patchHash, DRAW_DIAMETER)));
    }

    private static int drawPatchArms(ProbableBiomeStruct probableBiome, long patchHash) {
        return drawArms(drawPatch(patchHash, DRAW_ARMS), probableBiome.getMinArms(), probableBiome.getMaxArms());
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

    // Half the share at the outline whatever the band, so only the steepness of the fade follows sharpness
    private static float computeOutlineMembership(float edgeMetric, ProbableEdgeStruct edge, float sharpness) {

        float band = lerp(edge.getSoftBand(), edge.getSharpBand(), sharpness);

        return 1f - smoothstep((edgeMetric - (1f - band)) / (2f * band));
    }

    // 0 where the outline rolls and 1 where it breaks sharp, fading from one to the other along the outline
    private static float computeSharpness(long shapeHash, ProbableEdgeStruct edge, double x, double z) {

        if (!edge.hasSharpStretches())
            return 0f;

        if (edge.getSharpShare() >= 1f)
            return 1f;

        double frequency = 1.0 / edge.getSharpWavelength();
        float noise = NoiseUtility.noise2(
                shapeHash ^ EngineSetting.BIOME_PROBABLE_EDGE_SHARP_SEED, x * frequency, z * frequency);
        float threshold = (1f - 2f * edge.getSharpShare()) * EngineSetting.BIOME_PROBABLE_EDGE_SHARP_SPAN;

        return smoothstep((noise - threshold) / EngineSetting.BIOME_PROBABLE_EDGE_SHARP_FADE + 0.5f);
    }

    // A dome gathered toward the patch's heart; wherever the outline breaks sharp it climbs its sharp rise at the
    // outline at once and the dome carries on from there, so a scarp never leaves a level top behind it
    private static float computeCrest(float edgeMetric, ProbableEdgeStruct edge, float sharpness) {
        return 1f - (1f - smoothstep(1f - edgeMetric)) * (1f - sharpness * edge.getSharpRise());
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