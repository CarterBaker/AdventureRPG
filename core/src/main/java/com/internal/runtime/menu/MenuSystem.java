package com.internal.runtime.menu;

import com.internal.bootstrap.menupipeline.menueventsmanager.menus.MainMenuBranch;
import com.internal.bootstrap.renderpipeline.windowmanager.WindowManager;
import com.internal.core.engine.SystemPackage;

public class MenuSystem extends SystemPackage {

    /*
     * Opens the main menu at runtime startup. Passes the main window so the
     * menu is bound to the correct window from the start.
     */

    // Internal
    private MainMenuBranch mainMenuBranch;
    private WindowManager windowManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.mainMenuBranch = get(MainMenuBranch.class);
        this.windowManager = get(WindowManager.class);
    }

    @Override
    protected void awake() {
        mainMenuBranch.openMenu(windowManager.getMainWindow());
    }
}