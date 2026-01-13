package com.internal.bootstrap.worldpipeline.gridmanager;

import com.internal.core.engine.ManagerPackage;

public class GridManager extends ManagerPackage {

    // Internal
    private GridBuildSystem gridBuildSystem;
    private GridInstance grid;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.gridBuildSystem = create(GridBuildSystem.class);
    }

    @Override
    protected void awake() {
        this.grid = gridBuildSystem.buildGrid();
    }

    // Accessible \\

    public GridInstance getGrid() {
        return grid;
    }
}
