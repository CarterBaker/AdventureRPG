package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.roadplannerpipeline.roadplannermanager.RoadPlannerManager;
import engine.root.BranchPackage;

public class RoadPlannerBranch extends BranchPackage {

    /*
     * Menu event handlers for the Road Planner. Grid cells carry their index
     * and Palette rows a road type's or a biome's name; New and Duplicate
     * first open the road type drawn in the Hierarchy, so the Info Panel's
     * own New and Duplicate make the type, named through the same dialogs;
     * Save and Revert act on its file. The toolbar opens the Biome Viewer
     * through the Biome Planner's own handler.
     */

    // Internal
    private RoadPlannerManager roadPlannerManager;
    private InfoPanelBranch infoPanelBranch;

    // Base \\

    @Override
    protected void get() {
        this.roadPlannerManager = get(RoadPlannerManager.class);
        this.infoPanelBranch = get(InfoPanelBranch.class);
    }

    // Grid \\

    public void pressCell(String cell) {
        roadPlannerManager.pressCell(Integer.parseInt(cell));
    }

    // Selection \\

    public void selectRow(String key) {
        roadPlannerManager.choosePaletteRow(key);
    }

    // File \\

    public void save() {
        roadPlannerManager.save();
    }

    public void revert() {
        roadPlannerManager.revert();
    }

    public void newRoad(WindowInstance window) {
        if (roadPlannerManager.focusRoad())
            infoPanelBranch.newEntry(window);
    }

    public void duplicateRoad(WindowInstance window) {
        if (roadPlannerManager.focusRoad())
            infoPanelBranch.copySelection(window);
    }
}
