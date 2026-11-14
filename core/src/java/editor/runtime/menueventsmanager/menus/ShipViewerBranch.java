package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.runtime.shipviewer.ShipViewerContext;
import engine.root.BranchPackage;

public class ShipViewerBranch extends BranchPackage {

    /*
     * Menu event handlers for the Ship Viewer. Each action targets the viewer
     * paired with the window its button was clicked in; a gizmo axis carries
     * the axis it looks back along, a deck button the step it takes.
     */

    // View \\

    public void view(String axisName, WindowInstance window) {
        if (window.getContext() instanceof ShipViewerContext shipViewerContext)
            shipViewerContext.view(axisName);
    }

    public void centre(WindowInstance window) {
        if (window.getContext() instanceof ShipViewerContext shipViewerContext)
            shipViewerContext.centre();
    }

    // Decks \\

    public void stepDeck(String step, WindowInstance window) {
        if (window.getContext() instanceof ShipViewerContext shipViewerContext)
            shipViewerContext.stepDeck(Integer.parseInt(step));
    }

}
