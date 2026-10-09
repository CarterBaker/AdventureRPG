package application.bootstrap.mappipeline.mapmanager;

import application.bootstrap.mappipeline.map.MapTileInstance;
import application.bootstrap.mappipeline.util.MapShadeUtility;
import application.bootstrap.worldpipeline.layout.LayoutSurfaceKind;
import application.bootstrap.worldpipeline.layout.LayoutSurfaceStruct;
import application.bootstrap.worldpipeline.layoutmanager.LayoutManager;
import application.bootstrap.worldpipeline.settlementmanager.SettlementManager;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldgenerationmanager.TerrainSurfaceSampleStruct;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.graphics.color.PackedColorUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

public class MapGenerationBranch extends BranchPackage {

    /*
     * Async — generates a map tile on the WorldMap pool, straight from the
     * terrain through WorldGenerationManager.sampleSurface(), the same sample
     * the macro terrain is built from, so the map and the distant world agree.
     * The terrain is sampled on a lattice at most MAP_TILE_SAMPLES_PER_SIDE on
     * a side, one sample beyond the tile all round, and spread to every texel,
     * one texel beyond the tile all round, so each texel finds its slope; then
     * each texel is shaded as land or water, the sea and still water alike
     * deepening below their own surfaces. Once the lattice is fine enough to
     * show a building, the settlements and roads reaching the tile are
     * gathered and laid over it: roads recolor the ground and structures,
     * walls and bridges stand at their own heights in their own colors, so
     * the map shades them like the land. The tile raises its generated flag
     * only once its pixels are whole.
     */

    // Internal
    private ThreadHandle threadHandle;
    private WorldGenerationManager worldGenerationManager;
    private SettlementManager settlementManager;
    private LayoutManager layoutManager;
    private MapTileAsyncContainer mapTileAsyncContainer;

    // Base \\

    @Override
    protected void create() {
        this.mapTileAsyncContainer = create(MapTileAsyncContainer.class);
    }

    @Override
    protected void get() {
        this.threadHandle = getThreadHandleFromThreadName(EngineSetting.MAP_THREAD_NAME);
        this.worldGenerationManager = get(WorldGenerationManager.class);
        this.settlementManager = get(SettlementManager.class);
        this.layoutManager = get(LayoutManager.class);
    }

    // Generation \\

    boolean hasCapacity() {
        return threadHandle.hasCapacity();
    }

    void generateTile(MapTileInstance tile, WorldHandle worldHandle, double tileBlocks) {

        tile.beginGeneration();

        executeAsync(threadHandle, () -> {

            MapTileAsyncContainer scratch = mapTileAsyncContainer.getInstance();

            try {
                sampleLattice(scratch, worldHandle, tile, tileBlocks);
                resolveTexels(scratch, tileBlocks);
                shadeTexels(scratch, tile, tileBlocks);
            } finally {
                scratch.reset();
                tile.finishGeneration();
            }
        });
    }

    // Lattice \\

    // The lattice starts one sample before the tile and runs one past it, so every border texel lies inside it
    private void sampleLattice(
            MapTileAsyncContainer scratch,
            WorldHandle worldHandle,
            MapTileInstance tile,
            double tileBlocks) {

        double spacing = resolveSpacing(tileBlocks);
        double latticeX = tile.getTileX() * tileBlocks - spacing;
        double latticeZ = tile.getTileZ() * tileBlocks - spacing;
        TerrainSurfaceSampleStruct sample = scratch.sample;

        scratch.samplesPerSide = (int) Math.round(tileBlocks / spacing) + 3;

        gatherLayouts(scratch, worldHandle, latticeX, latticeZ, spacing);

        for (int z = 0; z < scratch.samplesPerSide; z++) {
            for (int x = 0; x < scratch.samplesPerSide; x++) {

                int index = z * scratch.samplesPerSide + x;

                worldGenerationManager.sampleSurface(
                        worldHandle,
                        WorldWrapUtility.wrapBlockX(worldHandle, latticeX + x * spacing),
                        WorldWrapUtility.wrapBlockZ(worldHandle, latticeZ + z * spacing),
                        sample);

                scratch.sampleHeights[index] = sample.getGroundHeightBlocks();
                scratch.sampleWater[index] = sample.isOpenWater() || sample.isLakeWater() ? 1f : 0f;
                scratch.sampleWaterSurfaces[index] = sample.getWaterSurfaceBlocks();
                scratch.sampleTopColors[index] = sample.getTopColor();
                scratch.sampleSideColors[index] = sample.getSideColor();

                overlayLayouts(scratch, worldHandle, index, latticeX + x * spacing, latticeZ + z * spacing, spacing);
            }
        }
    }

    // Layouts \\

    // Every settlement and road reaching the lattice, gathered only while it is fine enough to show a building
    private void gatherLayouts(
            MapTileAsyncContainer scratch,
            WorldHandle worldHandle,
            double latticeX,
            double latticeZ,
            double spacing) {

        scratch.layouts.clear();

        if (spacing > EngineSetting.SETTLEMENT_MAP_MAX_SPACING_BLOCKS)
            return;

        double span = (scratch.samplesPerSide - 1) * spacing;

        settlementManager.collectLayouts(
                worldHandle, latticeX - spacing, latticeZ - spacing,
                latticeX + span + spacing, latticeZ + span + spacing, scratch.layouts);
    }

