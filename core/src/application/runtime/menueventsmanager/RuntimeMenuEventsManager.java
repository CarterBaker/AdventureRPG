package application.runtime.menueventsmanager;

import application.bootstrap.menupipeline.menueventsmanager.menus.InventoryBranch;
import application.bootstrap.menupipeline.menueventsmanager.menus.MainMenuBranch;
import engine.root.ManagerPackage;

public class RuntimeMenuEventsManager extends ManagerPackage {

    @Override
    protected void create() {
        create(MainMenuBranch.class);
        create(InventoryBranch.class);
    }
}
