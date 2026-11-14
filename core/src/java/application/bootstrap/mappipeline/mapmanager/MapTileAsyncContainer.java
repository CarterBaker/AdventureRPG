package application.bootstrap.mappipeline.mapmanager;

import application.bootstrap.worldpipeline.layout.LayoutPlanStruct;
import application.bootstrap.worldpipeline.layout.LayoutSurfaceStruct;
import application.bootstrap.worldpipeline.worldgenerationmanager.TerrainSurfaceSampleStruct;
import engine.root.AsyncContainerPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MapTileAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for one map tile: the terrain lattice the tile is
     * sampled on, one sample beyond the tile on every side, and the ground,
     * water cover, water surface and colors resolved at every texel, one texel beyond the tile
     * on every side so each inner texel finds its slope, and the settlement
     * and road layouts reaching the tile with the sample each point is read
     * against them through. Filled and read by MapGenerationBranch on the
     * WorldMap pool, so a tile allocates nothing.
     */

    static final int MAX_SAMPLES_PER_SIDE = EngineSetting.MAP_TILE_SAMPLES_PER_SIDE + 3;
    static final int TEXELS_PER_SIDE = EngineSetting.MAP_TILE_TEXELS + 2;

    // Lattice
    int samplesPerSide;
    float[] sampleHeights;
    float[] sampleWater;
    float[] sampleWaterSurfaces;
    int[] sampleTopColors;
    int[] sampleSideColors;

    // Texels
    float[] texelHeights;
    float[] texelWater;
    float[] texelWaterSurfaces;
    int[] texelTopColors;
    int[] texelSideColors;

    // Layouts
    ObjectArrayList<LayoutPlanStruct> layouts;
    LayoutSurfaceStruct layoutSurface;

    // Surface
    TerrainSurfaceSampleStruct sample;

    @Override
    protected void create() {

        int sampleCount = MAX_SAMPLES_PER_SIDE * MAX_SAMPLES_PER_SIDE;
        int texelCount = TEXELS_PER_SIDE * TEXELS_PER_SIDE;

        this.sampleHeights = new float[sampleCount];
        this.sampleWater = new float[sampleCount];
        this.sampleWaterSurfaces = new float[sampleCount];
        this.sampleTopColors = new int[sampleCount];
        this.sampleSideColors = new int[sampleCount];
        this.texelHeights = new float[texelCount];
        this.texelWater = new float[texelCount];
        this.texelWaterSurfaces = new float[texelCount];
        this.texelTopColors = new int[texelCount];
        this.texelSideColors = new int[texelCount];
        this.layouts = new ObjectArrayList<>();
        this.layoutSurface = new LayoutSurfaceStruct();
        this.sample = new TerrainSurfaceSampleStruct();
    }

    @Override
    public void reset() {
        sample.getBlend().reset();
        layouts.clear();
    }
}
