package application.bootstrap.worldpipeline.treemanager;

import java.util.Arrays;

import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.tree.TreeSiteStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.AsyncContainerPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TreeSiteAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for finding the wild trees of one block region:
     * the region widened by a full clearance on every side, the biomes found
     * around it and the placement cells one kind of tree covers, every site
     * rolled so far with the keys already taken, and the buckets that sort
     * those sites by place so each is judged only against its neighbours. The
     * sites are pooled and kept warm between searches, so a search allocates
     * nothing once its pool has grown.
     */

    // Region — the widened region, its corner and its span in blocks
    WorldHandle worldHandle;
    long minX;
    long minZ;
    int spanX;
    int spanZ;

    // Biomes
    IntOpenHashSet biomeIDs;
    ObjectArrayList<BiomeHandle> biomes;
    BiomeBlendStruct blend;

    // Placement Cells
    IntArrayList cellsX;
    IntArrayList cellsZ;

    // Sites — the pool, the first siteCount of it rolled this search
    ObjectArrayList<TreeSiteStruct> sites;
    int siteCount;
    LongOpenHashSet siteKeys;

    // Buckets — the first site of each bucket and the next site after each, INDEX_NOT_FOUND where none follows
    int bucketSize;
    int bucketsX;
    int bucketsZ;
    int[] bucketHeads;
    int[] bucketNext;

    @Override
    protected void create() {

        // Biomes
        this.biomeIDs = new IntOpenHashSet();
        this.biomes = new ObjectArrayList<>();
        this.blend = new BiomeBlendStruct();

        // Placement Cells
        this.cellsX = new IntArrayList();
        this.cellsZ = new IntArrayList();

        // Sites
        this.sites = new ObjectArrayList<>();
        this.siteKeys = new LongOpenHashSet();

        // Buckets
        this.bucketHeads = new int[0];
        this.bucketNext = new int[0];
    }

    @Override
    public void reset() {
        biomeIDs.clear();
        biomes.clear();
        siteKeys.clear();
        this.siteCount = 0;
    }

    // Sites \\

    // The next pooled site, grown when every one is in use
    TreeSiteStruct nextSite() {

        if (siteCount == sites.size())
            sites.add(new TreeSiteStruct());

        return sites.get(siteCount++);
    }

    // Buckets \\

    // Every bucket emptied, each array grown to hold the region's buckets and its sites
    void prepareBuckets(int bucketSize) {

        this.bucketSize = bucketSize;
        this.bucketsX = (spanX + bucketSize - 1) / bucketSize;
        this.bucketsZ = (spanZ + bucketSize - 1) / bucketSize;

        int bucketCount = bucketsX * bucketsZ;

        if (bucketHeads.length < bucketCount)
            this.bucketHeads = new int[bucketCount];

        if (bucketNext.length < siteCount)
            this.bucketNext = new int[sites.size()];

        Arrays.fill(bucketHeads, 0, bucketCount, EngineSetting.INDEX_NOT_FOUND);
    }
}
