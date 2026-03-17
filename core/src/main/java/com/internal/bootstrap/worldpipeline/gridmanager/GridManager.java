package com.internal.bootstrap.worldpipeline.gridmanager;

import com.internal.bootstrap.entitypipeline.entity.EntityInstance;
import com.internal.core.engine.ManagerPackage;

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

    public GridInstance buildGrid(EntityInstance focalEntity) {
        return gridBuildSystem.buildGrid(focalEntity);
    }
}