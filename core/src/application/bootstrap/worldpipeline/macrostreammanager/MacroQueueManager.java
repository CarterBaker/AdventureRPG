package application.bootstrap.worldpipeline.macrostreammanager;

import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.macrochunk.MacroChunkInstance;
import application.bootstrap.worldpipeline.macrochunk.MacroDataSyncContainer;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

class MacroQueueManager extends ManagerPackage {

    /*
     * Drives the per-frame macro pipeline for every grid: re-anchor, admit,
     * assess. A macro tile is world-aligned, so walking only re-places the
     * resident tiles and streams the ring's edge; nothing is rebuilt. Builds
     * are paced by the MacroStreaming pool's capacity and uploads by their own
     * budget, and a macro with a build reserved is never recycled, so a pooled
     * macro never has work in flight.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private WorldRenderManager worldRenderManager;
    private ThreadHandle macroStreamingThreadHandle;

    // Branches
    private MacroRingBranch ringBranch;
    private MacroBuildBranch buildBranch;
    private MacroRenderBranch renderBranch;

    // Pool — shared across all grids
    private ObjectArrayList<MacroChunkInstance> macroPool;
    private int macroPoolMaxOverflow;

    // Streaming
    private int maxMacroAdmissionsPerFrame;
    private int maxMacroAssessPerFrame;

    // GPU Upload Throttle
    private int macroGpuUploadBudget;
    private int gpuUploadsThisFrame;

    // Internal \\

    @Override
    protected void create() {

        // Branches
        this.ringBranch = create(MacroRingBranch.class);
        this.buildBranch = create(MacroBuildBranch.class);
        this.renderBranch = create(MacroRenderBranch.class);
        create(MacroMeshBranch.class);

        // Pool
        this.macroPool = new ObjectArrayList<>();
        this.macroPoolMaxOverflow = EngineSetting.MACRO_POOL_MAX_OVERFLOW;

        // Streaming
        this.maxMacroAdmissionsPerFrame = EngineSetting.MACRO_ADMISSIONS_PER_FRAME;
        this.maxMacroAssessPerFrame = EngineSetting.MACRO_ASSESS_PER_FRAME;

        // GPU Upload Throttle
        this.macroGpuUploadBudget = EngineSetting.MAX_MACRO_GPU_UPLOADS_PER_FRAME;
    }

    @Override
    protected void get() {

        // Internal
        this.worldStreamManager = get(WorldStreamManager.class);
        this.worldRenderManager = get(WorldRenderManager.class);
        this.macroStreamingThreadHandle = getThreadHandleFromThreadName(EngineSetting.MACRO_STREAMING_THREAD_NAME);
    }

    @Override
    protected void update() {

        this.gpuUploadsThisFrame = 0;

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++) {

            GridInstance grid = (GridInstance) elements[i];

            if (ringBranch.updateRing(grid))
                placeActiveMacros(grid);

            admitMacros(grid);
            assessActiveMacros(grid);
        }
    }

    // Grid Events \\

    void onGridRebuilt(GridInstance grid) {
        flushActiveMacros(grid);
        grid.resetMacroRing();
    }

    void onGridRemoved(GridInstance grid) {
        onGridRebuilt(grid);
    }

    // Placement \\

    private void placeActiveMacros(GridInstance grid) {

        ObjectIterator<Long2ObjectMap.Entry<MacroChunkInstance>> iterator = grid.getActiveMacroChunks()
                .long2ObjectEntrySet()
                .fastIterator();

        while (iterator.hasNext())
            worldRenderManager.placeMacroInstance(iterator.next().getValue(), grid);
    }

    // Admission \\

    private void admitMacros(GridInstance grid) {

        Long2ObjectLinkedOpenHashMap<MacroChunkInstance> activeMacroChunks = grid.getActiveMacroChunks();
        LongArrayList macroLoadOrder = grid.getMacroLoadOrder();

        int cursor = grid.getMacroAdmitCursor();
        int admitted = 0;

        while (cursor < macroLoadOrder.size() && admitted < maxMacroAdmissionsPerFrame) {

            long macroCoordinate = macroLoadOrder.getLong(cursor);
            cursor++;

            if (activeMacroChunks.containsKey(macroCoordinate))
                continue;

            MacroChunkInstance macro = macroPool.isEmpty() ? create(MacroChunkInstance.class) : macroPool.pop();
            macro.constructor(grid.getWorldHandle(), macroCoordinate);
            worldRenderManager.placeMacroInstance(macro, grid);

            activeMacroChunks.put(macroCoordinate, macro);
            admitted++;
        }

        grid.setMacroAdmitCursor(cursor);
    }

    // Assessment \\

    private void assessActiveMacros(GridInstance grid) {

        Long2ObjectLinkedOpenHashMap<MacroChunkInstance> activeMacroChunks = grid.getActiveMacroChunks();
        LongOpenHashSet macroCoordinates = grid.getMacroCoordinates();

        if (activeMacroChunks.isEmpty())
            return;

        var iterator = activeMacroChunks.long2ObjectEntrySet().iterator();
        int assessed = 0;

        while (iterator.hasNext() && assessed < maxMacroAssessPerFrame) {

            var entry = iterator.next();
            long macroCoordinate = entry.getLongKey();
            MacroChunkInstance macro = entry.getValue();
            iterator.remove();
            assessed++;

            if (!macroCoordinates.contains(macroCoordinate)) {

                if (!unloadMacro(grid, macro))
                    activeMacroChunks.put(macroCoordinate, macro);

                continue;
            }

            switch (determineOperation(macro)) {
                case BUILD -> buildBranch.buildMacro(macro);
                case RENDER -> {
                    if (gpuUploadsThisFrame < macroGpuUploadBudget) {
                        renderBranch.renderMacro(macro);
                        gpuUploadsThisFrame++;
                    }
                }
                case SKIP -> {
                }
            }

            activeMacroChunks.put(macroCoordinate, macro);
        }
    }

    private MacroQueueOperation determineOperation(MacroChunkInstance macro) {

        MacroDataSyncContainer sync = macro.getMacroDataSyncContainer();

        if (!sync.tryAcquire())
            return MacroQueueOperation.SKIP;

        try {
            if (sync.isBuilding())
                return MacroQueueOperation.SKIP;

            if (sync.isBuilt())
                return MacroQueueOperation.RENDER;

            if (macro.isRendered())
                return MacroQueueOperation.SKIP;

            // A saturated pool leaves the macro for a later pass before its build is reserved
            if (!macroStreamingThreadHandle.hasCapacity() || !sync.beginWorkLocked())
                return MacroQueueOperation.SKIP;

            return MacroQueueOperation.BUILD;
        } finally {
            sync.release();
        }
    }

    // Unload \\

    private boolean unloadMacro(GridInstance grid, MacroChunkInstance macro) {

        MacroDataSyncContainer sync = macro.getMacroDataSyncContainer();

        if (!sync.tryAcquire())
            return false;

        try {
            if (sync.isBuilding())
                return false;

            worldRenderManager.removeMacroInstance(macro);
            macro.reset();
        } finally {
            sync.release();
        }

        recycleMacro(grid, macro);
        return true;
    }

    private void recycleMacro(GridInstance grid, MacroChunkInstance macro) {

        if (macroPool.size() < grid.getMacroLoadOrder().size() + macroPoolMaxOverflow)
            macroPool.push(macro);
        else
            worldRenderManager.disposeMacroInstance(macro);
    }

    // Flush \\

    private void flushActiveMacros(GridInstance grid) {

        Long2ObjectLinkedOpenHashMap<MacroChunkInstance> activeMacroChunks = grid.getActiveMacroChunks();
        ObjectIterator<Long2ObjectMap.Entry<MacroChunkInstance>> iterator = activeMacroChunks
                .long2ObjectEntrySet()
                .fastIterator();

        while (iterator.hasNext()) {

            MacroChunkInstance macro = iterator.next().getValue();
            MacroDataSyncContainer sync = macro.getMacroDataSyncContainer();

            // A build in flight finishes in milliseconds, and a flush must leave nothing behind
            sync.acquireIdle();

            try {
                worldRenderManager.removeMacroInstance(macro);
                macro.reset();
            } finally {
                sync.release();
            }

            recycleMacro(grid, macro);
        }

        activeMacroChunks.clear();
    }
}
