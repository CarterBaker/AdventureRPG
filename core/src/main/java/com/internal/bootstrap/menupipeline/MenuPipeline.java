package com.internal.bootstrap.menupipeline;

import com.internal.bootstrap.menupipeline.buttoneventsmanager.ButtonEventsManager;
import com.internal.bootstrap.menupipeline.elementsystem.ElementSystem;
import com.internal.bootstrap.menupipeline.fontmanager.FontManager;
import com.internal.bootstrap.menupipeline.menumanager.MenuManager;
import com.internal.core.engine.PipelinePackage;

public class MenuPipeline extends PipelinePackage {

    // Menu Pipeline \\

    @Override
    protected void create() {
        create(ButtonEventsManager.class);
        create(ElementSystem.class);
        create(FontManager.class);
        create(MenuManager.class);
    }
}