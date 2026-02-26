package com.internal.bootstrap.menupipeline.buttoneventsmanager.menus;

import com.internal.core.engine.BranchPackage;

public class MainMenuBranch extends BranchPackage {

    public void quitGame() {
        internal.closeGame();
    }
}