package application.runtime.menueventsmanager;

import application.runtime.menueventsmanager.menus.InventoryBranch;
import application.runtime.menueventsmanager.menus.MainMenuBranch;
import application.runtime.menueventsmanager.util.GenericButtonBranch;
import engine.root.ManagerPackage;

public class MenuEventsManager extends ManagerPackage {

    @Override
    protected void create() {
        create(MainMenuBranch.class);
        create(InventoryBranch.class);
        create(GenericButtonBranch.class);
    }
}
