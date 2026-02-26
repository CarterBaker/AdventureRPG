package com.internal.bootstrap.menupipeline;

import com.internal.bootstrap.menupipeline.buttoneventsmanager.ButtonEventsManager;
import com.internal.bootstrap.menupipeline.menumanager.MenuManager;
import com.internal.core.engine.PipelinePackage;

public class MenuPipeline extends PipelinePackage {

    // Menu Pipeline \\

    @Override
    protected void create() {

        // Menu Pipeline
        create(ButtonEventsManager.class);
        create(MenuManager.class);
    }
}
