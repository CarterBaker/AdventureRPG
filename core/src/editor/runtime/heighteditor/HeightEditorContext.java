package editor.runtime.heighteditor;

import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.bootstrap.worldeditorpipeline.util.WorldEditorLayer;
import editor.runtime.heighteditor.menu.HeightEditorMenuSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.placementicon.PlacementIconSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import editor.runtime.worldeditor.WorldEditorLayerProvider;
import editor.runtime.worldeditor.render.WorldEditorRenderSystem;
import editor.runtime.worldeditor.tool.WorldEditorToolSystem;
import editor.runtime.worldeditor.view.WorldEditorViewSystem;
import engine.root.ContextPackage;

public class HeightEditorContext extends ContextPackage implements ToolCanvasProvider, WorldEditorLayerProvider {

    /*
     * Editor tab for the world's height layer, one image pixel per biome cell
     * of the world, drawn as its black and white heightmap over the biomes
     * beneath it. Its only task is shaping the ground: the Raise and Lower
     * tools build it up and wear it down a little more with every stamp, a
     * soft brush fading at its edge and a hard one keeping it, Smooth evens
     * it out, Flatten levels it to where a stroke begins and Erase returns it
     * to the ground as generated. It works the World Editor's way, through
     * the same view, tool and render systems, on the shared images, their
     * history and every live update; the tools and brushes are chosen in
     * their own tabs. The tab is the height canvas for as long as it is open.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private WorldEditorViewSystem worldEditorViewSystem;
    private WorldEditorToolSystem worldEditorToolSystem;
    private PlacementIconSystem placementIconSystem;
    private WorldEditorRenderSystem worldEditorRenderSystem;
    private HeightEditorMenuSystem heightEditorMenuSystem;
    private ToolCanvasSystem toolCanvasSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.worldEditorViewSystem = create(WorldEditorViewSystem.class);
        this.worldEditorToolSystem = create(WorldEditorToolSystem.class);
        this.placementIconSystem = create(PlacementIconSystem.class);
        this.worldEditorRenderSystem = create(WorldEditorRenderSystem.class);
        this.heightEditorMenuSystem = create(HeightEditorMenuSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Tools \\

    @Override
    public ToolCanvas getToolCanvas() {
        return WorldEditorLayer.HEIGHTS.getToolCanvas();
    }

    // Layer \\

    @Override
    public WorldEditorLayer getWorldEditorLayer() {
        return WorldEditorLayer.HEIGHTS;
    }
}
