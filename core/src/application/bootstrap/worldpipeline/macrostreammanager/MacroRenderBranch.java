package application.bootstrap.worldpipeline.macrostreammanager;

import application.bootstrap.worldpipeline.macrochunk.MacroChunkInstance;
import application.bootstrap.worldpipeline.macrochunk.MacroDataSyncContainer;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import engine.root.BranchPackage;

public class MacroRenderBranch extends BranchPackage {

    /*
     * Main thread — uploads a built macro's mesh to the GPU under its lock,
     * records the resolution it was built at, then clears the CPU copy so a
     * resident macro holds no geometry on the heap.
     */

    // Internal
    private WorldRenderManager worldRenderManager;

    // Internal \\

    @Override
    protected void get() {
        this.worldRenderManager = get(WorldRenderManager.class);
    }

    // Render \\

    public void renderMacro(MacroChunkInstance macro) {

        MacroDataSyncContainer sync = macro.getMacroDataSyncContainer();

        if (!sync.tryAcquire())
            return;

        try {
            if (!sync.isBuilt())
                return;

            worldRenderManager.addMacroInstance(macro);
            macro.setBuilt(sync.getBuiltCellsPerSide());
            sync.clearGeometry();
        } finally {
            sync.release();
        }
    }
}
