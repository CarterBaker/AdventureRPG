package com.internal.runtime.menu;

import com.internal.bootstrap.menupipeline.menueventsmanager.menus.MainMenuBranch;
import com.internal.core.engine.SystemPackage;

public class MenuSystem extends SystemPackage {

    /*
     * Triggers the main menu on runtime startup.
     * Owned by RuntimePipeline. Delegates entirely to MainMenuBranch.
     */

    // Internal
    private MainMenuBranch mainMenuBranch;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.mainMenuBranch = get(MainMenuBranch.class);
    }

    @Override
    protected void awake() {
        mainMenuBranch.openMenu();
    }
}