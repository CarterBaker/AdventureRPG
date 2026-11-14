package application.bootstrap.worldpipeline.macrostreammanager;

import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeSiteStruct;
import application.bootstrap.worldpipeline.tree.TreeStandIn;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import application.bootstrap.worldpipeline.util.TreeImpostorUtility;
import application.bootstrap.worldpipeline.util.TreeSkeletonUtility;
import application.bootstrap.worldpipeline.util.TreeStandInUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldgenerationmanager.TerrainSurfaceSampleStruct;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import engine.graphics.color.PackedColorUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;

public class MacroTreeBranch extends BranchPackage {

    /*
     * Async — the trees the macro terrain nearer than its canopy stands in one
     * by one, on a tile sampled finely enough to show them. Every wild tree
     * rooted in the tile is found exactly where chunk placement finds it,
     * through TreeManager.collectSites(), and stood on the tile's own surface
     * under the very crown a far mega outlines — the same lumps, sized from
     * its mature height and age — in the flat colors its bark and leaves show
     * from afar. resolveStandIn() is the one place a tile's resolution picks
     * how finely: the finest tiles facet each lump round over a tapered trunk,
     * coarser ones keep each lump a plain diamond, and the coarsest draw the
     * whole crown as one spire reaching down to the root. A root under water
     * is left out: a cell wholly dry or wholly wet answers from its corners,
     * and a shore cell asks the terrain at the root itself. Every shape
     * carries the chunk its tree roots in, so a whole tree yields at once the
     * moment the chunk grid takes that chunk over, never split across the
     * seam. The shapes are laid into the tile's mesh by MacroMeshBranch.
     */

    // Internal
    private TreeManager treeManager;
    private WorldGenerationManager worldGenerationManager;

