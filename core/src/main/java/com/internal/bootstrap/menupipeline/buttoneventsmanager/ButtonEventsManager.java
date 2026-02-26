package com.internal.bootstrap.menupipeline.buttoneventsmanager;

import com.internal.bootstrap.menupipeline.buttoneventsmanager.menus.MainMenuBranch;
import com.internal.core.engine.ManagerPackage;

public class ButtonEventsManager extends ManagerPackage {

    // Internal \\

    @Override
    protected void create() {

        // Internal
        create(MainMenuBranch.class);
    }
}
