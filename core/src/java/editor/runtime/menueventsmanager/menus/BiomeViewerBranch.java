package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.runtime.biomeviewer.BiomeViewerContext;
import engine.root.BranchPackage;

public class BiomeViewerBranch extends BranchPackage {

    /*
     * Menu event handlers for the Biome Viewer. Each action targets the
     * viewer paired with the window its button was clicked in.
     */

    // Pause \\

    public void resume(WindowInstance window) {
        if (window.getContext() instanceof BiomeViewerContext biomeViewerContext)
            biomeViewerContext.resume();
    }
}
