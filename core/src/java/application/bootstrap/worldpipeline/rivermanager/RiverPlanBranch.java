package application.bootstrap.worldpipeline.rivermanager;

import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biome.BiomeRiverStruct;
import application.bootstrap.worldpipeline.river.RiverNetworkStruct;
import application.bootstrap.worldpipeline.river.RiverPathStruct;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import application.bootstrap.worldpipeline.util.RiverPathUtility;
import application.bootstrap.worldpipeline.util.RoadPathUtility;
import application.bootstrap.worldpipeline.util.TerrainFeatureStruct;
import application.bootstrap.worldpipeline.util.TerrainShapeUtility;
import application.bootstrap.worldpipeline.util.TideUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldgenerationmanager.TerrainSurfaceSampleStruct;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

class RiverPlanBranch extends BranchPackage {

    /*
     * Async — settles one reach against the real land once, on whatever
     * thread first needs it. The river sets out from its cell's heart along
     * the line it arrived on and feels its way toward the next heart over the
     * ground, a few headings at a time, taking the lowest that does not turn
     * it from its goal, a pull that tightens as the goal nears and a corridor
     * it never leaves, so it finds the valleys between. The trace is rounded,
     * then wound in meanders that vanish at both hearts, wide on flat ground
     * and none in steep, their length following the river's width and their
     * swing its biome. Its level falls from one heart's to the next, cutting
     * into whatever rises above it by its biome's cut and never rising, never
     * below the highest tide. Where it meets the sea it ends in a curtain
     * pouring off its lip and a short trench the tide fills, and a reach its
     * biome sends underground runs as a tunnel wherever the land stands high
     * enough over it.
     */

    // Internal
    private WorldGenerationManager worldGenerationManager;
    private RiverPlanAsyncContainer planContainer;

    // Base \\

    @Override
    protected void create() {
        this.planContainer = create(RiverPlanAsyncContainer.class);
    }

    @Override
    protected void get() {
        this.worldGenerationManager = get(WorldGenerationManager.class);
    }

    // Plan \\

    RiverPathStruct plan(RiverNetworkStruct network, int sourceIndex) {

        RiverPlanAsyncContainer scratch = planContainer.getInstance();

        try {

            WorldHandle worldHandle = network.getWorldHandle();
            int targetIndex = network.getDownstreamIndex(sourceIndex);
            double originX = network.getNodeX(sourceIndex);
            double originZ = network.getNodeZ(sourceIndex);
            double endX = WorldWrapUtility.wrappedBlockDeltaX(worldHandle, network.getNodeX(targetIndex), originX);
            double endZ = WorldWrapUtility.wrappedBlockDeltaZ(worldHandle, network.getNodeZ(targetIndex), originZ);
            boolean seaward = !network.isRiver(targetIndex);

            int startLevel = network.getLevelBlocks(sourceIndex);
            int endLevel = seaward ? TideUtility.BAND_MAX_Y : Math.min(startLevel, network.getLevelBlocks(targetIndex));

            sampleLand(scratch, worldHandle, originX, originZ, 0.0, 0.0);
            BiomeRiverStruct sourceRiver = scratch.sample.getBlend().getDominantLandBiome().getRiver();
            float endFloor = endLevel + sourceRiver.getIncisionBlocks();

            traceReach(scratch, network, worldHandle, sourceIndex, originX, originZ, endX, endZ, endFloor);
            smoothLine(scratch);
            resampleLine(scratch);

            float startHalfWidth = RiverPathUtility.resolveHalfWidth(network.getStrength(sourceIndex));

            windLine(scratch, network, sourceIndex, sourceRiver.getMeander(), startHalfWidth);
            resampleLine(scratch);

            for (int i = 0; i < scratch.lineX.size(); i++)
                sampleLandPoint(scratch, worldHandle, originX, originZ, i);

            int mouth = findSea(scratch, 0);

            if (mouth == EngineSetting.INDEX_NOT_FOUND && seaward)
                mouth = extendLine(scratch, worldHandle, originX, originZ, EngineSetting.RIVER_MOUTH_SEARCH_BLOCKS,
                        true);

            if (mouth != EngineSetting.INDEX_NOT_FOUND)
                fitTrench(scratch, worldHandle, originX, originZ, mouth);

            return buildPath(scratch, network, sourceIndex, targetIndex, seaward, originX, originZ,
                    startLevel, endLevel, mouth, sourceRiver.getUnderground());
        } finally {
            scratch.reset();
        }
    }

