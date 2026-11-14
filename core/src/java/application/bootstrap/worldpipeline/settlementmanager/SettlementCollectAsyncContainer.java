package application.bootstrap.worldpipeline.settlementmanager;

import application.bootstrap.worldpipeline.layout.LayoutPlanStruct;
import engine.root.AsyncContainerPackage;
import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SettlementCollectAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for gathering the layouts that reach a stretch of
     * the world: each found layout under the key that orders it, roads before
     * settlements and cells in one fixed order, so every chunk lays
     * overlapping layouts alike, and the gathered layouts handed to a
     * single-point query.
     */

    // Order
    Long2ObjectAVLTreeMap<LayoutPlanStruct> order2Layout;

    // Query
    ObjectArrayList<LayoutPlanStruct> layouts;

    @Override
    protected void create() {
        this.order2Layout = new Long2ObjectAVLTreeMap<>();
        this.layouts = new ObjectArrayList<>();
    }

    @Override
    public void reset() {
        order2Layout.clear();
    }
}
