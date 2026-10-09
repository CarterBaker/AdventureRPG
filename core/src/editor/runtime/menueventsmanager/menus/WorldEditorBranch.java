package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.worldeditorpipeline.util.WorldEditorTool;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.worldeditor.WorldEditorContext;
import engine.root.BranchPackage;

public class WorldEditorBranch extends BranchPackage {

    /*
     * Menu event handlers for the World Editor toolbar and biome palette.
     * Every action works on the shared world image and its hand placements,
     * except the player marker toggle, which targets the editor its button
     * was clicked in; palette rows carry their biome's name.
     */

    // Internal
    private WorldEditorManager worldEditorManager;

    // Base \\

    @Override
    protected void get() {
        this.worldEditorManager = get(WorldEditorManager.class);
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

    // Tools \\

    public void selectBrushTool() {
        worldEditorManager.setTool(WorldEditorTool.BRUSH);
    }

    public void selectFillTool() {
        worldEditorManager.setTool(WorldEditorTool.FILL);
    }

    public void selectPickTool() {
        worldEditorManager.setTool(WorldEditorTool.PICK);
    }

    public void selectPlaceTool() {
        worldEditorManager.setTool(WorldEditorTool.PLACE);
    }

    public void selectClearTool() {
        worldEditorManager.setTool(WorldEditorTool.CLEAR);
    }

    public void previousPlaceable() {
        worldEditorManager.cyclePlaceable(-1);
    }

    public void nextPlaceable() {
        worldEditorManager.cyclePlaceable(1);
    }

    public void turnPlacement() {
        worldEditorManager.turnPlacement();
    }

    public void shrinkBrush() {
        worldEditorManager.resizeBrush(-1);
    }

    public void growBrush() {
        worldEditorManager.resizeBrush(1);
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