    // What the gathered layouts show at one lattice point, laid over the land sampled there
    private void overlayLayouts(
            MapTileAsyncContainer scratch,
            WorldHandle worldHandle,
            int index,
            double x,
            double z,
            double spacing) {

        if (scratch.layouts.isEmpty())
            return;

        LayoutSurfaceStruct surface = scratch.layoutSurface;

        layoutManager.sampleSurface(worldHandle, scratch.layouts, x, z, spacing * 0.5, surface);

        if (!surface.isFound())
            return;

        if (surface.getKind() == LayoutSurfaceKind.GROUND && scratch.sampleWater[index] > 0f)
            return;

        if (surface.getKind() == LayoutSurfaceKind.RAISED) {
            scratch.sampleHeights[index] = Math.max(scratch.sampleHeights[index], surface.getHeightBlocks());
            scratch.sampleWater[index] = 0f;
            scratch.sampleSideColors[index] = surface.getSideColor();
        }

        scratch.sampleTopColors[index] = surface.getTopColor();
    }

    double resolveSpacing(double tileBlocks) {
        return Math.max(
                tileBlocks / EngineSetting.MAP_TILE_SAMPLES_PER_SIDE,
                EngineSetting.MAP_TILE_SAMPLE_SPACING_MIN_BLOCKS);
    }

    // Texels \\

    // Texel (x, z) of the bordered grid is texel (x - 1, z - 1) of the tile
    private void resolveTexels(MapTileAsyncContainer scratch, double tileBlocks) {

        double spacing = resolveSpacing(tileBlocks);
        double texelBlocks = tileBlocks / EngineSetting.MAP_TILE_TEXELS;
        int texelsPerSide = MapTileAsyncContainer.TEXELS_PER_SIDE;
        int samplesPerSide = scratch.samplesPerSide;

        for (int z = 0; z < texelsPerSide; z++) {

            double sampleZ = ((z - 0.5) * texelBlocks + spacing) / spacing;
            int cellZ = (int) Math.floor(sampleZ);
            float tz = (float) (sampleZ - cellZ);

            for (int x = 0; x < texelsPerSide; x++) {

                double sampleX = ((x - 0.5) * texelBlocks + spacing) / spacing;
                int cellX = (int) Math.floor(sampleX);
                float tx = (float) (sampleX - cellX);

                int corner = cellZ * samplesPerSide + cellX;
                int below = corner + samplesPerSide;
                int texel = z * texelsPerSide + x;

                scratch.texelHeights[texel] = bilinear(scratch.sampleHeights, corner, below, tx, tz);
                scratch.texelWater[texel] = bilinear(scratch.sampleWater, corner, below, tx, tz);
                scratch.texelWaterSurfaces[texel] = bilinear(scratch.sampleWaterSurfaces, corner, below, tx, tz);
                scratch.texelTopColors[texel] = bilinear(scratch.sampleTopColors, corner, below, tx, tz);
                scratch.texelSideColors[texel] = bilinear(scratch.sampleSideColors, corner, below, tx, tz);
            }
        }
    }

    private float bilinear(float[] values, int corner, int below, float tx, float tz) {

        float top = values[corner] + (values[corner + 1] - values[corner]) * tx;
        float bottom = values[below] + (values[below + 1] - values[below]) * tx;

        return top + (bottom - top) * tz;
    }

    private int bilinear(int[] colors, int corner, int below, float tx, float tz) {
        return PackedColorUtility.mix(
                PackedColorUtility.mix(colors[corner], colors[corner + 1], tx),
                PackedColorUtility.mix(colors[below], colors[below + 1], tx),
                tz);
    }

    // Shading \\

    private void shadeTexels(MapTileAsyncContainer scratch, MapTileInstance tile, double tileBlocks) {

        float texelBlocks = (float) (tileBlocks / EngineSetting.MAP_TILE_TEXELS);
        int texelsPerSide = MapTileAsyncContainer.TEXELS_PER_SIDE;
        byte[] pixels = tile.getPixels();

        for (int z = 0; z < EngineSetting.MAP_TILE_TEXELS; z++) {
            for (int x = 0; x < EngineSetting.MAP_TILE_TEXELS; x++) {

                int texel = (z + 1) * texelsPerSide + x + 1;
                int color;

                if (scratch.texelWater[texel] >= 0.5f)
                    color = MapShadeUtility.shadeWater(
                            scratch.texelTopColors[texel],
                            scratch.texelWaterSurfaces[texel] - scratch.texelHeights[texel]);
                else
                    color = MapShadeUtility.shadeLand(
                            scratch.texelTopColors[texel],
                            scratch.texelSideColors[texel],
                            (scratch.texelHeights[texel + 1] - scratch.texelHeights[texel - 1]) / (2f * texelBlocks),
                            (scratch.texelHeights[texel + texelsPerSide] - scratch.texelHeights[texel - texelsPerSide])
                                    / (2f * texelBlocks),
                            texelBlocks);

                MapShadeUtility.writeTexel(pixels, z * EngineSetting.MAP_TILE_TEXELS + x, color);
            }
        }
    }
}
