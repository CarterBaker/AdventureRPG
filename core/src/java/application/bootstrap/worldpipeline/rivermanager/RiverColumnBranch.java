package application.bootstrap.worldpipeline.rivermanager;

import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biome.BiomeRiverStruct;
import application.bootstrap.worldpipeline.river.RiverColumnStruct;
import application.bootstrap.worldpipeline.river.RiverGatherStruct;
import application.bootstrap.worldpipeline.river.RiverNetworkStruct;
import application.bootstrap.worldpipeline.river.RiverPathStruct;
import application.bootstrap.worldpipeline.river.RiverQueryStruct;
import application.bootstrap.worldpipeline.util.RiverPathUtility;
import application.bootstrap.worldpipeline.util.TideUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class RiverColumnBranch extends BranchPackage {

    /*
     * Async — what the rivers make of a column. The reaches that can touch a
     * span are gathered once from the drainage cells about it, a reach planned
     * only once the hearts it joins show it can reach the span, and each
     * narrowed to the segments that can lie nearest any column of it. A
     * column then takes its shape from the reach
     * whose channel lies nearest: inside the channel the bed dips deepest at
     * the middle and the water stands at the reach's level; beside it a flat
     * bank one block above the water, raised where the land falls short so
     * the water is always held; beyond, the cut's walls rise at the biome's
     * slope and break into a rim where the cut gives out, every nearby
     * open reach cutting its own. Underground, an arched tunnel takes the
     * channel's place, caves kept above its water. At the sea the curtain
     * holds the river's level over the lip and the trench past it is cut
     * below the lowest tide for the sea to fill. Still water keeps its own
     * columns, and its rim is never cut below its level. A sample spaced too
     * widely to show a river's banks reads the drainage directly instead,
     * each river a straight run between its hearts shown only where it is
     * wide enough to matter at that spacing, so maps and distant terrain
     * never plan a reach.
     */

    // Internal
    private RiverNetworkBranch riverNetworkBranch;
    private RiverCacheBranch riverCacheBranch;
    private RiverQueryAsyncContainer queryContainer;

    // Base \\

    @Override
    protected void create() {
        this.queryContainer = create(RiverQueryAsyncContainer.class);
    }

    @Override
    protected void get() {
        this.riverNetworkBranch = get(RiverNetworkBranch.class);
        this.riverCacheBranch = get(RiverCacheBranch.class);
    }

    // Gather \\

    // Every reach that can touch a span of world blocks, inclusive, from the drainage cells about its first corner
    void gather(WorldHandle worldHandle, double fromX, double fromZ, double spanBlocks, RiverGatherStruct out) {

        out.clear(fromX, fromZ);

        RiverNetworkStruct network = riverNetworkBranch.getNetwork(worldHandle);
        int cellX = network.toCellX(fromX);
        int cellZ = network.toCellZ(fromZ);

        if (!network.isNear(network.toIndex(cellX, cellZ)))
            return;

        int radius = RiverNetworkStruct.NEAR_RADIUS_CELLS;

        for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
            for (int offsetX = -radius; offsetX <= radius; offsetX++) {

                int index = network.toIndex(cellX + offsetX, cellZ + offsetZ);

                if (!network.isRiver(index) || !canReach(network, worldHandle, index, fromX, fromZ, spanBlocks))
                    continue;

                RiverPathStruct path = riverCacheBranch.getPath(network, index);
                double localFromX = WorldWrapUtility.wrappedBlockDeltaX(worldHandle, fromX, path.getOriginX());
                double localFromZ = WorldWrapUtility.wrappedBlockDeltaZ(worldHandle, fromZ, path.getOriginZ());
                double localToX = localFromX + spanBlocks;
                double localToZ = localFromZ + spanBlocks;

                if (!path.reaches(localFromX, localFromZ, localToX, localToZ))
                    continue;

                int firstSegment = RiverPathUtility.findFirstSegment(
                        path, localFromX, localFromZ, localToX, localToZ, path.getReachBlocks());

                if (firstSegment == RiverPathUtility.SEGMENT_NONE)
                    continue;

                int lastSegment = RiverPathUtility.findLastSegment(
                        path, localFromX, localFromZ, localToX, localToZ, path.getReachBlocks());

                addNarrowed(path, localFromX, localFromZ, spanBlocks, firstSegment, lastSegment, out);
            }
        }
    }

    // Whether a reach could touch a span, judged from its two hearts alone before it is ever planned: its trace
    // keeps within a corridor about the line between them, its meanders swing no further than their widest and its
    // valley cuts no wider than its widest, and a reach to the sea runs on past its last heart to find the shore
    private boolean canReach(
            RiverNetworkStruct network,
            WorldHandle worldHandle,
            int index,
            double fromX,
            double fromZ,
            double spanBlocks) {

        int target = network.getDownstreamIndex(index);
        double originX = network.getNodeX(index);
        double originZ = network.getNodeZ(index);
        double endX = WorldWrapUtility.wrappedBlockDeltaX(worldHandle, network.getNodeX(target), originX);
        double endZ = WorldWrapUtility.wrappedBlockDeltaZ(worldHandle, network.getNodeZ(target), originZ);
        double localFromX = WorldWrapUtility.wrappedBlockDeltaX(worldHandle, fromX, originX);
        double localFromZ = WorldWrapUtility.wrappedBlockDeltaZ(worldHandle, fromZ, originZ);
        double bound = network.getCellSizeBlocks() * EngineSetting.RIVER_TRACE_CORRIDOR
                + EngineSetting.RIVER_TRACE_STEP_BLOCKS
                + EngineSetting.RIVER_MEANDER_MAX_AMPLITUDE_BLOCKS
                + EngineSetting.RIVER_MAX_HALF_WIDTH_BLOCKS
                + EngineSetting.RIVER_MAX_FLOODPLAIN_BLOCKS
                + EngineSetting.RIVER_MAX_VALLEY_BLOCKS;

        if (!network.isRiver(target))
            bound += EngineSetting.RIVER_MOUTH_SEARCH_BLOCKS + EngineSetting.RIVER_MOUTH_TRENCH_BLOCKS;

        return localFromX + spanBlocks >= Math.min(0.0, endX) - bound
                && localFromX <= Math.max(0.0, endX) + bound
                && localFromZ + spanBlocks >= Math.min(0.0, endZ) - bound
                && localFromZ <= Math.max(0.0, endZ) + bound;
    }

    // A span's run narrowed to the segments that can lie nearest any of its columns: none further from its centre
    // than the nearest segment to the centre, plus the span's half diagonal twice over
    private void addNarrowed(
            RiverPathStruct path,
            double localFromX,
            double localFromZ,
            double spanBlocks,
            int firstSegment,
            int lastSegment,
            RiverGatherStruct out) {

        if (spanBlocks <= 0.0) {
            out.add(path, firstSegment, lastSegment, localFromX, localFromZ);
            return;
        }

        RiverQueryStruct query = queryContainer.<RiverQueryAsyncContainer>getInstance().query;
        double centerX = localFromX + spanBlocks * 0.5;
        double centerZ = localFromZ + spanBlocks * 0.5;
        double halfDiagonal = (spanBlocks + 1.0) * Math.sqrt(0.5);
        double limit = RiverPathUtility.locate(path, centerX, centerZ, firstSegment, lastSegment, query)
                + halfDiagonal * 2.0;

        out.add(path,
                RiverPathUtility.findFirstSegmentWithin(path, centerX, centerZ, firstSegment, lastSegment, limit),
                RiverPathUtility.findLastSegmentWithin(path, centerX, centerZ, firstSegment, lastSegment, limit),
                localFromX, localFromZ);
    }

    // The thread's own gather for one point
    RiverGatherStruct gatherPoint(WorldHandle worldHandle, double worldX, double worldZ) {

        RiverGatherStruct pointGather = queryContainer.<RiverQueryAsyncContainer>getInstance().pointGather;

        gather(worldHandle, worldX, worldZ, 0.0, pointGather);

        return pointGather;
    }

    // Column \\

    void resolveColumn(
            RiverGatherStruct gather,
            double worldX,
            double worldZ,
            int groundHeight,
            boolean oceanWater,
            boolean lakeWater,
            int lakeRimLevel,
            RiverColumnStruct out) {

        out.reset(groundHeight);

        if (gather.isEmpty() || lakeWater)
            return;

        RiverQueryStruct query = queryContainer.<RiverQueryAsyncContainer>getInstance().query;

        double nearestEdge = Double.MAX_VALUE;
        double distance = 0.0;
        int level = 0;
        float halfWidth = 0f;
        float depth = 0f;
        byte zone = RiverPathStruct.ZONE_OPEN;
        BiomeHandle biome = null;
        int carveTop = Integer.MAX_VALUE;

        for (int i = 0; i < gather.size(); i++) {

            RiverPathStruct path = gather.getPath(i);
            double localX = gather.toLocalX(i, worldX);
            double localZ = gather.toLocalZ(i, worldZ);
            double pathDistance = RiverPathUtility.locate(
                    path, localX, localZ, gather.getFirstSegment(i), gather.getLastSegment(i), query);

            if (pathDistance > path.getReachBlocks())
                continue;

            if (query.getZone() == RiverPathStruct.ZONE_OPEN)
                carveTop = Math.min(carveTop, resolveCarveTop(query, pathDistance));

            double edge = pathDistance - query.getHalfWidthBlocks();

            if (edge >= nearestEdge)
                continue;

            nearestEdge = edge;
            distance = pathDistance;
            level = query.getLevelBlocks();
            halfWidth = query.getHalfWidthBlocks();
            depth = query.getDepthBlocks();
            zone = query.getZone();
            biome = query.getBiome();
        }

        if (biome == null)
            return;

        int ground = groundHeight;
        byte columnZone = RiverColumnStruct.ZONE_NONE;

        if (carveTop < ground) {
            ground = carveTop;
            columnZone = RiverColumnStruct.ZONE_WALL;
        }

        BiomeRiverStruct river = biome.getRiver();
        boolean inChannel = distance < halfWidth;
        int bed = level - resolveDepthAt(distance, halfWidth, depth);
        int water = RiverColumnStruct.NONE;

        switch (zone) {

            case RiverPathStruct.ZONE_OPEN -> {

                if (inChannel) {
                    ground = Math.min(ground, bed);
                    columnZone = RiverColumnStruct.ZONE_CHANNEL;
                    water = oceanWater ? RiverColumnStruct.NONE : level;
                } else if (distance < halfWidth + resolveBankBlocks(river)) {
                    ground = oceanWater ? Math.min(ground, level + 1) : level + 1;
                    columnZone = RiverColumnStruct.ZONE_BANK;
                }

                if (distance < halfWidth + resolveBankBlocks(river) + EngineSetting.RIVER_CAVE_GUARD_BLOCKS)
                    out.setCaveCeilingMaxY(level - Math.round(depth) - EngineSetting.RIVER_CAVE_GUARD_DEPTH_BLOCKS);
            }

            case RiverPathStruct.ZONE_TUNNEL -> {

                if (inChannel && ground <= level) {
                    ground = Math.min(ground, bed);
                    columnZone = RiverColumnStruct.ZONE_CHANNEL;
                    water = level;
                } else if (inChannel) {
                    int ceiling = level + Math.max(1, Math.round(river.getTunnelHeadroomBlocks()
                            * (float) Math.sqrt(1.0 - (distance / halfWidth) * (distance / halfWidth))));

                    out.setTunnel(bed + 1, Math.min(ceiling, ground), level);
                    columnZone = RiverColumnStruct.ZONE_TUNNEL;
                }

                if (distance < halfWidth + EngineSetting.RIVER_CAVE_GUARD_BLOCKS)
                    out.setCaveFloorMinY(level + 1);
            }

            case RiverPathStruct.ZONE_CURTAIN -> {

                if (inChannel) {
                    ground = Math.min(ground, bed);
                    columnZone = RiverColumnStruct.ZONE_CURTAIN;
                    water = level;
                }
            }

            default -> {

                if (inChannel && oceanWater) {
                    ground = Math.min(ground, TideUtility.BAND_MIN_Y - resolveDepthAt(distance, halfWidth, depth));
                    columnZone = RiverColumnStruct.ZONE_TRENCH;
                }
            }
        }

        if (lakeRimLevel != RiverColumnStruct.NONE && ground < lakeRimLevel) {
            ground = lakeRimLevel;
            columnZone = columnZone == RiverColumnStruct.ZONE_NONE ? RiverColumnStruct.ZONE_WALL : columnZone;
        }

        if (water != RiverColumnStruct.NONE && water > ground)
            out.setWaterLevel(water);

        if (columnZone != RiverColumnStruct.ZONE_NONE || ground != groundHeight)
            out.setGround(ground, columnZone, biome);
    }

    // Coarse \\

    // A widely spaced sample against the drainage itself: each river a straight run between its hearts, its level
    // eased from one heart's to the next, shown only where it is wide enough to matter at the spacing and then at
    // least half a sample wide, so it reads as a line however coarse the sample
    void resolveCoarse(
            WorldHandle worldHandle,
            double worldX,
            double worldZ,
            double spacingBlocks,
            int groundHeight,
            boolean oceanWater,
            boolean lakeWater,
            RiverColumnStruct out) {

        out.reset(groundHeight);

        if (oceanWater || lakeWater)
            return;

        RiverNetworkStruct network = riverNetworkBranch.getNetwork(worldHandle);
        int cellX = network.toCellX(worldX);
        int cellZ = network.toCellZ(worldZ);

        if (!network.isNear(network.toIndex(cellX, cellZ)))
            return;

        int radius = RiverNetworkStruct.NEAR_RADIUS_CELLS;
        double nearestEdge = Double.MAX_VALUE;
        int level = RiverColumnStruct.NONE;
        float depth = 0f;

        for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
            for (int offsetX = -radius; offsetX <= radius; offsetX++) {

                int index = network.toIndex(cellX + offsetX, cellZ + offsetZ);

                if (!network.isRiver(index))
                    continue;

                int strength = network.getStrength(index);
                float halfWidth = RiverPathUtility.resolveHalfWidth(strength);

                if (halfWidth * 2f * EngineSetting.RIVER_COARSE_WIDTH_GAIN < spacingBlocks)
                    continue;

                int target = network.getDownstreamIndex(index);
                double originX = network.getNodeX(index);
                double originZ = network.getNodeZ(index);
                double endX = WorldWrapUtility.wrappedBlockDeltaX(worldHandle, network.getNodeX(target), originX);
                double endZ = WorldWrapUtility.wrappedBlockDeltaZ(worldHandle, network.getNodeZ(target), originZ);
                double localX = WorldWrapUtility.wrappedBlockDeltaX(worldHandle, worldX, originX);
                double localZ = WorldWrapUtility.wrappedBlockDeltaZ(worldHandle, worldZ, originZ);
                double distance = RiverPathUtility.distanceToSegment(localX, localZ, 0.0, 0.0, endX, endZ);
                double edge = distance - Math.max(halfWidth, spacingBlocks * 0.5);

                if (edge >= 0.0 || edge >= nearestEdge)
                    continue;

                double lengthSq = endX * endX + endZ * endZ;
                double t = Math.max(0.0, Math.min(1.0, (localX * endX + localZ * endZ) / lengthSq));
                int startLevel = network.getLevelBlocks(index);
                int endLevel = network.isRiver(target) ? network.getLevelBlocks(target) : TideUtility.BAND_MAX_Y;

                nearestEdge = edge;
                level = (int) Math.round(startLevel + (endLevel - startLevel) * t);
                depth = RiverPathUtility.resolveDepth(strength);
            }
        }

        if (level == RiverColumnStruct.NONE)
            return;

        int ground = Math.min(groundHeight, level - Math.max(1, Math.round(depth)));

        out.setWaterLevel(level);
        out.setGround(ground, RiverColumnStruct.ZONE_CHANNEL, null);
    }

    // Shape \\

    // The flat bank beside the water, never narrower than the block that holds it in
    private int resolveBankBlocks(BiomeRiverStruct river) {
        return Math.max(EngineSetting.RIVER_MIN_BANK_BLOCKS, river.getFloodplainBlocks());
    }

    // How deep the bed lies below the water at a distance from the centreline: deepest at the middle, never less
    // than a block at the edge
    private int resolveDepthAt(double distance, float halfWidth, float depth) {

        double across = Math.min(1.0, distance / halfWidth);

        return Math.max(1, (int) Math.round(depth * (1.0 - across * across)));
    }

    // The highest the ground may stand at a distance from an open reach: a block over its water across the channel
    // and bank, then rising at the biome's wall slope, breaking into a rim near the edge of the widest cut, and
    // free beyond it
    private int resolveCarveTop(RiverQueryStruct query, double distance) {

        BiomeRiverStruct river = query.getBiome().getRiver();
        double wallStart = query.getHalfWidthBlocks() + resolveBankBlocks(river);
        double outward = distance - wallStart;
        double valley = EngineSetting.RIVER_MAX_VALLEY_BLOCKS;

        if (outward > valley)
            return Integer.MAX_VALUE;

        if (outward <= 0.0)
            return query.getLevelBlocks() + 1;

        double rimStart = valley - EngineSetting.RIVER_RIM_WIDTH_BLOCKS;
        double rise = outward * river.getWallSlope()
                + Math.max(0.0, outward - rimStart) * EngineSetting.RIVER_RIM_SLOPE;

        return query.getLevelBlocks() + 1 + (int) Math.floor(rise);
    }
}
