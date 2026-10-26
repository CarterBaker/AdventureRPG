package editor.runtime.worldeditor;

import editor.runtime.input.EditorInputSystem;
import editor.runtime.placementicon.PlacementIconSystem;
import editor.runtime.worldeditor.menu.WorldEditorMenuSystem;
import editor.runtime.worldeditor.render.WorldEditorRenderSystem;
import editor.runtime.worldeditor.tool.WorldEditorToolSystem;
import editor.runtime.worldeditor.view.WorldEditorViewSystem;
import engine.root.ContextPackage;

public class WorldEditorContext extends ContextPackage {

    /*
     * Editor tab for painting the world image, one pixel per biome cell of
     * the world. Reads input, pans and zooms the view, turns the pointer into
     * image pixels for the active tool, draws the image with the brush and
     * an icon for every hand placement, and shows the toolbar and biome
     * palette, with every player's marker when shown — the image, its
     * history, the palette, the Settlement Planner's design it places and
     * every live update live in the editor and engine.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private WorldEditorViewSystem worldEditorViewSystem;
    private WorldEditorToolSystem worldEditorToolSystem;
    private PlacementIconSystem placementIconSystem;
    private WorldEditorRenderSystem worldEditorRenderSystem;
    private WorldEditorMenuSystem worldEditorMenuSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.worldEditorViewSystem = create(WorldEditorViewSystem.class);
        this.worldEditorToolSystem = create(WorldEditorToolSystem.class);
        this.placementIconSystem = create(PlacementIconSystem.class);
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
