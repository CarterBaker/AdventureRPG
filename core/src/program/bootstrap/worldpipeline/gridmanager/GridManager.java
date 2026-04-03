package program.bootstrap.worldpipeline.gridmanager;

import program.bootstrap.entitypipeline.entity.EntityInstance;
import program.bootstrap.worldpipeline.grid.GridInstance;
import program.core.engine.ManagerPackage;
import program.core.kernel.window.WindowInstance;

public class GridManager extends ManagerPackage {

    /*
     * Pure factory for GridInstance construction. Delegates all build logic to
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

    public GridInstance buildGrid(EntityInstance focalEntity, WindowInstance windowInstance) {
        return gridBuildSystem.buildGrid(focalEntity, windowInstance);
    }
}