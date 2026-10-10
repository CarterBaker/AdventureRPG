package application.bootstrap.worldpipeline.macrostreammanager;

import application.bootstrap.worldpipeline.layout.LayoutPlanStruct;
import application.bootstrap.worldpipeline.layout.LayoutSurfaceStruct;
import application.bootstrap.worldpipeline.tree.TreeSiteStruct;
import application.bootstrap.worldpipeline.worldgenerationmanager.TerrainSurfaceSampleStruct;
import engine.root.AsyncContainerPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MacroBuildAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for one macro build, sized for the finest lattice
     * a tile can take: the ground height, packed top and side colors and open
     * sea and still water flags at every lattice point, the lattice's lowest
     * ground, the canopy its woods raise over it with that canopy's colors,
     * the stand-in boxes its trees are drawn as with the sites they stand on
     * and the crown each is sized by, what settlements and their roads raise
     * over it with their colors, the layouts reaching the tile with the
     * sample each point is read against them through, and the surface sample
     * each point is read through. Filled by MacroBuildBranch,
     * MacroCanopyBranch, MacroTreeBranch and MacroSettlementBranch and read
     * by MacroMeshBranch, so a build allocates nothing once its lists have
     * grown.
     */

    static final int MAX_SAMPLES_PER_SIDE = EngineSetting.MACRO_CELLS_PER_SIDE_MAX + 1;
    static final int MAX_SAMPLE_COUNT = MAX_SAMPLES_PER_SIDE * MAX_SAMPLES_PER_SIDE;

    // Lattice
    int cellsPerSide;
    float[] heightBlocks;
    float[] topColors;
    float[] sideColors;
    boolean[] openWater;
    boolean[] stillWater;
    float minHeightBlocks;
    int openWaterCount;

    // Canopy — blocks the woods stand above the ground, zero where none do
    float[] canopyHeights;
    float[] canopyTopColors;
    float[] canopySideColors;
    int canopyCount;

    // Stand-Ins — boxes as bounds then top, side and cover, crowns apart from trunks; set when the tile draws them
    boolean standIns;
    FloatArrayList standInCrowns;
    FloatArrayList standInTrunks;
    ObjectArrayList<TreeSiteStruct> sites;
    float[] crown;
    float[] lumps;

    // Settlements — blocks structures, walls and bridges stand above the ground, zero where none do
    float[] structureHeights;
    float[] structureTopColors;
    float[] structureSideColors;
    int structureCount;

    // Layouts
    ObjectArrayList<LayoutPlanStruct> layouts;
    LayoutSurfaceStruct layoutSurface;

    // Surface
    TerrainSurfaceSampleStruct sample;

    @Override
    protected void create() {
        this.heightBlocks = new float[MAX_SAMPLE_COUNT];
        this.topColors = new float[MAX_SAMPLE_COUNT];
        this.sideColors = new float[MAX_SAMPLE_COUNT];
        this.openWater = new boolean[MAX_SAMPLE_COUNT];
        this.stillWater = new boolean[MAX_SAMPLE_COUNT];
        this.canopyHeights = new float[MAX_SAMPLE_COUNT];
        this.canopyTopColors = new float[MAX_SAMPLE_COUNT];
        this.canopySideColors = new float[MAX_SAMPLE_COUNT];
        this.standInCrowns = new FloatArrayList();
        this.standInTrunks = new FloatArrayList();
        this.sites = new ObjectArrayList<>();
        this.crown = new float[EngineSetting.TREE_IMPOSTOR_CROWN_FLOATS];
        this.lumps = new float[EngineSetting.TREE_IMPOSTOR_LUMPS_MAX_FLOATS];
        this.structureHeights = new float[MAX_SAMPLE_COUNT];
        this.structureTopColors = new float[MAX_SAMPLE_COUNT];
        this.structureSideColors = new float[MAX_SAMPLE_COUNT];
        this.layouts = new ObjectArrayList<>();
        this.layoutSurface = new LayoutSurfaceStruct();
        this.sample = new TerrainSurfaceSampleStruct();
    }

    @Override
    public void reset() {
        sample.getBlend().reset();
        this.openWaterCount = 0;
        this.canopyCount = 0;
        this.structureCount = 0;
        this.layouts.clear();
        standInCrowns.clear();
        standInTrunks.clear();
        sites.clear();
    }

    // Lattice \\

    int getSamplesPerSide() {
        return cellsPerSide + 1;
    }

    int getSampleCount() {
        return (cellsPerSide + 1) * (cellsPerSide + 1);
    }
}
