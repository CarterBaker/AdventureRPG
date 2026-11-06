package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.settlementplannerpipeline.settlementplannermanager.SettlementPlannerManager;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.worldeditor.WorldEditorContext;
import editor.runtime.worldeditor.WorldEditorLayerProvider;
import engine.root.BranchPackage;

public class WorldEditorBranch extends BranchPackage {

    /*
     * Menu event handlers for the World Editor's and Height Editor's toolbars
     * and keys and the Biome Palette tab. Saving and reloading work on both
     * of the world's layers together; undo and redo work on the layer of the
     * tab they were pressed in, and the player marker toggle on the editor
     * its button was clicked in. Biome rows carry their biome's name.
     * Choosing and turning what is placed works on the Settlement Planner's
     * shared design. The tools themselves are chosen through ToolBranch, as
     * every editor's are.
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

    public void undo(WindowInstance window) {
        if (window.getContext() instanceof WorldEditorLayerProvider layerProvider)
            worldEditorManager.undo(layerProvider.getWorldEditorLayer());
    }

    public void redo(WindowInstance window) {
        if (window.getContext() instanceof WorldEditorLayerProvider layerProvider)
            worldEditorManager.redo(layerProvider.getWorldEditorLayer());
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
}
