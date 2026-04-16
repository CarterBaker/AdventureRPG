package application.bootstrap.menupipeline;

import application.bootstrap.menupipeline.cursorlocksystem.CursorLockSystem;
import application.bootstrap.menupipeline.elementhitsystem.ElementHitSystem;
import application.bootstrap.menupipeline.elementsystem.ElementSystem;
import application.bootstrap.menupipeline.fontmanager.FontManager;
import application.bootstrap.menupipeline.menueventsmanager.MenuEventsManager;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.menurendersystem.MenuRenderSystem;
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
        create(FontManager.class);
        create(ElementSystem.class);
        create(ElementHitSystem.class);
        create(MenuEventsManager.class);
        create(MenuRenderSystem.class);
        create(MenuManager.class);
        create(CursorLockSystem.class);
    }
}