    // Settings
    private int chunkSize;
    private int macroChunkSize;
    private int tileSizeBlocks;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.macroChunkSize = EngineSetting.MACRO_CHUNK_SIZE;
        this.tileSizeBlocks = EngineSetting.MACRO_TILE_SIZE_BLOCKS;
    }

    @Override
    protected void get() {
        this.treeManager = get(TreeManager.class);
        this.worldGenerationManager = get(WorldGenerationManager.class);
    }

    // Sample \\

    // True when a tile sampled at a resolution draws its trees one by one rather than as a canopy
    boolean drawsStandIns(int cellsPerSide) {
        return cellsPerSide >= EngineSetting.TREE_STAND_IN_MIN_CELLS_PER_SIDE;
    }

    // How finely a tile sampled at a resolution that draws its trees one by one shapes them
    TreeStandIn resolveStandIn(int cellsPerSide) {

        if (cellsPerSide >= EngineSetting.TREE_STAND_IN_FACETED_CELLS_PER_SIDE)
            return TreeStandIn.FACETED;

        if (cellsPerSide >= EngineSetting.TREE_STAND_IN_PLAIN_CELLS_PER_SIDE)
            return TreeStandIn.PLAIN;

        return TreeStandIn.SPIRE;
    }

    // Worker — every tree rooted in the tile stood in, read from the lattice the tile was just sampled at
    void sampleStandIns(MacroBuildAsyncContainer scratch, WorldHandle worldHandle, long coordinate) {

        scratch.standInCrowns.clear();
        scratch.standInTrunks.clear();

        if (!scratch.standIns)
            return;

        TreeStandIn tier = resolveStandIn(scratch.cellsPerSide);

        long originX = (long) Coordinate2Long.unpackX(coordinate) * chunkSize;
        long originZ = (long) Coordinate2Long.unpackY(coordinate) * chunkSize;

        treeManager.collectSites(
                worldHandle,
                originX,
                originZ,
                originX + tileSizeBlocks - 1,
                originZ + tileSizeBlocks - 1,
                scratch.sites);

        for (int i = 0; i < scratch.sites.size(); i++)
            standIn(scratch, worldHandle, tier, scratch.sites.get(i), originX, originZ);

        scratch.sites.clear();
    }

    private void standIn(
            MacroBuildAsyncContainer scratch,
            WorldHandle worldHandle,
            TreeStandIn tier,
            TreeSiteStruct site,
            long originX,
            long originZ) {

        int blockX = (int) WorldWrapUtility.wrappedBlockDeltaX(worldHandle, site.getAnchorX(), originX);
        int blockZ = (int) WorldWrapUtility.wrappedBlockDeltaZ(worldHandle, site.getAnchorZ(), originZ);
        float rootX = blockX + EngineSetting.TREE_ROOT_CENTER_BLOCKS;
        float rootZ = blockZ + EngineSetting.TREE_ROOT_CENTER_BLOCKS;

        if (isFlooded(scratch, worldHandle, site, rootX, rootZ))
            return;

        TreeHandle treeHandle = site.getTreeHandle();
        float rootY = resolveSurface(scratch, rootX, rootZ);
        float cover = blockZ / chunkSize * macroChunkSize + blockX / chunkSize;
        float turn = TreeStandInUtility.resolveTurn(site.getSeed());
        float[] crown = scratch.crown;
        int leafAlbedo = treeHandle.getPartAlbedos()[EngineSetting.TREE_PART_LEAF];
        int barkAlbedo = treeHandle.getPartAlbedos()[EngineSetting.TREE_PART_BARK];

        TreeImpostorUtility.estimateCrown(
                treeHandle,
                TreeSkeletonUtility.resolveMatureHeight(treeHandle, site.getSeed()),
                site.getAge(),
                crown);

        if (crown[EngineSetting.TREE_IMPOSTOR_CROWN_RADIUS] > 0f)
            TreeStandInUtility.emitCrown(
                    scratch.standInCrowns, tier, treeHandle.getForm(), crown, scratch.lumps,
                    rootX, rootY, rootZ, turn,
                    leafAlbedo, PackedColorUtility.scale(leafAlbedo, EngineSetting.TREE_CANOPY_SIDE_SHADE), cover);

        TreeStandInUtility.emitTrunk(
                scratch.standInTrunks, tier, rootX, rootY, rootZ,
                crown[EngineSetting.TREE_IMPOSTOR_TRUNK_HALF_WIDTH], crown[EngineSetting.TREE_IMPOSTOR_TRUNK_TOP], turn,
                barkAlbedo, PackedColorUtility.scale(barkAlbedo, EngineSetting.TREE_CANOPY_SIDE_SHADE), cover);
    }

    // Water \\

    // True where water stands over the root — read from the corners of its lattice cell unless they disagree
    private boolean isFlooded(
            MacroBuildAsyncContainer scratch,
            WorldHandle worldHandle,
            TreeSiteStruct site,
            float rootX,
            float rootZ) {

        int corner = resolveCellCorner(scratch, rootX, rootZ);
        int samplesPerSide = scratch.getSamplesPerSide();
        int wetCorners = countWet(scratch, corner) + countWet(scratch, corner + 1)
                + countWet(scratch, corner + samplesPerSide) + countWet(scratch, corner + samplesPerSide + 1);

        if (wetCorners == 0)
            return false;

        if (wetCorners == EngineSetting.QUAD_VERTEX_COUNT)
            return true;

        TerrainSurfaceSampleStruct sample = scratch.sample;

        worldGenerationManager.sampleSurface(worldHandle, site.getAnchorX(), site.getAnchorZ(), sample);

        return sample.isOpenWater() || sample.isLakeWater();
    }

    private int countWet(MacroBuildAsyncContainer scratch, int sample) {
        return scratch.openWater[sample] || scratch.stillWater[sample] ? 1 : 0;
    }

    // Surface \\

    // The height of the tile's own surface over a point, across the triangle of its cell the mesh draws there
    private float resolveSurface(MacroBuildAsyncContainer scratch, float x, float z) {

        float cellSizeBlocks = (float) tileSizeBlocks / scratch.cellsPerSide;
        int corner = resolveCellCorner(scratch, x, z);
        int samplesPerSide = scratch.getSamplesPerSide();
        float shareX = Math.min(x / cellSizeBlocks - corner % samplesPerSide, 1f);
        float shareZ = Math.min(z / cellSizeBlocks - corner / samplesPerSide, 1f);
        float low = scratch.heightBlocks[corner];
        float lowNext = scratch.heightBlocks[corner + 1];
        float high = scratch.heightBlocks[corner + samplesPerSide];
        float highNext = scratch.heightBlocks[corner + samplesPerSide + 1];

        if (shareX + shareZ <= 1f)
            return low + (lowNext - low) * shareX + (high - low) * shareZ;

        return highNext + (high - highNext) * (1f - shareX) + (lowNext - highNext) * (1f - shareZ);
    }

    // The lattice point at the low corner of the cell a point of the tile lies in
    private int resolveCellCorner(MacroBuildAsyncContainer scratch, float x, float z) {

        float cellSizeBlocks = (float) tileSizeBlocks / scratch.cellsPerSide;
        int lastCell = scratch.cellsPerSide - 1;
        int cellX = Math.min((int) (x / cellSizeBlocks), lastCell);
        int cellZ = Math.min((int) (z / cellSizeBlocks), lastCell);

        return cellZ * scratch.getSamplesPerSide() + cellX;
    }
}
