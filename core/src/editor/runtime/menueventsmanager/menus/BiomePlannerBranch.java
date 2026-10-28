package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.biomeplannerpipeline.biomeplannermanager.BiomePlannerManager;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import engine.root.BranchPackage;

public class BiomePlannerBranch extends BranchPackage {

    /*
     * Menu event handlers for the Biome Planner. Cells carry their index in
     * the grid. New and Duplicate first open the selected biome in the
     * Hierarchy, so the Info Panel's own New and Duplicate make the biome,
     * named through the same dialogs; Open Viewer opens a Biome Viewer beside
     * the planner.
     */

    // Internal
    private BiomePlannerManager biomePlannerManager;
    private InfoPanelBranch infoPanelBranch;
    private TabManager tabManager;

    // Base \\

    @Override
    protected void get() {
        this.biomePlannerManager = get(BiomePlannerManager.class);
        this.infoPanelBranch = get(InfoPanelBranch.class);
        this.tabManager = get(TabManager.class);
    }

    // Grid \\

    public void paintCell(String cell) {
        biomePlannerManager.paintCell(Integer.parseInt(cell));
    }

    public void resetGrid() {
        biomePlannerManager.resetGrid();
    }

    // Biomes \\

    public void newBiome(WindowInstance window) {
        if (biomePlannerManager.focusCentre())
            infoPanelBranch.newEntry(window);
    }

    public void duplicateBiome(WindowInstance window) {
        if (biomePlannerManager.focusCentre())
            infoPanelBranch.copySelection(window);
    }

    // Viewer \\

    public void openViewer(WindowInstance window) {
        tabManager.openBiomeViewer(window.getGLWindow());
    }
}
