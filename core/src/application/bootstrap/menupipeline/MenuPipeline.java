package application.bootstrap.menupipeline;

import application.bootstrap.menupipeline.fontmanager.FontManager;
import application.bootstrap.menupipeline.fontmanager.FontRenderSystem;
import application.bootstrap.menupipeline.hierarchymanager.HierarchyManager;
import application.bootstrap.menupipeline.menumanager.ElementHitSystem;
import application.bootstrap.menupipeline.menumanager.ElementSystem;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.menumanager.MenuRenderSystem;
import engine.root.PipelinePackage;

public class MenuPipeline extends PipelinePackage {

    /*
     * Registers the menu managers in dependency order: fonts before menus,
     * since menus need fonts at load time, and HierarchyManager after
     * MenuManager so panels lay out after the frame's menus have rendered and
     * dispatched clicks.
     */

    @Override
    protected void create() {
        create(FontManager.class);
        create(ElementSystem.class);
        create(ElementHitSystem.class);
        create(MenuRenderSystem.class);
        create(FontRenderSystem.class);
        create(MenuManager.class);
        create(HierarchyManager.class);
    }
}