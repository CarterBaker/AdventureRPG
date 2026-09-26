package engine.editor.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.console.ConsoleContext;
import engine.root.BranchPackage;

public class ConsoleBranch extends BranchPackage {

    /*
     * Menu event handlers for the console tab. Each action targets the
     * console paired with the window its button was clicked in.
     */

    // Console Operations \\

    public void clear(WindowInstance window) {
        if (window.getContext() instanceof ConsoleContext consoleContext)
            consoleContext.clear();
    }
}
