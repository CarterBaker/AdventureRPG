package editor.runtime.menueventsmanager.menus;

import editor.bootstrap.worldeditorpipeline.util.WorldEditorTool;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import engine.root.BranchPackage;

public class WorldEditorBranch extends BranchPackage {

    /*
     * Menu event handlers for the World Editor toolbar and biome palette.
     * Every action works on the shared world image; palette rows carry their
     * biome's name.
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

    public void shrinkBrush() {
        worldEditorManager.resizeBrush(-1);
    }

    public void growBrush() {
        worldEditorManager.resizeBrush(1);
    }

    // Palette \\

    public void selectBiome(String biomeName) {
        worldEditorManager.selectBiome(biomeName);
    }
}
