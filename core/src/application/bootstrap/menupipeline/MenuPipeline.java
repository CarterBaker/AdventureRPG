package application.bootstrap.menupipeline;

import application.bootstrap.menupipeline.fontmanager.FontManager;
import application.bootstrap.menupipeline.menueventsmanager.MenuEventsManager;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import engine.root.PipelinePackage;

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
        create(MenuManager.class);
    }
}