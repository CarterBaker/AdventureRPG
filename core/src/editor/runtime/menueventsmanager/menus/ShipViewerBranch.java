package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.runtime.shipviewer.ShipViewerContext;
import engine.root.BranchPackage;

public class ShipViewerBranch extends BranchPackage {

    /*
     * Menu event handlers for the Ship Viewer. Each action targets the viewer
     * paired with the window its button was clicked in; a step button
     * carries how many places along the vehicles it moves.
     */

    // Step \\

    public void step(String step, WindowInstance window) {
        if (window.getContext() instanceof ShipViewerContext shipViewerContext)
            shipViewerContext.step(Integer.parseInt(step));
    }
}