    // Trace \\

    // Steps toward the next heart over the lowest ground among a fan of headings, the pull toward the heart rising
    // as it nears, the corridor about the straight line between the hearts never left; ground below where the
    // river must arrive counts as level with it, so the trace never sinks into a hollow it would have to climb from
    private void traceReach(
            RiverPlanAsyncContainer scratch,
            RiverNetworkStruct network,
            WorldHandle worldHandle,
            int sourceIndex,
            double originX,
            double originZ,
            double endX,
            double endZ,
            float endFloor) {

        double chordLength = Math.sqrt(endX * endX + endZ * endZ);
        double step = EngineSetting.RIVER_TRACE_STEP_BLOCKS;
        double corridor = EngineSetting.RIVER_TRACE_CORRIDOR * network.getCellSizeBlocks();
        int headings = EngineSetting.RIVER_TRACE_HEADINGS;
        int maxSteps = (int) Math.ceil(chordLength / step * EngineSetting.RIVER_TRACE_MAX_STEP_FACTOR);

        double headingX = endX / chordLength;
        double headingZ = endZ / chordLength;
        int upstreamIndex = network.findMainUpstream(sourceIndex);

        if (upstreamIndex != RiverNetworkStruct.INDEX_NONE) {

            double tangentX = endX - WorldWrapUtility.wrappedBlockDeltaX(
                    worldHandle, network.getNodeX(upstreamIndex), originX);
            double tangentZ = endZ - WorldWrapUtility.wrappedBlockDeltaZ(
                    worldHandle, network.getNodeZ(upstreamIndex), originZ);
            double tangentLength = Math.sqrt(tangentX * tangentX + tangentZ * tangentZ);

            if (tangentLength > EngineSetting.DIVISION_EPSILON) {
                headingX = tangentX / tangentLength;
                headingZ = tangentZ / tangentLength;
            }
        }

        double x = 0.0;
        double z = 0.0;

        scratch.lineX.add(x);
        scratch.lineZ.add(z);

        for (int stepIndex = 0; stepIndex < maxSteps; stepIndex++) {

            double toX = endX - x;
            double toZ = endZ - z;
            double remaining = Math.sqrt(toX * toX + toZ * toZ);

            if (remaining <= step * 1.5)
                break;

            toX /= remaining;
            toZ /= remaining;

            double pull = EngineSetting.RIVER_TRACE_PULL_BLOCKS * chordLength / Math.max(remaining, step);
            double bestScore = Double.MAX_VALUE;
            double bestX = toX;
            double bestZ = toZ;
            float lowest = Float.MAX_VALUE;
            float highest = -Float.MAX_VALUE;

            for (int k = 0; k < headings; k++) {

                double angle = (k - (headings - 1) * 0.5) * EngineSetting.RIVER_TRACE_TURN_RADIANS;
                double candidateX = RoadPathUtility.turnHeadingX(headingX, headingZ, angle);
                double candidateZ = RoadPathUtility.turnHeadingZ(headingX, headingZ, angle);
                double alignment = candidateX * toX + candidateZ * toZ;
                double probeX = x + candidateX * step;
                double probeZ = z + candidateZ * step;

                if (alignment <= 0.0 || Math.abs(endX * probeZ - endZ * probeX) / chordLength > corridor)
                    continue;

                float ground = sampleLand(scratch, worldHandle, originX, originZ, probeX, probeZ);
                double score = Math.max(ground, endFloor) + pull * (1.0 - alignment);

                lowest = Math.min(lowest, ground);
                highest = Math.max(highest, ground);

                if (score < bestScore) {
                    bestScore = score;
                    bestX = candidateX;
                    bestZ = candidateZ;
                }
            }

            if (highest >= lowest) {
                scratch.reliefSum += highest - lowest;
                scratch.reliefCount++;
            }

            headingX = bestX;
            headingZ = bestZ;
            x += headingX * step;
            z += headingZ * step;

            scratch.lineX.add(x);
            scratch.lineZ.add(z);
        }

        scratch.lineX.add(endX);
        scratch.lineZ.add(endZ);
    }

