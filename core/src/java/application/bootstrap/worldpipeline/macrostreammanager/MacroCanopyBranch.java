package application.bootstrap.worldpipeline.macrostreammanager;

import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeSiteStruct;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import application.bootstrap.worldpipeline.util.TreeImpostorUtility;
import application.bootstrap.worldpipeline.util.TreeSkeletonUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.graphics.color.PackedColorUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;

public class MacroCanopyBranch extends BranchPackage {

    /*
     * Async — the woods the farthest macro terrain shows, on tiles sampled
     * too coarsely to stand their trees in one by one, raised only where trees
     * truly stand. Every wild tree rooted in the tile is found exactly where
     * chunk placement finds it, through TreeManager.collectSites(), and its
     * crown counted toward the lattice point nearest its root; where the
     * crowns counted to a point cover enough of the ground around it, a
     * canopy stands over it as high as their crowns reach, jittered a little
     * from point to point so its top is ragged, in the colors their leaves
     * show from afar. Clearings, roads, settlements, open sea and still water
     * hold no tree, so they carry none.
     */

    // Internal
    private TreeManager treeManager;

    // Settings
    private int chunkSize;
    private int tileSizeBlocks;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.tileSizeBlocks = EngineSetting.MACRO_TILE_SIZE_BLOCKS;
    }

    @Override
    protected void get() {
        this.treeManager = get(TreeManager.class);
    }

    // Sample \\

    // Worker — the canopy over every lattice point of the tile, read from the lattice it was just sampled at
    void sampleCanopy(MacroBuildAsyncContainer scratch, WorldHandle worldHandle, long coordinate) {

        int sampleCount = scratch.getSampleCount();

        for (int i = 0; i < sampleCount; i++) {
            scratch.canopyHeights[i] = 0f;
            scratch.canopyCover[i] = 0f;
            scratch.canopyRed[i] = 0f;
            scratch.canopyGreen[i] = 0f;
            scratch.canopyBlue[i] = 0f;
        }

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
            countSite(scratch, worldHandle, scratch.sites.get(i), originX, originZ);

        scratch.sites.clear();

        float cellSizeBlocks = (float) tileSizeBlocks / scratch.cellsPerSide;
        float cellArea = cellSizeBlocks * cellSizeBlocks;

        for (int i = 0; i < sampleCount; i++)
            raiseCanopy(scratch, worldHandle, i, cellArea, originX, originZ);
    }

    // One tree's crown counted toward the lattice point nearest its root, unless water stands there
    private void countSite(
            MacroBuildAsyncContainer scratch,
            WorldHandle worldHandle,
            TreeSiteStruct site,
            long originX,
            long originZ) {

        float cellSizeBlocks = (float) tileSizeBlocks / scratch.cellsPerSide;
        float rootX = WorldWrapUtility.wrappedBlockDeltaX(worldHandle, site.getAnchorX(), originX)
                + EngineSetting.TREE_ROOT_CENTER_BLOCKS;
        float rootZ = WorldWrapUtility.wrappedBlockDeltaZ(worldHandle, site.getAnchorZ(), originZ)
                + EngineSetting.TREE_ROOT_CENTER_BLOCKS;
        int x = Math.min(Math.round(rootX / cellSizeBlocks), scratch.cellsPerSide);
        int z = Math.min(Math.round(rootZ / cellSizeBlocks), scratch.cellsPerSide);
        int index = z * scratch.getSamplesPerSide() + x;

        if (scratch.openWater[index] || scratch.stillWater[index])
            return;

        TreeHandle treeHandle = site.getTreeHandle();
        float[] crown = scratch.crown;

        TreeImpostorUtility.estimateCrown(
                treeHandle,
                TreeSkeletonUtility.resolveMatureHeight(treeHandle, site.getSeed()),
                site.getAge(),
                crown);

        float radius = crown[EngineSetting.TREE_IMPOSTOR_CROWN_RADIUS];
        float area = (float) Math.PI * radius * radius;
        int albedo = treeHandle.getPartAlbedos()[EngineSetting.TREE_PART_LEAF];

        scratch.canopyCover[index] += area;
        scratch.canopyHeights[index] += area * crown[EngineSetting.TREE_IMPOSTOR_CROWN_TOP];
        scratch.canopyRed[index] += area * PackedColorUtility.red(albedo);
        scratch.canopyGreen[index] += area * PackedColorUtility.green(albedo);
        scratch.canopyBlue[index] += area * PackedColorUtility.blue(albedo);
    }

    // A canopy over a lattice point whose counted crowns cover enough of its ground, as high as they reach on average
    private void raiseCanopy(
            MacroBuildAsyncContainer scratch,
            WorldHandle worldHandle,
            int index,
            float cellArea,
            long originX,
            long originZ) {

        float cover = scratch.canopyCover[index];

        if (cover <= 0f || cover / cellArea < EngineSetting.TREE_CANOPY_MIN_COVERAGE) {
            scratch.canopyHeights[index] = 0f;
            return;
        }

        int samplesPerSide = scratch.getSamplesPerSide();
        int cellSizeBlocks = tileSizeBlocks / scratch.cellsPerSide;
        float roll = BiomeFieldUtility.hash01(BiomeFieldUtility.hashCell(
                worldHandle.getSeed() ^ EngineSetting.TREE_CANOPY_SALT,
                (int) (originX + (long) (index % samplesPerSide) * cellSizeBlocks),
                (int) (originZ + (long) (index / samplesPerSide) * cellSizeBlocks)));
        int topColor = PackedColorUtility.pack(
                scratch.canopyRed[index] / cover,
                scratch.canopyGreen[index] / cover,
                scratch.canopyBlue[index] / cover);

        scratch.canopyHeights[index] = scratch.canopyHeights[index] / cover
                * (1f + EngineSetting.TREE_CANOPY_JITTER * (roll * 2f - 1f));
        scratch.canopyTopColors[index] = topColor;
        scratch.canopySideColors[index] = PackedColorUtility.scale(topColor, EngineSetting.TREE_CANOPY_SIDE_SHADE);
        scratch.canopyCount++;
    }
}
