package com.internal.bootstrap.menupipeline.buttoneventsmanager;

import com.internal.bootstrap.menupipeline.buttoneventsmanager.menus.MainMenuBranch;
import com.internal.bootstrap.menupipeline.buttoneventsmanager.util.GenericButtonBranch;
import com.internal.core.engine.ManagerPackage;

public class ButtonEventsManager extends ManagerPackage {

    @Override
    protected void create() {

        // Util
        create(GenericButtonBranch.class);

        // Menus
        create(MainMenuBranch.class);
    }
}
