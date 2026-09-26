package application.bootstrap.worldpipeline.gridmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.ManagerPackage;

public class GridManager extends ManagerPackage {

    /*
     * Pure factory for GridInstance construction and in-place slot rebuilds.
     * Delegates all build logic to GridBuildSystem. Holds no grid state —
     * callers own what they build.
     */

    // Internal
    private GridBuildSystem gridBuildSystem;

    // Internal \\

    @Override
    protected void create() {
        this.gridBuildSystem = create(GridBuildSystem.class);
    }

    // Accessible \\

    public GridInstance buildGrid(
            EntityInstance focalEntity,
            WindowInstance windowInstance,
            FBOInstance renderTargetFbo) {
        return gridBuildSystem.buildGrid(focalEntity, windowInstance, renderTargetFbo);
    }

    public void rebuildGrid(GridInstance gridInstance) {
        gridBuildSystem.rebuildGrid(gridInstance);
    }
}
