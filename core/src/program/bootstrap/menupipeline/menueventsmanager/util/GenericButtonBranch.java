package program.bootstrap.menupipeline.menueventsmanager.util;

import program.core.engine.BranchPackage;

public class GenericButtonBranch extends BranchPackage {

    /*
     * Handles button actions that are not tied to any specific menu.
     * Reusable across any screen that needs generic engine-level actions.
     */

    public void quitGame() {
        internal.closeGame();
    }
}