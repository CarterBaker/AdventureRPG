package application.bootstrap.menupipeline.menueventsmanager;

import application.bootstrap.menupipeline.menueventsmanager.util.GenericButtonBranch;
import engine.root.ManagerPackage;

public class MenuEventsManager extends ManagerPackage {

    @Override
    protected void create() {
        create(GenericButtonBranch.class);
    }
}
