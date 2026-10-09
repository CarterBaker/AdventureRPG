package application.bootstrap.worldpipeline.macrostreammanager;

import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeSiteStruct;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import application.bootstrap.worldpipeline.util.TreeImpostorUtility;
import application.bootstrap.worldpipeline.util.TreeSkeletonUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldgenerationmanager.TerrainSurfaceSampleStruct;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import engine.graphics.color.PackedColorUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class MacroTreeBranch extends BranchPackage {

    /*
     * Async — the trees the macro terrain nearest the chunk grid stands in one
     * by one, on a tile sampled finely enough to show them. Every wild tree
     * rooted in the tile is found exactly where chunk placement finds it,
     * through TreeManager.collectSites(), and stood on the tile's own surface
     * as a trunk box under the very crown a mega's stand-in grows — the same
     * lumps, sized from its mature height and age — in the flat colors its
     * bark and leaves show from afar. A root under water is left out: a cell
     * wholly dry or wholly wet answers from its corners, and a shore cell
     * asks the terrain at the root itself. Every box carries the chunk its
     * tree roots in, so a whole tree yields at once the moment the chunk grid
     * takes that chunk over, never split across the seam. The boxes are laid
     * into the tile's mesh by MacroMeshBranch.
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

    // Worker — every tree rooted in the tile stood in, read from the lattice the tile was just sampled at
    void sampleStandIns(MacroBuildAsyncContainer scratch, WorldHandle worldHandle, long coordinate) {

        scratch.standInCrowns.clear();
        scratch.standInTrunks.clear();

        if (!scratch.standIns)
            return;

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
            standIn(scratch, worldHandle, scratch.sites.get(i), originX, originZ);

        scratch.sites.clear();
    }

    private void standIn(
            MacroBuildAsyncContainer scratch,
            WorldHandle worldHandle,
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
        float[] crown = scratch.crown;

        TreeImpostorUtility.estimateCrown(
                treeHandle,
                TreeSkeletonUtility.resolveMatureHeight(treeHandle, site.getSeed()),
                site.getAge(),
                crown);

        if (crown[EngineSetting.TREE_IMPOSTOR_CROWN_RADIUS] > 0f)
            standInCrown(scratch, treeHandle, rootX, rootY, rootZ, cover);

        float trunkTop = crown[EngineSetting.TREE_IMPOSTOR_TRUNK_TOP];
        float halfWidth = crown[EngineSetting.TREE_IMPOSTOR_TRUNK_HALF_WIDTH];

        if (trunkTop <= 0f)
            return;

        int albedo = treeHandle.getPartAlbedos()[EngineSetting.TREE_PART_BARK];

        addBox(scratch.standInTrunks,
                rootX - halfWidth, rootY - EngineSetting.TREE_TRUNK_SINK_BLOCKS, rootZ - halfWidth,
                rootX + halfWidth, rootY + trunkTop, rootZ + halfWidth,
                albedo, PackedColorUtility.scale(albedo, EngineSetting.TREE_CANOPY_SIDE_SHADE), cover);
    }

    // The crown's lumps as boxes, each covering what the lump's rounded cluster does
    private void standInCrown(
            MacroBuildAsyncContainer scratch,
            TreeHandle treeHandle,
            float rootX,
            float rootY,
            float rootZ,
            float cover) {

        float[] lumps = scratch.lumps;
        int count = TreeImpostorUtility.layoutCrown(treeHandle.getForm(), scratch.crown, lumps);
        int albedo = treeHandle.getPartAlbedos()[EngineSetting.TREE_PART_LEAF];
        int sideColor = PackedColorUtility.scale(albedo, EngineSetting.TREE_CANOPY_SIDE_SHADE);

        for (int lump = 0; lump < count; lump++) {

            int offset = lump * EngineSetting.TREE_IMPOSTOR_LUMP_FLOATS;
            float centerY = rootY + lumps[offset + EngineSetting.TREE_IMPOSTOR_LUMP_CENTER_Y];
            float radiusH = lumps[offset + EngineSetting.TREE_IMPOSTOR_LUMP_RADIUS_H]
                    * EngineSetting.TREE_STAND_IN_LUMP_SHARE;
            float radiusV = lumps[offset + EngineSetting.TREE_IMPOSTOR_LUMP_RADIUS_V]
                    * EngineSetting.TREE_STAND_IN_LUMP_SHARE;

            addBox(scratch.standInCrowns,
                    rootX - radiusH, centerY - radiusV, rootZ - radiusH,
                    rootX + radiusH, centerY + radiusV, rootZ + radiusH,
                    albedo, sideColor, cover);
        }
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

    // Boxes \\

    private void addBox(
            FloatArrayList boxes,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            int topColor,
            int sideColor,
            float cover) {

        boxes.add(minX);
        boxes.add(minY);
        boxes.add(minZ);
        boxes.add(maxX);
        boxes.add(maxY);
        boxes.add(maxZ);
        boxes.add(topColor);
        boxes.add(sideColor);
        boxes.add(cover);
    }
}
