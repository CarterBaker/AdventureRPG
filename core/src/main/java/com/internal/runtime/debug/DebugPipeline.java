package com.internal.runtime.debug;

import com.internal.bootstrap.menupipeline.buttoneventsmanager.menus.MainMenuBranch;
import com.internal.core.engine.PipelinePackage;

public class DebugPipeline extends PipelinePackage {

    // Internal
    private Sky sky;
    private MainMenuBranch mainMenuBranch;

    // Base \\

    @Override
    protected void create() {
        this.sky = create(Sky.class);
    }

    @Override
    protected void get() {
        this.mainMenuBranch = get(MainMenuBranch.class);
    }

    @Override
    protected void awake() {
        mainMenuBranch.openMenu();
    }
}