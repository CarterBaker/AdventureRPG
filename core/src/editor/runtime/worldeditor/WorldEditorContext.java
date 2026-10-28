package editor.runtime.worldeditor;

import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.placementicon.PlacementIconSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import editor.runtime.worldeditor.menu.WorldEditorMenuSystem;
import editor.runtime.worldeditor.render.WorldEditorRenderSystem;
import editor.runtime.worldeditor.tool.WorldEditorToolSystem;
import editor.runtime.worldeditor.view.WorldEditorViewSystem;
import engine.root.ContextPackage;

public class WorldEditorContext extends ContextPackage implements ToolCanvasProvider {

    /*
     * Editor tab for the world's canvas, one image pixel per biome cell of
     * the world. Reads input, pans and zooms the view, turns the pointer into
     * image pixels for the tool chosen for what it works on, painting the
     * world's layers or placing settlements and structures, draws the image
     * with the brush or the design's footprint and an icon for every hand
     * placement, and shows the toolbar and status line, with every player's
     * marker when shown. A design dragged from the Settlement Planner is
     * stood where it is dropped. The tools, brushes, biome palette and design
     * are chosen in their own tabs; the image, its history and every live
     * update live in the editor and engine. The tab is a tool canvas for as
     * long as it is open, working on whatever the World Editor works on.
     */

    // Internal
    private WorldEditorManager worldEditorManager;
    private EditorInputSystem editorInputSystem;
    private WorldEditorViewSystem worldEditorViewSystem;
    private WorldEditorToolSystem worldEditorToolSystem;
    private PlacementIconSystem placementIconSystem;
    private WorldEditorRenderSystem worldEditorRenderSystem;
    private WorldEditorMenuSystem worldEditorMenuSystem;
    private ToolCanvasSystem toolCanvasSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.worldEditorViewSystem = create(WorldEditorViewSystem.class);
        this.worldEditorToolSystem = create(WorldEditorToolSystem.class);
        this.placementIconSystem = create(PlacementIconSystem.class);
        this.worldEditorRenderSystem = create(WorldEditorRenderSystem.class);
        this.worldEditorMenuSystem = create(WorldEditorMenuSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
    }

    @Override
    protected void get() {
        this.worldEditorManager = get(WorldEditorManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Management \\

    public void togglePlayers() {
        worldEditorRenderSystem.togglePlayers();
    }

    public void dropDesign() {
        worldEditorToolSystem.drop();
    }

    // Tools \\

    @Override
    public ToolCanvas getToolCanvas() {
        return worldEditorManager.getToolCanvas();
    }
}
