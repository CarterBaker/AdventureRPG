package editor.runtime.itemeditor;

import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.itemeditor.frame.ItemEditorFrameSystem;
import editor.runtime.itemeditor.menu.ItemEditorMenuSystem;
import editor.runtime.itemeditor.render.ItemEditorRenderSystem;
import editor.runtime.itemeditor.tool.ItemEditorToolSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import editor.runtime.viewcamera.ViewCameraProvider;
import editor.runtime.viewcamera.ViewCameraSubject;
import editor.runtime.viewcamera.ViewCameraSystem;
import engine.root.ContextPackage;

public class ItemEditorContext extends ContextPackage implements ToolCanvasProvider, ViewCameraProvider {

    /*
     * Editor tab for building items from sub-voxel cubes. Reads input, orbits
     * the camera, casts the pointer, draws the active item, and shows the
     * toolbar and parts panel — all state and cube logic live in the editor and
     * engine. The tab is a tool canvas, the item, for as long as it is open.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private ItemEditorFrameSystem itemEditorFrameSystem;
    private ViewCameraSystem viewCameraSystem;
    private ItemEditorToolSystem itemEditorToolSystem;
    private ItemEditorRenderSystem itemEditorRenderSystem;
    private ItemEditorMenuSystem itemEditorMenuSystem;
    private ToolCanvasSystem toolCanvasSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.itemEditorFrameSystem = create(ItemEditorFrameSystem.class);
        this.viewCameraSystem = create(ViewCameraSystem.class);
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

    // Camera \\

    @Override
    public ViewCameraSubject getViewCameraSubject() {
        return itemEditorFrameSystem;
    }
}
