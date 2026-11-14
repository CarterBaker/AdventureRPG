package application.runtime.menueventsmanager.util;

import engine.root.BranchPackage;

public class GenericButtonBranch extends BranchPackage {

    /*
     * Handles button actions that are not tied to any specific menu.
     * Reusable across any screen that needs generic engine-level actions.
     * Quitting only closes the application from the context that owns the
     * main window, so an editor preview's Quit button does nothing.
     */

    public void quitGame() {
        internal.close(context);
    }
}
