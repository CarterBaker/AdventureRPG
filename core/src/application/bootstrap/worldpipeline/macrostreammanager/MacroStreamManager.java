package application.bootstrap.worldpipeline.macrostreammanager;

import application.bootstrap.worldpipeline.grid.GridInstance;
import engine.root.ManagerPackage;

public class MacroStreamManager extends ManagerPackage {

    /*
     * Internal macro terrain streaming facade. Owned and created by
     * WorldStreamManager. Macro chunks are coarse heightfield tiles sampled
     * straight from the terrain noise, filling the view beyond the chunk grid.
     * Every grid owns its own activeMacroChunks map and macro ring; the pool
     * and all queue logic live in MacroQueueManager.
     */

    // Internal
    private MacroQueueManager macroQueueManager;

    // Internal \\

    @Override
    protected void create() {
        this.macroQueueManager = create(MacroQueueManager.class);
    }

    // Grid Events \\

    public void onGridRebuilt(GridInstance grid) {
        macroQueueManager.onGridRebuilt(grid);
    }

    public void onGridRemoved(GridInstance grid) {
        macroQueueManager.onGridRemoved(grid);
    }
}
