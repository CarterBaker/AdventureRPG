package com.internal.runtime.debug;

import com.internal.bootstrap.menupipeline.buttoneventsmanager.menus.MainMenuBranch;
import com.internal.core.engine.PipelinePackage;

public class DebugPipeline extends PipelinePackage {

    // Lighting
    public Sky sky;

    // Internal
    private MainMenuBranch mainMenu;

    // Base \\

    @Override
    protected void create() {
        this.sky = create(Sky.class);
    }

    @Override
    protected void get() {
        this.mainMenu = get(MainMenuBranch.class);
    }

    @Override
    protected void awake() {
        mainMenu.openMenu();
    }
}