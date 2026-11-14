package application.bootstrap.worldpipeline.layoutmanager;

import application.bootstrap.worldpipeline.architecture.ArchitectureWallStruct;
import application.bootstrap.worldpipeline.layout.LayoutLotStruct;
import application.bootstrap.worldpipeline.layout.LayoutPlanStruct;
import application.bootstrap.worldpipeline.layout.LayoutWallStruct;
import application.bootstrap.worldpipeline.road.RoadPathStruct;
import application.bootstrap.worldpipeline.road.RoadQueryStruct;
import application.bootstrap.worldpipeline.roadmanager.RoadManager;
import application.bootstrap.worldpipeline.structuremanager.StructureManager;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.ChunkWriteUtility;
import application.bootstrap.worldpipeline.util.RoadPathUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;

class LayoutStampBranch extends BranchPackage {

    /*
     * Async — lays a planned layout's share of one chunk: its roads first,
     * so its walls and structures stand over them, then its walls, then
     * every lot whose box reaches the chunk. A wall rises from the ground to
     * its height over its rolling foot, left open at its gates, its top
     * capped and both its faces crenellated every other few blocks along it.
     */

    // Internal
    private RoadManager roadManager;
    private StructureManager structureManager;
    private WorldGenerationManager worldGenerationManager;
    private LayoutQueryAsyncContainer queryContainer;

    // Settings
    private int chunkSize;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.queryContainer = create(LayoutQueryAsyncContainer.class);

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
    }

    @Override
    protected void get() {
        this.roadManager = get(RoadManager.class);
        this.structureManager = get(StructureManager.class);
        this.worldGenerationManager = get(WorldGenerationManager.class);
    }

    // Stamp \\

    void stamp(WorldHandle worldHandle, long chunkCoordinate, SubChunkInstance[] subChunks, LayoutPlanStruct plan) {

        double chunkMinX = WorldWrapUtility.unwrapBlockX(
                worldHandle, (double) Coordinate2Long.unpackX(chunkCoordinate) * chunkSize, plan.getReferenceX());
        double chunkMinZ = WorldWrapUtility.unwrapBlockZ(
                worldHandle, (double) Coordinate2Long.unpackY(chunkCoordinate) * chunkSize, plan.getReferenceZ());
        double chunkMaxX = chunkMinX + chunkSize;
        double chunkMaxZ = chunkMinZ + chunkSize;

        if (!plan.overlaps(chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ))
            return;

        RoadPathStruct[] paths = plan.getPaths();

        for (int i = 0; i < paths.length; i++)
            roadManager.stampPath(worldHandle, chunkCoordinate, subChunks, paths[i]);

        LayoutWallStruct[] walls = plan.getWalls();

        for (int i = 0; i < walls.length; i++)
            stampWall(chunkCoordinate, subChunks, walls[i], chunkMinX, chunkMinZ);

        LayoutLotStruct[] lots = plan.getLots();

        for (int i = 0; i < lots.length; i++) {

            LayoutLotStruct lot = lots[i];

            if (lot.getMaxX() < chunkMinX || lot.getMinX() > chunkMaxX
                    || lot.getMaxZ() < chunkMinZ || lot.getMinZ() > chunkMaxZ)
                continue;

            structureManager.stampStructure(
                    worldHandle, chunkCoordinate, subChunks, lot.getStructureHandle(),
                    lot.getAnchorX(), lot.getAnchorZ(), lot.getAnchorY(), lot.getQuarterTurns());
        }
    }

    // Wall \\

    private void stampWall(
            long chunkCoordinate,
            SubChunkInstance[] subChunks,
            LayoutWallStruct layoutWall,
            double chunkMinX,
            double chunkMinZ) {

        RoadPathStruct centerline = layoutWall.getCenterline();
        ArchitectureWallStruct wall = layoutWall.getWall();
        LayoutQueryAsyncContainer scratch = queryContainer.getInstance();
        RoadQueryStruct query = scratch.query;
        double halfThickness = wall.getHalfThicknessBlocks();

        for (int localZ = 0; localZ < chunkSize; localZ++) {
            for (int localX = 0; localX < chunkSize; localX++) {

                double x = chunkMinX + localX + EngineSetting.BLOCK_CENTER_OFFSET;
                double z = chunkMinZ + localZ + EngineSetting.BLOCK_CENTER_OFFSET;

                if (!RoadPathUtility.reaches(centerline, x, z, halfThickness))
                    continue;

                double distance = RoadPathUtility.locate(centerline, x, z, query);

                if (distance > halfThickness || layoutWall.isGate(query.getAlong()))
                    continue;

                int groundHeight = worldGenerationManager.getColumnGroundHeight(chunkCoordinate, localX, localZ);
                int topY = Math.max((int) Math.floor(query.getSurfaceY()), groundHeight) + wall.getHeightBlocks();

                for (int worldY = groundHeight + 1; worldY < topY; worldY++)
                    ChunkWriteUtility.writeSolid(subChunks, localX, worldY, localZ, wall.getWallBlockID());

                ChunkWriteUtility.writeSolid(subChunks, localX, topY, localZ, wall.getCapBlockID());

                boolean face = distance > halfThickness - EngineSetting.LAYOUT_WALL_CRENEL_DEPTH_BLOCKS;
                boolean merlon = ((int) Math.floor(query.getAlong() / EngineSetting.LAYOUT_WALL_CRENEL_SPACING_BLOCKS)
                        & 1) == 0;

                if (face && merlon)
                    ChunkWriteUtility.writeSolid(subChunks, localX, topY + 1, localZ, wall.getCapBlockID());
            }
        }
    }
}
