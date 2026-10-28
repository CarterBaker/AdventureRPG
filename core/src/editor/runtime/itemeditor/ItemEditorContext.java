package editor.runtime.itemeditor;

import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.itemeditor.camera.ItemEditorCameraSystem;
import editor.runtime.itemeditor.menu.ItemEditorMenuSystem;
import editor.runtime.itemeditor.render.ItemEditorRenderSystem;
import editor.runtime.itemeditor.tool.ItemEditorToolSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;

public class ItemEditorContext extends ContextPackage implements ToolCanvasProvider {

    /*
     * Editor tab for building items from sub-voxel cubes. Reads input, orbits
     * the camera, casts the pointer, draws the active item, and shows the
     * toolbar and parts panel — all state and cube logic live in the editor and
     * engine. The tab is a tool canvas, the item, for as long as it is open.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private ItemEditorCameraSystem itemEditorCameraSystem;
    private ItemEditorToolSystem itemEditorToolSystem;
    private ItemEditorRenderSystem itemEditorRenderSystem;
    private ItemEditorMenuSystem itemEditorMenuSystem;
    private ToolCanvasSystem toolCanvasSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.itemEditorCameraSystem = create(ItemEditorCameraSystem.class);
        this.itemEditorToolSystem = create(ItemEditorToolSystem.class);
        this.itemEditorRenderSystem = create(ItemEditorRenderSystem.class);
        this.itemEditorMenuSystem = create(ItemEditorMenuSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Tools \\

    @Override
    public ToolCanvas getToolCanvas() {
        return ToolCanvas.ITEM;
    }
}
