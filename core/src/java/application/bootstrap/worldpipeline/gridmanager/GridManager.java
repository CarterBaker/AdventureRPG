package application.bootstrap.worldpipeline.gridmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.grid.GridRangeStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.ManagerPackage;

public class GridManager extends ManagerPackage {

    /*
     * Pure factory for GridInstance construction, in-place slot rebuilds and
     * the push of each grid's render settings. Delegates all build logic to
     * GridBuildSystem. Holds no grid state — callers own what they build.
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
            FBOInstance renderTargetFbo,
            GridRangeStruct range) {
        return gridBuildSystem.buildGrid(focalEntity, windowInstance, renderTargetFbo, range);
    }

    public void rebuildGrid(GridInstance gridInstance) {
        gridBuildSystem.rebuildGrid(gridInstance);
    }

    public void pushRenderSettings(GridInstance gridInstance) {
        gridBuildSystem.pushRenderSettings(gridInstance);
    }
}
