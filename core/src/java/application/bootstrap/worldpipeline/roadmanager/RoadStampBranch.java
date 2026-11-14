package application.bootstrap.worldpipeline.roadmanager;

import application.bootstrap.worldpipeline.road.RoadBridgeStruct;
import application.bootstrap.worldpipeline.road.RoadHandle;
import application.bootstrap.worldpipeline.road.RoadPathStruct;
import application.bootstrap.worldpipeline.road.RoadQueryStruct;
import application.bootstrap.worldpipeline.road.RoadTunnelStruct;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import application.bootstrap.worldpipeline.util.ChunkWriteUtility;
import application.bootstrap.worldpipeline.util.RoadPathUtility;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.WeightedTableUtility;

class RoadStampBranch extends BranchPackage {

    /*
     * Async — lays one planned road into one chunk, column by column, by how
     * far each column stands from the centreline, so every bend and junction
     * comes out round. On the ground the surface is laid at the road's
     * height, a half step above it where a smoothed slope climbs, the land is
     * filled up under it and cut away over it, and a shoulder edges it into
     * the land beside. A bridge lays its deck with a rail along each edge and
     * stands pillars down to the ground every so many blocks along it. A
     * tunnel bores an arched passage, lowest at its walls, and lines its
     * floor, walls and ceiling. Surface blocks are drawn per column from the
     * road's weighted mix, salted by the world and the road.
     */

    // Internal
    private WorldGenerationManager worldGenerationManager;
    private RoadStampAsyncContainer stampContainer;

