package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.settlementplannerpipeline.settlementplannermanager.SettlementPlannerManager;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.worldeditor.WorldEditorContext;
import engine.root.BranchPackage;

public class WorldEditorBranch extends BranchPackage {

    /*
     * Menu event handlers for the World Editor toolbar and keys and the
     * Palette tab. Every action works on the shared world images and their
     * hand placements, except the player marker toggle, which targets the
     * editor its button was clicked in; biome rows carry their biome's name
     * and height rows their level. Choosing and turning what is placed works
     * on the Settlement Planner's shared design. The tools themselves are
     * chosen through ToolBranch, as every editor's are.
     */

    // Internal
    private WorldEditorManager worldEditorManager;
    private SettlementPlannerManager settlementPlannerManager;

    // Base \\

    @Override
    protected void get() {
        this.worldEditorManager = get(WorldEditorManager.class);
        this.settlementPlannerManager = get(SettlementPlannerManager.class);
    }

    // Image \\

    public void saveImage() {
        worldEditorManager.saveWorldImage();
    }

    public void reloadImage() {
        worldEditorManager.reloadWorldImage();
    }

    public void undo() {
        worldEditorManager.undo();
    }

    public void redo() {
        worldEditorManager.redo();
    }

    public void toggleLayer() {
        worldEditorManager.toggleLayer();
    }

    // Placement \\

    public void deleteSelection() {
        worldEditorManager.deleteSelection();
    }

    public void previousPlaceable() {
        settlementPlannerManager.cycleEntry(-1);
    }

    public void nextPlaceable() {
        settlementPlannerManager.cycleEntry(1);
    }

    public void turnPlacement() {
        settlementPlannerManager.turn();
    }

    public void togglePlayers(WindowInstance window) {
        if (window.getContext() instanceof WorldEditorContext worldEditorContext)
            worldEditorContext.togglePlayers();
    }

    // Palette \\

    public void selectBiome(String biomeName) {
        worldEditorManager.selectBiome(biomeName);
    }

    public void selectHeight(String level) {
        worldEditorManager.selectHeightLevel(Integer.parseInt(level));
    }
}
