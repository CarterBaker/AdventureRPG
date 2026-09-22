package application.bootstrap.worldpipeline.structuremanager;

import java.util.Arrays;

import application.bootstrap.worldpipeline.structure.RoadData;
import application.bootstrap.worldpipeline.structure.RoadPathStruct;
import application.bootstrap.worldpipeline.structure.RoadSegmentType;
import application.bootstrap.worldpipeline.structure.StructureCacheStruct;
import application.bootstrap.worldpipeline.structure.StructureGeometryUtility;
import application.bootstrap.worldpipeline.structure.StructurePlacementStruct;
import application.bootstrap.worldpipeline.structure.StructurePlanStruct;
import application.bootstrap.worldpipeline.structure.StructureRegionStruct;
import application.bootstrap.worldpipeline.structure.StructureTemplateData;
import application.bootstrap.worldpipeline.structure.StructureTemplatePieceStruct;
import application.bootstrap.worldpipeline.structure.StructureType;
import application.bootstrap.worldpipeline.structure.StructureWriteAsyncContainer;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class StructureStampBranch extends BranchPackage {

    /*
     * Turns placements, layouts and roads into the block overrides for one
     * chunk column.
     *
     * The world is cut into square structure regions. The first chunk
     * generated inside a region resolves, once, every placement plan whose
     * bounding circle reaches the region and every world road whose band
     * crosses it; every other chunk in the region reuses that short list.
     * A world road is one edge of the placement link graph, planned once
     * from its lower-ID end so the path never depends on which region asked
     * for it first, and memoized by edge.
     *
     * Rasterizing a road assigns every block column in the chunk to its
     * nearest centreline point within reach, then builds that column once
     * from that point's height and mode — so a road climbing a slope never
     * has one point's clearance carve away the next point's surface.
     * Templates are copied cell by cell through their rotation, with a
     * foundation grown down to the ground under any solid bottom cell.
     */

    // Internal
    private StructureManager structureManager;
    private StructurePlacementBranch placementBranch;
    private StructureLayoutBranch layoutBranch;
    private RoadPlanBranch roadPlanBranch;

    private StructureCacheStruct<StructureRegionStruct> regionCache;
    private StructureCacheStruct<RoadPathStruct> roadCache;

    // Base \\

    @Override
    protected void create() {
        this.regionCache = new StructureCacheStruct<>(EngineSetting.STRUCTURE_REGION_CACHE_LIMIT);
        this.roadCache = new StructureCacheStruct<>(EngineSetting.STRUCTURE_PLAN_CACHE_LIMIT);
    }

    @Override
    protected void get() {
        this.structureManager = get(StructureManager.class);
        this.placementBranch = get(StructurePlacementBranch.class);
        this.layoutBranch = get(StructureLayoutBranch.class);
        this.roadPlanBranch = get(RoadPlanBranch.class);
    }

    // Chunk \\

    void stampChunk(
            WorldHandle worldHandle,
            long chunkCoordinate,
            int[] groundHeightBlocks,
            StructureWriteAsyncContainer writes) {

        if (placementBranch.bindWorld(worldHandle)) {
            regionCache.clear();
            roadCache.clear();
            layoutBranch.clear();
        }

        if (structureManager.getPlaceableHandles().length == 0)
            return;

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);
        long originX = (long) chunkX * EngineSetting.CHUNK_SIZE;
        long originZ = (long) chunkZ * EngineSetting.CHUNK_SIZE;

        int regionSize = EngineSetting.STRUCTURE_REGION_SIZE_BLOCKS;
        int regionX = (int) Math.floorDiv(originX, regionSize);
        int regionZ = (int) Math.floorDiv(originZ, regionSize);

        StructureRegionStruct region = regionCache.get(
                Coordinate2Long.pack(regionX, regionZ), key -> buildRegion(worldHandle, regionX, regionZ));

        if (region.isEmpty())
            return;

        ChunkContext context = new ChunkContext(worldHandle, chunkCoordinate, originX, originZ, groundHeightBlocks,
                writes);

        ObjectArrayList<RoadPathStruct> roads = region.getRoads();

        for (int i = 0; i < roads.size(); i++)
            stampPath(context, roads.get(i));

        ObjectArrayList<StructurePlanStruct> plans = region.getPlans();

        for (int i = 0; i < plans.size(); i++) {

            StructurePlanStruct plan = plans.get(i);

            for (int s = 0; s < plan.getStreets().size(); s++)
                stampPath(context, plan.getStreets().get(s));

            for (int p = 0; p < plan.getPieces().size(); p++)
                stampPiece(context, plan.getPieces().get(p));
        }
    }

    private static final class ChunkContext {

        final WorldHandle worldHandle;
        final long wrappedChunkCoordinate;
        final long originX;
        final long originZ;
        final int[] groundHeightBlocks;
        final StructureWriteAsyncContainer writes;
        final long worldWidth;
        final long worldHeight;

        ChunkContext(
                WorldHandle worldHandle, long wrappedChunkCoordinate,
                long originX, long originZ,
                int[] groundHeightBlocks,
                StructureWriteAsyncContainer writes) {

            this.worldHandle = worldHandle;
            this.wrappedChunkCoordinate = wrappedChunkCoordinate;
            this.originX = originX;
            this.originZ = originZ;
            this.groundHeightBlocks = groundHeightBlocks;
            this.writes = writes;
            this.worldWidth = worldHandle.getWorldScale().x;
            this.worldHeight = worldHandle.getWorldScale().y;
        }

        int ground(int localX, int localZ) {
            return groundHeightBlocks[localZ * EngineSetting.CHUNK_SIZE + localX];
        }
    }

    // Region \\

    private StructureRegionStruct buildRegion(WorldHandle worldHandle, int regionX, int regionZ) {

        int regionSize = EngineSetting.STRUCTURE_REGION_SIZE_BLOCKS;
        double centerX = regionX * (double) regionSize + regionSize * 0.5;
        double centerZ = regionZ * (double) regionSize + regionSize * 0.5;
        double halfDiagonal = regionSize * Math.sqrt(0.5);

        StructureRegionStruct region = new StructureRegionStruct();

        ObjectArrayList<StructurePlacementStruct> placements = new ObjectArrayList<>();
        placementBranch.collectPlacements(worldHandle, centerX, centerZ, halfDiagonal, placements);

        for (int i = 0; i < placements.size(); i++)
            region.addPlan(layoutBranch.getPlan(worldHandle, placements.get(i)));

        collectRoads(worldHandle, regionX, regionZ, centerX, centerZ, halfDiagonal, region);

        return region.isEmpty() ? StructureRegionStruct.EMPTY : region;
    }

    /*
     * Any link whose road can reach this region has both ends within link
     * distance plus corridor of it, so gathering the connectable placements
     * inside that radius and unioning their neighbour choices finds every
     * candidate edge; the cheap segment-distance test then skips planning
     * the ones that plainly run elsewhere.
     */
    private void collectRoads(
            WorldHandle worldHandle,
            int regionX, int regionZ,
            double centerX, double centerZ,
            double halfDiagonal,
            StructureRegionStruct region) {

        int maxLink = placementBranch.getMaxRoadLinkDistance();

        if (maxLink <= 0)
            return;

        double corridor = Math.max(EngineSetting.ROAD_PATH_MIN_CORRIDOR_BLOCKS,
                maxLink * EngineSetting.ROAD_PATH_CORRIDOR_FRACTION) + EngineSetting.ROAD_PATH_GRID_BLOCKS;

        ObjectArrayList<StructurePlacementStruct> nearby = new ObjectArrayList<>();
        placementBranch.collectPlacements(worldHandle, centerX, centerZ, halfDiagonal + maxLink + corridor, nearby);

        LongOpenHashSet seenEdges = new LongOpenHashSet();

        for (int i = 0; i < nearby.size(); i++) {

            StructurePlacementStruct from = nearby.get(i);

            if (!from.getStructureHandle().connectsToRoads())
                continue;

            ObjectArrayList<StructurePlacementStruct> neighbours = placementBranch.getRoadNeighbours(worldHandle, from);

            for (int n = 0; n < neighbours.size(); n++) {

                StructurePlacementStruct to = neighbours.get(n);

                StructurePlacementStruct low = from.getPlacementID() < to.getPlacementID() ? from : to;
                StructurePlacementStruct high = low == from ? to : from;
                long edgeKey = StructureGeometryUtility.mix(low.getPlacementID()) * 31L + high.getPlacementID();

                if (!seenEdges.add(edgeKey))
                    continue;

                double worldWidth = worldHandle.getWorldScale().x;
                double worldHeight = worldHandle.getWorldScale().y;
                double endX = low.getX() + StructureGeometryUtility.wrapDelta(high.getX() - low.getX(), worldWidth);
                double endZ = low.getZ() + StructureGeometryUtility.wrapDelta(high.getZ() - low.getZ(), worldHeight);
                double regionRelX = low.getX() + StructureGeometryUtility.wrapDelta(centerX - low.getX(), worldWidth);
                double regionRelZ = low.getZ() + StructureGeometryUtility.wrapDelta(centerZ - low.getZ(), worldHeight);

                double distance = StructureGeometryUtility.pointSegmentDistance(
                        regionRelX, regionRelZ, low.getX(), low.getZ(), endX, endZ);

                if (distance > halfDiagonal + corridor)
                    continue;

                RoadPathStruct road = roadCache.get(edgeKey, key -> planEdge(worldHandle, low, high));

                if (road != null && touchesRegion(worldHandle, road, regionX, regionZ))
                    region.addRoad(road);
            }
        }
    }

    private RoadPathStruct planEdge(
            WorldHandle worldHandle,
            StructurePlacementStruct low,
            StructurePlacementStruct high) {

        StructurePlacementStruct owner = low.outranks(high) ? low : high;
        RoadData road = structureManager.getStructureHandleOfType(
                owner.getStructureHandle().getRoadLinkData().getRoadName(),
                StructureType.ROAD, owner.getStructureHandle().getStructureName()).getRoadData();

        StructurePlanStruct lowPlan = layoutBranch.getPlan(worldHandle, low);
        StructurePlanStruct highPlan = layoutBranch.getPlan(worldHandle, high);

        double ax = low.getX() + lowPlan.getConnectOffsetX();
        double az = low.getZ() + lowPlan.getConnectOffsetZ();
        double bx = high.getX() + highPlan.getConnectOffsetX();
        double bz = high.getZ() + highPlan.getConnectOffsetZ();

        double worldWidth = worldHandle.getWorldScale().x;
        double worldHeight = worldHandle.getWorldScale().y;
        double dx = StructureGeometryUtility.wrapDelta(bx - ax, worldWidth);
        double dz = StructureGeometryUtility.wrapDelta(bz - az, worldHeight);
        double length = Math.sqrt(dx * dx + dz * dz);

        ObjectArrayList<StructurePlacementStruct> avoid = new ObjectArrayList<>();
        placementBranch.collectPlacements(worldHandle, ax + dx * 0.5, az + dz * 0.5,
                length * 0.5 + Math.max(EngineSetting.ROAD_PATH_MIN_CORRIDOR_BLOCKS,
                        length * EngineSetting.ROAD_PATH_CORRIDOR_FRACTION),
                avoid);

        for (int i = avoid.size() - 1; i >= 0; i--) {
            long id = avoid.get(i).getPlacementID();
            if (id == low.getPlacementID() || id == high.getPlacementID())
                avoid.remove(i);
        }

        return roadPlanBranch.planRoad(
                worldHandle, road,
                ax, az, lowPlan.getConnectY(),
                bx, bz, highPlan.getConnectY(),
                avoid);
    }

    private boolean touchesRegion(WorldHandle worldHandle, RoadPathStruct road, int regionX, int regionZ) {

        double regionSize = EngineSetting.STRUCTURE_REGION_SIZE_BLOCKS;
        double reach = road.getReachBlocks();
        double worldWidth = worldHandle.getWorldScale().x;
        double worldHeight = worldHandle.getWorldScale().y;

        for (int shiftX = -1; shiftX <= 1; shiftX++) {
            for (int shiftZ = -1; shiftZ <= 1; shiftZ++) {

                double minX = regionX * regionSize + shiftX * worldWidth;
                double minZ = regionZ * regionSize + shiftZ * worldHeight;

                if (road.getMaxX() + reach >= minX && road.getMinX() - reach <= minX + regionSize
                        && road.getMaxZ() + reach >= minZ && road.getMinZ() - reach <= minZ + regionSize)
                    return true;
            }
        }

        return false;
    }

    // Roads \\

    private void stampPath(ChunkContext context, RoadPathStruct path) {

        IntArrayList indices = path.getPointIndicesForChunk(context.wrappedChunkCoordinate);

        if (indices == null)
            return;

        int chunkSize = EngineSetting.CHUNK_SIZE;
        float reach = path.getReachBlocks();

        float[] nearestDistance = new float[chunkSize * chunkSize];
        int[] nearestIndex = new int[chunkSize * chunkSize];
        Arrays.fill(nearestDistance, Float.MAX_VALUE);
        Arrays.fill(nearestIndex, -1);

        for (int n = 0; n < indices.size(); n++) {

            int index = indices.getInt(n);

            double pointX = StructureGeometryUtility.wrapDelta(path.getX(index) - context.originX, context.worldWidth);
            double pointZ = StructureGeometryUtility.wrapDelta(path.getZ(index) - context.originZ, context.worldHeight);

            int lowX = Math.max(0, (int) Math.floor(pointX - reach));
            int highX = Math.min(chunkSize - 1, (int) Math.floor(pointX + reach));
            int lowZ = Math.max(0, (int) Math.floor(pointZ - reach));
            int highZ = Math.min(chunkSize - 1, (int) Math.floor(pointZ + reach));

            for (int localX = lowX; localX <= highX; localX++) {
                for (int localZ = lowZ; localZ <= highZ; localZ++) {

                    float distance = (float) Math.hypot(localX + 0.5 - pointX, localZ + 0.5 - pointZ);
                    int column = localZ * chunkSize + localX;

                    if (distance <= reach && distance < nearestDistance[column]) {
                        nearestDistance[column] = distance;
                        nearestIndex[column] = index;
                    }
                }
            }
        }

        for (int localX = 0; localX < chunkSize; localX++) {
            for (int localZ = 0; localZ < chunkSize; localZ++) {

                int column = localZ * chunkSize + localX;

                if (nearestIndex[column] >= 0)
                    stampRoadColumn(context, path, nearestIndex[column], nearestDistance[column], localX, localZ);
            }
        }
    }

    private void stampRoadColumn(
            ChunkContext context,
            RoadPathStruct path,
            int index,
            float distance,
            int localX, int localZ) {

        RoadData road = path.getRoadData();
        StructureWriteAsyncContainer writes = context.writes;
        short air = structureManager.getAirBlockID();

        float halfWidth = road.getHalfWidthBlocks();
        boolean inside = distance <= halfWidth;
        boolean shoulder = !inside && distance <= halfWidth + 1f;

        if (!inside && !shoulder)
            return;

        int y = path.getY(index);
        int ground = context.ground(localX, localZ);

        switch (path.getSegmentType(index)) {

            case GROUND: {

                int cutLimit = y + road.getTunnelThresholdBlocks() + road.getClearanceBlocks();

                if (inside) {
                    writes.write(localX, y, localZ, surfaceBlock(context, road, distance, localX, localZ));
                    fill(writes, localX, localZ, y + 1, Math.min(cutLimit, Math.max(y + road.getClearanceBlocks(), ground)),
                            air);
                    fill(writes, localX, localZ, Math.max(ground + 1, y - road.getMaxFillBlocks() - 2), y - 1,
                            road.getFoundationBlockID());
                } else if (ground > y) {
                    fill(writes, localX, localZ, y + 1, Math.min(ground, cutLimit), air);
                } else if (ground < y - 1) {
                    fill(writes, localX, localZ, Math.max(ground + 1, y - road.getMaxFillBlocks()), y - 1,
                            road.getFoundationBlockID());
                }
                break;
            }

            case TUNNEL: {

                int ceiling = y + road.getTunnelHeightBlocks() + 1;

                if (inside) {
                    writes.write(localX, y, localZ, surfaceBlock(context, road, distance, localX, localZ));
                    fill(writes, localX, localZ, y + 1, ceiling - 1, air);
                    writes.write(localX, ceiling, localZ, road.getTunnelCeilingBlockID());
                } else {
                    fill(writes, localX, localZ, y, ceiling, road.getTunnelWallBlockID());
                }

                if (ground < y - 1)
                    fill(writes, localX, localZ,
                            Math.max(ground + 1, y - EngineSetting.STRUCTURE_MAX_FOUNDATION_DEPTH_BLOCKS), y - 1,
                            road.getFoundationBlockID());
                break;
            }

            case BRIDGE: {

                writes.write(localX, y, localZ, road.getBridgeDeckBlockID());

                if (inside) {

                    fill(writes, localX, localZ, y + 1, y + road.getClearanceBlocks(), air);

                    if (index % road.getSupportSpacingBlocks() == 0 && distance <= 1f)
                        fill(writes, localX, localZ, Math.max(ground + 1, EngineSetting.STRUCTURE_WORLD_MIN_Y_BLOCKS),
                                y - 1, road.getBridgeSupportBlockID());
                } else if (road.hasBridgeRail()) {
                    writes.write(localX, y + 1, localZ, road.getBridgeRailBlockID());
                }
                break;
            }
        }
    }

    private short surfaceBlock(ChunkContext context, RoadData road, float distance, int localX, int localZ) {

        if (road.hasEdgeBlock() && distance > road.getHalfWidthBlocks() - 1f)
            return road.getEdgeBlockID();

        float roll = StructureGeometryUtility.hash01(StructureGeometryUtility.hash(
                context.worldHandle.getSeed() ^ EngineSetting.ROAD_SURFACE_SEED,
                context.originX + localX, context.originZ + localZ, 0));

        return road.pickSurfaceBlockID(roll);
    }

    // Templates \\

    private void stampPiece(ChunkContext context, StructureTemplatePieceStruct piece) {

        int chunkSize = EngineSetting.CHUNK_SIZE;
        StructureTemplateData template = piece.getTemplateData();
        int rotation = piece.getRotation();
        int footprintX = piece.getFootprintX();
        int footprintZ = piece.getFootprintZ();

        long offsetX = StructureGeometryUtility.wrapDelta(piece.getOriginX() - context.originX, context.worldWidth);
        long offsetZ = StructureGeometryUtility.wrapDelta(piece.getOriginZ() - context.originZ, context.worldHeight);

        if (offsetX >= chunkSize || offsetZ >= chunkSize || offsetX + footprintX <= 0 || offsetZ + footprintZ <= 0)
            return;

        int lowX = (int) Math.max(0, offsetX);
        int highX = (int) Math.min(chunkSize - 1, offsetX + footprintX - 1);
        int lowZ = (int) Math.max(0, offsetZ);
        int highZ = (int) Math.min(chunkSize - 1, offsetZ + footprintZ - 1);

        short air = structureManager.getAirBlockID();
        int originY = piece.getOriginY();
        StructureWriteAsyncContainer writes = context.writes;

        for (int localX = lowX; localX <= highX; localX++) {
            for (int localZ = lowZ; localZ <= highZ; localZ++) {

                int footX = (int) (localX - offsetX);
                int footZ = (int) (localZ - offsetZ);
                int templateX = StructureTemplatePieceStruct.templateX(template, rotation, footX, footZ);
                int templateZ = StructureTemplatePieceStruct.templateZ(template, rotation, footX, footZ);

                for (int templateY = 0; templateY < template.getSizeY(); templateY++) {

                    short blockID = template.getBlock(templateX, templateY, templateZ);

                    if (blockID != StructureTemplateData.BLOCK_SKIP)
                        writes.write(localX, originY + templateY, localZ, blockID);
                }

                if (!template.hasFoundation())
                    continue;

                short bottom = template.getBlock(templateX, 0, templateZ);

                if (bottom == StructureTemplateData.BLOCK_SKIP || bottom == air)
                    continue;

                fill(writes, localX, localZ,
                        Math.max(context.ground(localX, localZ) + 1,
                                originY - EngineSetting.STRUCTURE_MAX_FOUNDATION_DEPTH_BLOCKS),
                        originY - 1, template.getFoundationBlockID());
            }
        }
    }

    // Util \\

    private static void fill(StructureWriteAsyncContainer writes, int localX, int localZ, int fromY, int toY,
            short blockID) {
        for (int y = fromY; y <= toY; y++)
            writes.write(localX, y, localZ, blockID);
    }
}
