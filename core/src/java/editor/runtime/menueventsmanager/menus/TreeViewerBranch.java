package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.runtime.treeviewer.TreeViewerContext;
import engine.root.BranchPackage;

public class TreeViewerBranch extends BranchPackage {

    /*
     * Menu event handlers for the Tree Viewer. Each action targets the viewer
     * paired with the window its button was clicked in; a frame button
     * carries the tier it frames, or FRAME_ALL for the whole row.
     */

    // Frame \\

    public void frame(String tier, WindowInstance window) {
        if (window.getContext() instanceof TreeViewerContext treeViewerContext)
            treeViewerContext.frame(Integer.parseInt(tier));
    }
}