    // Shape \\

    // Corner cutting, both ends held where they are
    private void smoothLine(RiverPlanAsyncContainer scratch) {

        for (int pass = 0; pass < EngineSetting.RIVER_SMOOTHING_PASSES; pass++) {

            DoubleArrayList lineX = scratch.lineX;
            DoubleArrayList lineZ = scratch.lineZ;
            int count = lineX.size();

            if (count < 3)
                return;

            scratch.swapX.add(lineX.getDouble(0));
            scratch.swapZ.add(lineZ.getDouble(0));

            for (int i = 0; i < count - 1; i++) {

                double ax = lineX.getDouble(i);
                double az = lineZ.getDouble(i);
                double bx = lineX.getDouble(i + 1);
                double bz = lineZ.getDouble(i + 1);

                if (i > 0) {
                    scratch.swapX.add(ax * 0.75 + bx * 0.25);
                    scratch.swapZ.add(az * 0.75 + bz * 0.25);
                }

                if (i < count - 2) {
                    scratch.swapX.add(ax * 0.25 + bx * 0.75);
                    scratch.swapZ.add(az * 0.25 + bz * 0.75);
                }
            }

            scratch.swapX.add(lineX.getDouble(count - 1));
            scratch.swapZ.add(lineZ.getDouble(count - 1));
            scratch.swapLine();
        }
    }

    // The centreline walked again at an even spacing, its last point kept
    private void resampleLine(RiverPlanAsyncContainer scratch) {

        DoubleArrayList lineX = scratch.lineX;
        DoubleArrayList lineZ = scratch.lineZ;
        double spacing = EngineSetting.RIVER_POINT_SPACING_BLOCKS;
        double carried = 0.0;

        scratch.swapX.add(lineX.getDouble(0));
        scratch.swapZ.add(lineZ.getDouble(0));

        for (int i = 0; i < lineX.size() - 1; i++) {

            double ax = lineX.getDouble(i);
            double az = lineZ.getDouble(i);
            double segmentX = lineX.getDouble(i + 1) - ax;
            double segmentZ = lineZ.getDouble(i + 1) - az;
            double length = Math.sqrt(segmentX * segmentX + segmentZ * segmentZ);
            double along = spacing - carried;

            while (along < length) {
                scratch.swapX.add(ax + segmentX * along / length);
                scratch.swapZ.add(az + segmentZ * along / length);
                along += spacing;
            }

            carried = length - (along - spacing);
        }

        int last = lineX.size() - 1;

        if (carried > spacing * 0.5 || scratch.swapX.size() < 2) {
            scratch.swapX.add(lineX.getDouble(last));
            scratch.swapZ.add(lineZ.getDouble(last));
        } else {
            scratch.swapX.set(scratch.swapX.size() - 1, lineX.getDouble(last));
            scratch.swapZ.set(scratch.swapZ.size() - 1, lineZ.getDouble(last));
        }

        scratch.swapLine();
    }

