package application.bootstrap.worldpipeline.treemanager;

import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.tree.TreeSiteStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.AsyncContainerPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TreePlacementAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for finding every tree that reaches one chunk: the
     * chunk and the game day it generates on, the wild sites rooted within
     * reach of it, the trees found so far and the planted trees to look
     * through, and the ground under recent anchors. Neighbouring chunks share
     * most of their anchors and generate on the same threads, so the ground
     * cache spares most terrain probes; it is emptied whenever it grows past
     * its bound.
     */

    // Chunk
    WorldHandle worldHandle;
    long chunkOriginX;
    long chunkOriginZ;
    double currentDay;

    // Sites
    ObjectArrayList<TreeSiteStruct> sites;

    // Found
    ObjectArrayList<TreeInstance> trees;
    ObjectArrayList<TreeInstance> planted;

    // Ground — the anchor's ground height, TREE_GROUND_FLOODED under water
    Long2IntOpenHashMap anchor2Ground;

    @Override
    protected void create() {

        // Sites
        this.sites = new ObjectArrayList<>();

        // Found
        this.trees = new ObjectArrayList<>();
        this.planted = new ObjectArrayList<>();

        // Ground
        this.anchor2Ground = new Long2IntOpenHashMap();
        this.anchor2Ground.defaultReturnValue(EngineSetting.TREE_GROUND_UNKNOWN);
    }

    @Override
    public void reset() {
        sites.clear();
        trees.clear();

        if (anchor2Ground.size() > EngineSetting.TREE_GROUND_CACHE_MAX)
            anchor2Ground.clear();
    }
}