    // Settings
    private int chunkSize;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.stampContainer = create(RoadStampAsyncContainer.class);

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
    }

    @Override
    protected void get() {
        this.worldGenerationManager = get(WorldGenerationManager.class);
    }

    // Stamp \\

    void stamp(WorldHandle worldHandle, long chunkCoordinate, SubChunkInstance[] subChunks, RoadPathStruct path) {

        RoadHandle roadHandle = path.getRoadHandle();
        RoadStampAsyncContainer scratch = stampContainer.getInstance();
        RoadQueryStruct query = scratch.query;

        long chunkOriginX = (long) Coordinate2Long.unpackX(chunkCoordinate) * chunkSize;
        long chunkOriginZ = (long) Coordinate2Long.unpackY(chunkCoordinate) * chunkSize;
        double planOriginX = WorldWrapUtility.unwrapBlockX(worldHandle, (double) chunkOriginX, path.getPointX(0));
        double planOriginZ = WorldWrapUtility.unwrapBlockZ(worldHandle, (double) chunkOriginZ, path.getPointZ(0));

        if (planOriginX + chunkSize < path.getMinX() || planOriginX > path.getMaxX()
                || planOriginZ + chunkSize < path.getMinZ() || planOriginZ > path.getMaxZ())
            return;

        double halfWidth = roadHandle.getHalfWidthBlocks();
        double reach = halfWidth + Math.max(roadHandle.getShoulderWidthBlocks(), 1f);
        long surfaceSeed = worldHandle.getSeed() ^ EngineSetting.ROAD_SURFACE_SALT
                ^ (roadHandle.getNameSeed() * EngineSetting.STRUCTURE_NAME_SEED_MULTIPLIER);

        for (int localZ = 0; localZ < chunkSize; localZ++) {
            for (int localX = 0; localX < chunkSize; localX++) {

                double x = planOriginX + localX + EngineSetting.BLOCK_CENTER_OFFSET;
                double z = planOriginZ + localZ + EngineSetting.BLOCK_CENTER_OFFSET;

                if (!RoadPathUtility.reaches(path, x, z, 0.0))
                    continue;

                double distance = RoadPathUtility.locate(path, x, z, query);

                if (distance > reach)
                    continue;

                int groundHeight = worldGenerationManager.getColumnGroundHeight(chunkCoordinate, localX, localZ);
                short surfaceBlockID = roadHandle.getSurfaceBlockIDs()[WeightedTableUtility.pick(
                        roadHandle.getSurfaceCumulativeWeights(),
                        BiomeFieldUtility.hash01(BiomeFieldUtility.hashCell(
                                surfaceSeed, (int) (chunkOriginX + localX), (int) (chunkOriginZ + localZ))))];

                switch (query.getSpan()) {
                    case GROUND -> layGround(
                            subChunks, roadHandle, query, distance, halfWidth, localX, localZ, groundHeight,
                            surfaceBlockID);
                    case BRIDGE -> layBridge(
                            subChunks, roadHandle, query, distance, halfWidth, localX, localZ, groundHeight);
                    case TUNNEL -> layTunnel(
                            subChunks, roadHandle, query, distance, halfWidth, localX, localZ, surfaceBlockID);
                }
            }
        }
    }

    // Ground \\

    private void layGround(
            SubChunkInstance[] subChunks,
            RoadHandle roadHandle,
            RoadQueryStruct query,
            double distance,
            double halfWidth,
            int localX,
            int localZ,
            int groundHeight,
            short surfaceBlockID) {

        float surfaceY = query.getSurfaceY();
        int surfaceTop = (int) Math.floor(surfaceY);

        if (distance > halfWidth) {
            layShoulder(subChunks, roadHandle, distance, halfWidth, localX, localZ, groundHeight, surfaceTop);
            return;
        }

        boolean halfStep = roadHandle.hasSmoothSlopes()
                && surfaceY - surfaceTop >= EngineSetting.ROAD_HALF_STEP_FRACTION;
        int clearFrom = surfaceTop + (halfStep ? 2 : 1);

        for (int worldY = groundHeight + 1; worldY < surfaceTop; worldY++)
            ChunkWriteUtility.writeSolid(subChunks, localX, worldY, localZ, roadHandle.getBaseBlockID());

        ChunkWriteUtility.writeSolid(subChunks, localX, surfaceTop, localZ, surfaceBlockID);

        if (halfStep)
            ChunkWriteUtility.writeBlock(
                    subChunks, localX, surfaceTop + 1, localZ, surfaceBlockID,
                    EngineSetting.DEFAULT_BLOCK_ORIENTATION, SubBlockUtility.layerMask(0), false);

        ChunkWriteUtility.clearColumn(
                subChunks, localX, clearFrom, Math.max(groundHeight, surfaceTop + roadHandle.getClearanceBlocks() - 1),
                localZ);
    }

    // The shoulder edges the road into the land, laid only where the land meets it within a step
    private void layShoulder(
            SubChunkInstance[] subChunks,
            RoadHandle roadHandle,
            double distance,
            double halfWidth,
            int localX,
            int localZ,
            int groundHeight,
            int surfaceTop) {

        if (distance > halfWidth + roadHandle.getShoulderWidthBlocks())
            return;

        if (Math.abs(groundHeight - surfaceTop) > EngineSetting.ROAD_SHOULDER_MAX_STEP_BLOCKS)
            return;

        ChunkWriteUtility.writeSolid(subChunks, localX, surfaceTop, localZ, roadHandle.getShoulderBlockID());
        ChunkWriteUtility.clearColumn(subChunks, localX, surfaceTop + 1, groundHeight, localZ);
    }

    // Bridge \\

    private void layBridge(
            SubChunkInstance[] subChunks,
            RoadHandle roadHandle,
            RoadQueryStruct query,
            double distance,
            double halfWidth,
            int localX,
            int localZ,
            int groundHeight) {

        if (distance > halfWidth)
            return;

        RoadBridgeStruct bridge = roadHandle.getBridge();
        int deckY = (int) Math.floor(query.getSurfaceY());

        ChunkWriteUtility.writeSolid(subChunks, localX, deckY, localZ, bridge.getDeckBlockID());
        ChunkWriteUtility.clearColumn(
                subChunks, localX, deckY + 1, deckY + roadHandle.getClearanceBlocks() - 1, localZ);

        if (distance > halfWidth - EngineSetting.ROAD_RAIL_WIDTH_BLOCKS)
            ChunkWriteUtility.writeSolid(subChunks, localX, deckY + 1, localZ, bridge.getRailBlockID());

        boolean pillarRow = query.getAlong() % bridge.getPillarSpacingBlocks() < EngineSetting.ROAD_PILLAR_DEPTH_BLOCKS;

        if (!pillarRow || distance > halfWidth - EngineSetting.ROAD_RAIL_WIDTH_BLOCKS)
            return;

        for (int worldY = deckY - 1; worldY > groundHeight; worldY--)
            ChunkWriteUtility.writeSolid(subChunks, localX, worldY, localZ, bridge.getPillarBlockID());
    }

    // Tunnel \\

    private void layTunnel(
            SubChunkInstance[] subChunks,
            RoadHandle roadHandle,
            RoadQueryStruct query,
            double distance,
            double halfWidth,
            int localX,
            int localZ,
            short surfaceBlockID) {

        RoadTunnelStruct tunnel = roadHandle.getTunnel();
        int floorY = (int) Math.floor(query.getSurfaceY());
        short liningBlockID = tunnel.getLiningBlockID();

        if (distance > halfWidth) {

            for (int worldY = floorY; worldY <= floorY + tunnel.getHeightBlocks(); worldY++)
                ChunkWriteUtility.writeSolid(subChunks, localX, worldY, localZ, liningBlockID);

            return;
        }

        double edge = distance / Math.max(halfWidth, EngineSetting.DIVISION_EPSILON);
        int ceilingY = floorY + tunnel.getHeightBlocks()
                - (int) Math.round(edge * edge * EngineSetting.ROAD_TUNNEL_ARCH_DROP_BLOCKS);

        ChunkWriteUtility.writeSolid(subChunks, localX, floorY, localZ, surfaceBlockID);
        ChunkWriteUtility.clearColumn(subChunks, localX, floorY + 1, ceilingY - 1, localZ);
        ChunkWriteUtility.writeSolid(subChunks, localX, ceilingY, localZ, liningBlockID);
    }
}
