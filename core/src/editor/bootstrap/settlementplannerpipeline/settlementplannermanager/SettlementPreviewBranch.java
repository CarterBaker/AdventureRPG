package editor.bootstrap.settlementplannerpipeline.settlementplannermanager;

import application.bootstrap.mappipeline.mapmanager.MapManager;
import application.bootstrap.worldpipeline.layout.LayoutDraftStruct;
import application.bootstrap.worldpipeline.layout.LayoutPlanStruct;
import application.bootstrap.worldpipeline.layoutmanager.LayoutManager;
import application.bootstrap.worldpipeline.settlementmanager.SettlementManager;
import application.bootstrap.worldpipeline.structuremanager.StructureManager;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.world.WorldPlacementKind;
import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class SettlementPreviewBranch extends BranchPackage {

    /*
     * Shows a design that does not stand yet on every map, exactly as it
     * would stand at the planner's site. A request plans it on the WorldMap
     * pool, a settlement through the same path generation plans a hand
     * placement by and a structure as a layout of its one lot, and once the
     * plan is back on the main thread it is handed to MapManager as the
     * preview layout, which redraws only the tiles it reaches. Only one plan
     * runs at a time and a newer request supersedes it, so a burst of changes
     * plans once more at most. A design already standing in the world, the
     * placement being edited, is planned for its summary alone and never
     * shown twice. The finished plan is counted for the planner's summary:
     * its buildings, its roads and whether it is walled.
     */

    // Internal
    private ThreadHandle threadHandle;
    private MapManager mapManager;
    private SettlementManager settlementManager;
    private StructureManager structureManager;
    private LayoutManager layoutManager;
    private SettlementPlannerManager settlementPlannerManager;

    // Request — main thread
    private WorldHandle requestedWorldHandle;
    private WorldPlacementStruct requestedPlacement;
    private boolean requestedShowing;
    private int requestRevision;
    private boolean pending;
    private boolean planning;
    private int planningRevision;
    private WorldHandle planningWorldHandle;
    private boolean planningShowing;

    // Result — written by the worker, read once finished is raised
    private LayoutPlanStruct plannedLayout;
    private volatile boolean finished;

    // Summary
    private boolean summarized;
    private int lotCount;
    private int pathCount;
    private boolean walled;

    // Base \\

    @Override
    protected void get() {
        this.threadHandle = getThreadHandleFromThreadName(EngineSetting.MAP_THREAD_NAME);
        this.mapManager = get(MapManager.class);
        this.settlementManager = get(SettlementManager.class);
        this.structureManager = get(StructureManager.class);
        this.layoutManager = get(LayoutManager.class);
        this.settlementPlannerManager = get(SettlementPlannerManager.class);
    }

    // Request \\

    // A design planned at its own column as soon as the pool has room, shown on the map unless it already stands
    void request(WorldHandle worldHandle, WorldPlacementStruct placement, boolean showing) {

        this.requestedWorldHandle = worldHandle;
        this.requestedPlacement = placement;
        this.requestedShowing = showing;
        this.requestRevision++;
        this.pending = true;
        this.summarized = false;

        if (!showing)
            mapManager.setPreviewLayout(null, null);
    }

    // Nothing previewed, any plan still running thrown away when it returns
    void clear() {

        this.requestedPlacement = null;
        this.requestRevision++;
        this.pending = false;
        this.summarized = false;

        mapManager.setPreviewLayout(null, null);
    }

    // Update \\

    @Override
    protected void update() {

        if (planning && finished)
            publish();

        if (pending && !planning && threadHandle.hasCapacity())
            submit();
    }

    private void submit() {

        WorldHandle worldHandle = requestedWorldHandle;
        WorldPlacementStruct placement = requestedPlacement;

        this.pending = false;
        this.planning = true;
        this.finished = false;
        this.planningRevision = requestRevision;
        this.planningWorldHandle = worldHandle;
        this.planningShowing = requestedShowing;

        executeAsync(threadHandle, () -> {
            try {
                plannedLayout = plan(worldHandle, placement);
            } finally {
                finished = true;
            }
        });
    }

    // A plan answering the newest request is shown; an older one is let go, the newest already waiting its turn
    private void publish() {

        LayoutPlanStruct layout = plannedLayout;

        this.planning = false;
        this.plannedLayout = null;

        if (planningRevision != requestRevision || layout == null)
            return;

        if (planningShowing)
            mapManager.setPreviewLayout(planningWorldHandle, layout);

        summarize(layout);
        settlementPlannerManager.notifyChanged();
    }

    // Worker \\

    private LayoutPlanStruct plan(WorldHandle worldHandle, WorldPlacementStruct placement) {
        return placement.getKind() == WorldPlacementKind.SETTLEMENT
                ? settlementManager.planPlacement(worldHandle, placement).getLayout()
                : planStructure(worldHandle, placement);
    }

    // One structure on its own lot at the placement's column, turned as it will stand
    private LayoutPlanStruct planStructure(WorldHandle worldHandle, WorldPlacementStruct placement) {

        long anchorX = WorldWrapUtility.wrapBlockX(worldHandle, placement.getWorldX());
        long anchorZ = WorldWrapUtility.wrapBlockZ(worldHandle, placement.getWorldZ());
        LayoutDraftStruct draft = new LayoutDraftStruct(
                worldHandle,
                anchorX + EngineSetting.BLOCK_CENTER_OFFSET,
                anchorZ + EngineSetting.BLOCK_CENTER_OFFSET);
        int ground = layoutManager.probeGround(worldHandle, anchorX, anchorZ);

        layoutManager.placeLotAt(
                draft,
                structureManager.getStructureHandleFromStructureName(placement.getName()),
                placement.getQuarterTurns(),
                anchorX,
                anchorZ,
                ground == EngineSetting.LAYOUT_GROUND_FLOODED ? EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS : ground);

        return layoutManager.seal(draft);
    }

    // Summary \\

    private void summarize(LayoutPlanStruct layout) {

        this.summarized = true;
        this.lotCount = layout.getLots().length;
        this.pathCount = layout.getPaths().length;
        this.walled = layout.getWalls().length > 0;
    }

    boolean isSummarized() {
        return summarized;
    }

    boolean isPlanning() {
        return pending || planning;
    }

    int getLotCount() {
        return lotCount;
    }

    int getPathCount() {
        return pathCount;
    }

    boolean isWalled() {
        return walled;
    }
}
