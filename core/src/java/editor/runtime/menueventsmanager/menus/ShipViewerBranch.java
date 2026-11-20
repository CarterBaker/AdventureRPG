package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.runtime.shipviewer.ShipViewerContext;
import engine.root.BranchPackage;

public class ShipViewerBranch extends BranchPackage {

    /*
     * Menu event handlers for the Ship Viewer. Each action targets the viewer
     * paired with the window its button was clicked in; a deck button carries
     * the step it takes.
     */

    // Decks \\

    public void stepDeck(String step, WindowInstance window) {
        if (window.getContext() instanceof ShipViewerContext shipViewerContext)
            shipViewerContext.stepDeck(Integer.parseInt(step));
    }
}
