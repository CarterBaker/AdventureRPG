package application.bootstrap.worldpipeline.rivermanager;

import application.bootstrap.mappipeline.mapmanager.MapManager;
import application.bootstrap.worldpipeline.river.RiverChangeStruct;
import application.bootstrap.worldpipeline.river.RiverColumnStruct;
import application.bootstrap.worldpipeline.river.RiverGatherStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;

public class RiverManager extends ManagerPackage {

    /*
     * Owns the world's rivers and is world generation's single entry point
     * for them. Each world drains once over a grid of map cells, and every
     * reach of river between two cells is planned against the land the first
     * time anything asks for it and cached, so a chunk, a probe, distant
     * terrain and the map all carve the same river. A chunk gathers the
     * reaches near it once and measures every column against them; a single
     * point gathers its own. Rivers are shaped entirely by the biomes they
     * cross and the world's height layer. An edit to the world's pixels or to
     * any biome drains the world again in the background, and once it lands
     * every chunk and map tile the changed rivers reach is streamed again.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private MapManager mapManager;
    private RiverNetworkBranch riverNetworkBranch;
    private RiverColumnBranch riverColumnBranch;

    // Base \\

    @Override
    protected void create() {
        this.riverNetworkBranch = create(RiverNetworkBranch.class);
        create(RiverPlanBranch.class);
        create(RiverCacheBranch.class);
        this.riverColumnBranch = create(RiverColumnBranch.class);
    }

    @Override
    protected void get() {
        this.worldStreamManager = get(WorldStreamManager.class);
        this.mapManager = get(MapManager.class);
    }

    @Override
    protected void update() {

        riverNetworkBranch.refreshStale();

        RiverChangeStruct change = riverNetworkBranch.pollChange();

        while (change != null) {
            restreamChange(change);
            change = riverNetworkBranch.pollChange();
        }
    }

    // Management \\

    // The world's pixels changed under it; it drains again in the background while its rivers keep standing
    public void invalidateNetwork(WorldHandle worldHandle) {
        riverNetworkBranch.requestRebuild(worldHandle);
    }

    private void restreamChange(RiverChangeStruct change) {

        int cellPixels = EngineSetting.RIVER_CELL_PIXELS;
        int minPixelX = change.getMinCellX() * cellPixels;
        int minPixelZ = change.getMinCellZ() * cellPixels;
        int maxPixelX = (change.getMaxCellX() + 1) * cellPixels - 1;
        int maxPixelZ = (change.getMaxCellZ() + 1) * cellPixels - 1;

        mapManager.invalidateWorldPixels(change.getWorldHandle(), minPixelX, minPixelZ, maxPixelX, maxPixelZ);
        worldStreamManager.requestLiveRebuild(change.getWorldHandle(), minPixelX, minPixelZ, maxPixelX, maxPixelZ);
    }

    // Column \\

    // Worker — every reach that can touch a chunk, gathered once for all its columns
    public void gatherChunk(WorldHandle worldHandle, long chunkCoordinate, RiverGatherStruct out) {

        double fromX = (double) Coordinate2Long.unpackX(chunkCoordinate) * EngineSetting.CHUNK_SIZE;
        double fromZ = (double) Coordinate2Long.unpackY(chunkCoordinate) * EngineSetting.CHUNK_SIZE;

        riverColumnBranch.gather(worldHandle, fromX, fromZ, EngineSetting.CHUNK_SIZE - 1, out);
    }

    // Worker — one block column of a chunk measured against the reaches gathered for it
    public void resolveColumn(
            RiverGatherStruct gather,
            double worldX,
            double worldZ,
            int groundHeight,
            boolean oceanWater,
            boolean lakeWater,
            int lakeRimLevel,
            RiverColumnStruct out) {
        riverColumnBranch.resolveColumn(
                gather, worldX, worldZ, groundHeight, oceanWater, lakeWater, lakeRimLevel, out);
    }

    // Worker — any single column, its reaches gathered on the spot
    public void resolvePoint(
            WorldHandle worldHandle,
            double worldX,
            double worldZ,
            int groundHeight,
            boolean oceanWater,
            boolean lakeWater,
            int lakeRimLevel,
            RiverColumnStruct out) {
        riverColumnBranch.resolveColumn(
                riverColumnBranch.gatherPoint(worldHandle, worldX, worldZ),
                worldX, worldZ, groundHeight, oceanWater, lakeWater, lakeRimLevel, out);
    }

    // Worker — a sample standing for a span of the world: planned reaches where it is fine enough to show a
    // river's banks, the drainage itself where it is coarser
    public void resolveSample(
            WorldHandle worldHandle,
            double worldX,
            double worldZ,
            double spacingBlocks,
            int groundHeight,
            boolean oceanWater,
            boolean lakeWater,
            int lakeRimLevel,
            RiverColumnStruct out) {

        if (spacingBlocks > EngineSetting.RIVER_PLANNED_SPACING_BLOCKS)
            riverColumnBranch.resolveCoarse(
                    worldHandle, worldX, worldZ, spacingBlocks, groundHeight, oceanWater, lakeWater, out);
        else
            resolvePoint(worldHandle, worldX, worldZ, groundHeight, oceanWater, lakeWater, lakeRimLevel, out);
    }
}
