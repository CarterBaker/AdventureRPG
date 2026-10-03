package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.worldmap.WorldMapContext;
import engine.root.BranchPackage;

public class WorldMapBranch extends BranchPackage {

    /*
     * Menu event handlers for the world map tab. Each action targets the map
     * paired with the window its button was clicked in.
     */

    // View Operations \\

    public void toggleFollow(WindowInstance window) {
        if (window.getContext() instanceof WorldMapContext worldMapContext)
            worldMapContext.toggleFollow();
    }

    public void toggleDayNight(WindowInstance window) {
        if (window.getContext() instanceof WorldMapContext worldMapContext)
            worldMapContext.toggleDayNight();
    }

    public void toggleWeather(WindowInstance window) {
        if (window.getContext() instanceof WorldMapContext worldMapContext)
            worldMapContext.toggleWeather();
    }
}
