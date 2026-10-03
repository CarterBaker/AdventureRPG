package editor.worldeditor;

import editor.runtime.EditorInputSystem;
import editor.worldeditor.menu.WorldEditorMenuSystem;
import editor.worldeditor.render.WorldEditorRenderSystem;
import editor.worldeditor.tool.WorldEditorToolSystem;
import editor.worldeditor.view.WorldEditorViewSystem;
import engine.root.ContextPackage;

public class WorldEditorContext extends ContextPackage {

    /*
     * Editor tab for painting the world image, one pixel per biome cell of
     * the world. Reads input, pans and zooms the view, turns the pointer into
     * image pixels for the active tool, draws the image with the brush, and
     * shows the toolbar and biome palette, with every player's marker when
     * shown — the image, its history, the
     * palette and every live update live in the editor and engine.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private WorldEditorViewSystem worldEditorViewSystem;
    private WorldEditorToolSystem worldEditorToolSystem;
    private WorldEditorRenderSystem worldEditorRenderSystem;
    private WorldEditorMenuSystem worldEditorMenuSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.worldEditorViewSystem = create(WorldEditorViewSystem.class);
        this.worldEditorToolSystem = create(WorldEditorToolSystem.class);
        this.worldEditorRenderSystem = create(WorldEditorRenderSystem.class);
        this.worldEditorMenuSystem = create(WorldEditorMenuSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Management \\

    public void togglePlayers() {
        worldEditorRenderSystem.togglePlayers();
    }
}
