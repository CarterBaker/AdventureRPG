package com.internal.bootstrap.menupipeline.buttoneventsmanager.util;

import com.internal.core.engine.BranchPackage;

public class GenericButtonBranch extends BranchPackage {

    public void quitGame() {
        internal.closeGame();
    }
}