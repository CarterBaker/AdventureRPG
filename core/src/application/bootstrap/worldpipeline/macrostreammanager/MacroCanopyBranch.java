package application.bootstrap.worldpipeline.macrostreammanager;

import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biome.BiomeTreeStruct;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import application.bootstrap.worldpipeline.util.TreeDistributionUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldgenerationmanager.TerrainSurfaceSampleStruct;
import engine.graphics.color.PackedColorUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MacroCanopyBranch extends BranchPackage {

    /*
     * Async — the woods distant macro terrain shows, judged per lattice point
     * without placing or growing a single tree. Every kind of tree the
     * point's biome grows covers a share of its ground, as tree placement
     * would spread it; where they cover enough of it together, a canopy
     * stands over the ground as high as their typical wild trees, jittered a
     * little from point to point so its top is ragged, in the colors their
     * leaves show from afar. Open sea and still water carry none.
     */

    // Internal
    private TreeManager treeManager;

    // Base \\

    @Override
    protected void get() {
        this.treeManager = get(TreeManager.class);
    }

    // Sample \\

    // Worker — the canopy over one lattice point, read from the surface sample the point was just taken through
    void sampleCanopy(
            MacroBuildAsyncContainer scratch,
            WorldHandle worldHandle,
            int index,
            double x,
            double z) {

        scratch.canopyHeights[index] = 0f;

        TerrainSurfaceSampleStruct sample = scratch.sample;

        if (sample.isOpenWater() || sample.isLakeWater())
            return;

        BiomeHandle biome = sample.getBlend().getDominantBiome();

        if (biome == null)
            return;

        ObjectArrayList<BiomeTreeStruct> kinds = biome.getTrees();
        float coverage = 0f;
        float height = 0f;
        float red = 0f;
        float green = 0f;
        float blue = 0f;

        for (int k = 0; k < kinds.size(); k++) {

            BiomeTreeStruct kind = kinds.get(k);
            TreeHandle treeHandle = treeManager.findTreeHandle(kind.getTreeName());

            if (treeHandle == null)
                continue;

            float share = TreeDistributionUtility.resolveCoverage(worldHandle.getSeed(), kind, treeHandle, x, z);

            if (share <= 0f)
                continue;

            int albedo = treeHandle.getPartAlbedos()[EngineSetting.TREE_PART_LEAF];

            coverage += share;
            height += share * TreeDistributionUtility.resolveTypicalHeight(treeHandle);
            red += share * PackedColorUtility.red(albedo);
            green += share * PackedColorUtility.green(albedo);
            blue += share * PackedColorUtility.blue(albedo);
        }

        if (coverage < EngineSetting.TREE_CANOPY_MIN_COVERAGE)
            return;

        float roll = BiomeFieldUtility.hash01(BiomeFieldUtility.hashCell(
                worldHandle.getSeed() ^ EngineSetting.TREE_CANOPY_SALT, (int) Math.floor(x), (int) Math.floor(z)));
        int topColor = PackedColorUtility.pack(red / coverage, green / coverage, blue / coverage);

        scratch.canopyHeights[index] = height / coverage
                * (1f + EngineSetting.TREE_CANOPY_JITTER * (roll * 2f - 1f));
        scratch.canopyTopColors[index] = topColor;
        scratch.canopySideColors[index] = PackedColorUtility.scale(topColor, EngineSetting.TREE_CANOPY_SIDE_SHADE);
        scratch.canopyCount++;
    }
}
