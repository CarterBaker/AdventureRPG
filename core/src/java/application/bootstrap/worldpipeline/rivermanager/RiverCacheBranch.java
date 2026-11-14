package application.bootstrap.worldpipeline.rivermanager;

import application.bootstrap.worldpipeline.river.RiverNetworkStruct;
import application.bootstrap.worldpipeline.river.RiverPathStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;

class RiverCacheBranch extends BranchPackage {

    /*
     * Async — every reach planned so far, keyed by the drainage cell it sets
     * out from, the least recently used let go once the cache is full, so a
     * reach is planned once however many chunks, probes, macros and map tiles
     * ask for it. The planning runs outside the lock on whatever thread first
     * asks; when two threads race to the same reach the first one stored
     * wins, and both are the same since a reach is a pure function of its
     * network. A reach planned against a network since replaced is planned
     * again.
     */

    // Internal
    private RiverPlanBranch riverPlanBranch;

    // Paths — guarded by sourceIndex2Path
    private Int2ObjectLinkedOpenHashMap<RiverPathStruct> sourceIndex2Path;

    // Base \\

    @Override
    protected void create() {
        this.sourceIndex2Path = new Int2ObjectLinkedOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.riverPlanBranch = get(RiverPlanBranch.class);
    }

    // Path \\

    // The reach a river cell sends to the cell it drains into
    RiverPathStruct getPath(RiverNetworkStruct network, int sourceIndex) {

        synchronized (sourceIndex2Path) {

            RiverPathStruct path = sourceIndex2Path.getAndMoveToLast(sourceIndex);

            if (path != null && path.getNetwork() == network)
                return path;
        }

        RiverPathStruct planned = riverPlanBranch.plan(network, sourceIndex);

        synchronized (sourceIndex2Path) {

            RiverPathStruct path = sourceIndex2Path.get(sourceIndex);

            if (path != null && path.getNetwork() == network)
                return path;

            sourceIndex2Path.putAndMoveToLast(sourceIndex, planned);

            while (sourceIndex2Path.size() > EngineSetting.RIVER_CACHE_CAPACITY)
                sourceIndex2Path.removeFirst();

            return planned;
        }
    }
}
