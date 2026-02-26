package com.internal.runtime.debug;

import com.internal.bootstrap.menupipeline.menumanager.MenuManager;
import com.internal.core.engine.PipelinePackage;

public class DebugPipeline extends PipelinePackage {

    // Lighting
    public Sky sky;

    // Internal
    private MenuManager menuManager;

    // Base \\

    @Override
    protected void create() {
        this.sky = create(Sky.class);
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
    }

    @Override
    protected void awake() {
        menuManager.openMenu("MainMenu");
    }
}