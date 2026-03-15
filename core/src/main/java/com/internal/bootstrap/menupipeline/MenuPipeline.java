package com.internal.bootstrap.menupipeline;

import com.internal.bootstrap.menupipeline.fontmanager.FontManager;
import com.internal.bootstrap.menupipeline.menueventsmanager.MenuEventsManager;
import com.internal.bootstrap.menupipeline.menumanager.MenuManager;
import com.internal.bootstrap.menupipeline.raycastsystem.RaycastSystem;
import com.internal.core.engine.PipelinePackage;

public class MenuPipeline extends PipelinePackage {

    /*
     * Registers all menu pipeline managers in dependency order. FontManager
     * is registered before MenuManager since menus depend on fonts being
     * available at load time. RaycastSystem is a peer system — created here
     * so MenuManager can get() it rather than own it.
     */

    @Override
    protected void create() {
        create(MenuEventsManager.class);
        create(FontManager.class);
        create(RaycastSystem.class);
        create(MenuManager.class);
    }
}