    // Meanders across the centreline, a whole number of half waves so both hearts stay put, with an overtone that
    // breaks their regularity; their length follows the river's width and their swing its biome and how flat the
    // ground the trace crossed lay
    private void windLine(
            RiverPlanAsyncContainer scratch,
            RiverNetworkStruct network,
            int sourceIndex,
            float meander,
            float halfWidth) {

        DoubleArrayList lineX = scratch.lineX;
        DoubleArrayList lineZ = scratch.lineZ;
        int count = lineX.size();
        double length = (count - 1) * EngineSetting.RIVER_POINT_SPACING_BLOCKS;
        float relief = scratch.reliefCount > 0 ? scratch.reliefSum / scratch.reliefCount : 0f;
        float flatness = Math.max(0f, 1f - relief / EngineSetting.RIVER_MEANDER_RELIEF_BLOCKS);
        float wavelength = Math.max(EngineSetting.RIVER_MEANDER_MIN_WAVELENGTH_BLOCKS,
                Math.min(EngineSetting.RIVER_MEANDER_MAX_WAVELENGTH_BLOCKS,
                        EngineSetting.RIVER_MEANDER_WAVELENGTH_PER_WIDTH * halfWidth * 2f));
        float amplitude = Math.min(
                meander * flatness * EngineSetting.RIVER_MEANDER_MAX_AMPLITUDE_BLOCKS,
                wavelength * EngineSetting.RIVER_MEANDER_MAX_AMPLITUDE_PER_WAVELENGTH);

        if (count < 3 || amplitude <= 0f)
            return;

        long hash = BiomeFieldUtility.hashCell(network.getSeed() ^ EngineSetting.RIVER_MEANDER_SALT,
                network.getCellX(sourceIndex), network.getCellZ(sourceIndex));
        double sign = BiomeFieldUtility.hash01(hash) < 0.5f ? -1.0 : 1.0;
        double overtoneSign = BiomeFieldUtility.hash01(hash ^ EngineSetting.HASH_FINALIZER_MULTIPLIER_1) < 0.5f
                ? -1.0
                : 1.0;
        int halfWaves = Math.max(1, (int) Math.round(2.0 * length / wavelength));
        int overtoneWaves = halfWaves * 2 + 1;
        double share = EngineSetting.RIVER_MEANDER_OVERTONE_SHARE;

        for (int i = 0; i < count; i++) {

            int previous = Math.max(0, i - 1);
            int next = Math.min(count - 1, i + 1);
            double tangentX = lineX.getDouble(next) - lineX.getDouble(previous);
            double tangentZ = lineZ.getDouble(next) - lineZ.getDouble(previous);
            double tangentLength = Math.max(EngineSetting.DIVISION_EPSILON,
                    Math.sqrt(tangentX * tangentX + tangentZ * tangentZ));
            double phase = Math.PI * i / (count - 1);
            double offset = amplitude * sign
                    * (Math.sin(phase * halfWaves) + share * overtoneSign * Math.sin(phase * overtoneWaves))
                    / (1.0 + share);

            scratch.swapX.add(lineX.getDouble(i) - tangentZ / tangentLength * offset);
            scratch.swapZ.add(lineZ.getDouble(i) + tangentX / tangentLength * offset);
        }

        scratch.swapLine();
    }

    // Land \\

    // The bare ground at an offset from the origin, the sample left in scratch
    private float sampleLand(
            RiverPlanAsyncContainer scratch,
            WorldHandle worldHandle,
            double originX,
            double originZ,
            double offsetX,
            double offsetZ) {

        worldGenerationManager.sampleLand(
                worldHandle,
                WorldWrapUtility.wrapBlockX(worldHandle, originX + offsetX),
                WorldWrapUtility.wrapBlockZ(worldHandle, originZ + offsetZ),
                scratch.sample);

        return scratch.sample.getGroundHeightBlocks();
    }

    private void sampleLandPoint(
            RiverPlanAsyncContainer scratch,
            WorldHandle worldHandle,
            double originX,
            double originZ,
            int index) {

        TerrainSurfaceSampleStruct sample = scratch.sample;
        float ground = sampleLand(scratch, worldHandle, originX, originZ,
                scratch.lineX.getDouble(index), scratch.lineZ.getDouble(index));

        scratch.ground.add(ground);
        scratch.sea.add(TerrainShapeUtility.isOceanReached(sample.getFeatures()));
        scratch.lakeLevels.add(sample.isLakeWater()
                ? (int) sample.getWaterSurfaceBlocks()
                : TerrainFeatureStruct.LAKE_LEVEL_UNDEFINED);
        scratch.pointBiomes.add(sample.getBlend().getDominantLandBiome());
    }

    private int findSea(RiverPlanAsyncContainer scratch, int from) {

        for (int i = from; i < scratch.sea.size(); i++)
            if (scratch.sea.getBoolean(i))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // The centreline carried straight on along its last heading for up to a length, each new point sampled;
    // returns the first new point the sea reaches when asked to stop there, INDEX_NOT_FOUND otherwise
    private int extendLine(
            RiverPlanAsyncContainer scratch,
            WorldHandle worldHandle,
            double originX,
            double originZ,
            double lengthBlocks,
            boolean stopAtSea) {

        DoubleArrayList lineX = scratch.lineX;
        DoubleArrayList lineZ = scratch.lineZ;
        int last = lineX.size() - 1;
        double headingX = lineX.getDouble(last) - lineX.getDouble(last - 1);
        double headingZ = lineZ.getDouble(last) - lineZ.getDouble(last - 1);
        double headingLength = Math.max(EngineSetting.DIVISION_EPSILON,
                Math.sqrt(headingX * headingX + headingZ * headingZ));
        double spacing = EngineSetting.RIVER_POINT_SPACING_BLOCKS;
        int steps = (int) Math.ceil(lengthBlocks / spacing);

        headingX /= headingLength;
        headingZ /= headingLength;

        for (int step = 1; step <= steps; step++) {

            lineX.add(lineX.getDouble(last) + headingX * spacing * step);
            lineZ.add(lineZ.getDouble(last) + headingZ * spacing * step);
            sampleLandPoint(scratch, worldHandle, originX, originZ, lineX.size() - 1);

            if (stopAtSea && scratch.sea.getBoolean(lineX.size() - 1))
                return lineX.size() - 1;
        }

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Past the mouth the reach keeps only the curtain and the trench, carried on straight where it runs out first
    private void fitTrench(
            RiverPlanAsyncContainer scratch,
            WorldHandle worldHandle,
            double originX,
            double originZ,
            int mouth) {

        int trenchPoints = (int) Math.ceil(
                EngineSetting.RIVER_MOUTH_TRENCH_BLOCKS / EngineSetting.RIVER_POINT_SPACING_BLOCKS);
        int keep = Math.max(mouth + 1, mouth + trenchPoints);

        if (scratch.lineX.size() > keep) {
            scratch.lineX.size(keep);
            scratch.lineZ.size(keep);
            scratch.ground.size(keep);
            scratch.sea.size(keep);
            scratch.lakeLevels.size(keep);
            scratch.pointBiomes.size(keep);
            return;
        }

        double missing = (keep - scratch.lineX.size()) * EngineSetting.RIVER_POINT_SPACING_BLOCKS;

        if (missing > 0.0 && scratch.lineX.size() >= 2)
            extendLine(scratch, worldHandle, originX, originZ, missing, false);
    }

    // Build \\

    private RiverPathStruct buildPath(
            RiverPlanAsyncContainer scratch,
            RiverNetworkStruct network,
            int sourceIndex,
            int targetIndex,
            boolean seaward,
            double originX,
            double originZ,
            int startLevel,
            int endLevel,
            int mouth,
            float underground) {

        int count = scratch.lineX.size();
        int landCount = mouth == EngineSetting.INDEX_NOT_FOUND ? count : mouth;
        double spacing = EngineSetting.RIVER_POINT_SPACING_BLOCKS;

        float[] pointX = new float[count];
        float[] pointZ = new float[count];
        float[] distances = new float[count];
        short[] levels = new short[count];
        float[] halfWidths = new float[count];
        float[] depths = new float[count];
        byte[] zones = new byte[count];
        byte[] biomeIndices = new byte[count];

        int sourceStrength = network.getStrength(sourceIndex);
        int targetStrength = seaward ? sourceStrength : network.getStrength(targetIndex);
        int running = startLevel;
        float reach = 0f;

        for (int i = 0; i < count; i++) {

            BiomeHandle biome = scratch.pointBiomes.get(i);
            BiomeRiverStruct river = biome.getRiver();
            float t = count > 1 ? i / (float) (count - 1) : 0f;
            float strength = sourceStrength + (targetStrength - sourceStrength) * t;
            float halfWidth = RiverPathUtility.resolveHalfWidth(strength) * river.getWidthScale();

            pointX[i] = (float) scratch.lineX.getDouble(i);
            pointZ[i] = (float) scratch.lineZ.getDouble(i);
            distances[i] = (float) (i * spacing);
            halfWidths[i] = Math.max(EngineSetting.RIVER_MIN_HALF_WIDTH_BLOCKS,
                    Math.min(EngineSetting.RIVER_MAX_HALF_WIDTH_BLOCKS, halfWidth));
            depths[i] = Math.min(EngineSetting.RIVER_MAX_DEPTH_BLOCKS,
                    RiverPathUtility.resolveDepth(strength) * river.getDepthScale());
            biomeIndices[i] = resolveBiomeIndex(scratch, biome);

            if (i < landCount) {

                int lakeLevel = scratch.lakeLevels.getInt(i);
                float surface = lakeLevel != TerrainFeatureStruct.LAKE_LEVEL_UNDEFINED
                        ? lakeLevel
                        : scratch.ground.getFloat(i);

                running = Math.max(endLevel, Math.min(running, (int) Math.floor(surface) - river.getIncisionBlocks()));
                zones[i] = RiverPathStruct.ZONE_OPEN;
            } else {
                zones[i] = distances[i] - distances[landCount] < EngineSetting.RIVER_CURTAIN_BLOCKS
                        ? RiverPathStruct.ZONE_CURTAIN
                        : RiverPathStruct.ZONE_TRENCH;
            }

            levels[i] = (short) running;
            reach = Math.max(reach,
                    halfWidths[i] + river.getFloodplainBlocks() + EngineSetting.RIVER_MAX_VALLEY_BLOCKS);
        }

        if (rollsUnderground(network, sourceIndex, underground))
            resolveTunnels(scratch, levels, zones, landCount);

        return new RiverPathStruct(
                network, sourceIndex, originX, originZ,
                pointX, pointZ, distances, levels, halfWidths, depths, zones, biomeIndices,
                scratch.biomeTable.toArray(new BiomeHandle[0]), reach);
    }

    // Wherever the land stands its biome's headroom and a roof above the water, away from still water, the reach
    // runs underground; a tunnel too short to be worth one stays open
    private void resolveTunnels(RiverPlanAsyncContainer scratch, short[] levels, byte[] zones, int landCount) {

        for (int i = 0; i < landCount; i++) {

            int headroom = scratch.pointBiomes.get(i).getRiver().getTunnelHeadroomBlocks();
            boolean covered = scratch.ground.getFloat(i) - levels[i]
                    >= headroom + EngineSetting.RIVER_TUNNEL_MIN_COVER_BLOCKS;

            if (covered && scratch.lakeLevels.getInt(i) == TerrainFeatureStruct.LAKE_LEVEL_UNDEFINED)
                zones[i] = RiverPathStruct.ZONE_TUNNEL;
        }

        int start = 0;

        while (start < landCount) {

            int end = start;

            while (end + 1 < landCount && zones[end + 1] == zones[start])
                end++;

            if (zones[start] == RiverPathStruct.ZONE_TUNNEL && end - start + 1 < EngineSetting.RIVER_TUNNEL_MIN_POINTS)
                for (int i = start; i <= end; i++)
                    zones[i] = RiverPathStruct.ZONE_OPEN;

            start = end + 1;
        }
    }

    private boolean rollsUnderground(RiverNetworkStruct network, int sourceIndex, float underground) {

        if (underground <= 0f)
            return false;

        long hash = BiomeFieldUtility.hashCell(network.getSeed() ^ EngineSetting.RIVER_TUNNEL_SALT,
                network.getCellX(sourceIndex), network.getCellZ(sourceIndex));

        return BiomeFieldUtility.hash01(hash) < underground;
    }

    private byte resolveBiomeIndex(RiverPlanAsyncContainer scratch, BiomeHandle biome) {

        int index = scratch.biomeTable.indexOf(biome);

        if (index == EngineSetting.INDEX_NOT_FOUND) {

            if (scratch.biomeTable.size() > Byte.MAX_VALUE)
                return 0;

            index = scratch.biomeTable.size();
            scratch.biomeTable.add(biome);
        }

        return (byte) index;
    }
}
