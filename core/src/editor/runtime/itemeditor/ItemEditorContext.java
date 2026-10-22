package editor.runtime.itemeditor;

import editor.runtime.itemeditor.camera.ItemEditorCameraSystem;
import editor.runtime.itemeditor.menu.ItemEditorMenuSystem;
import editor.runtime.itemeditor.render.ItemEditorRenderSystem;
import editor.runtime.itemeditor.tool.ItemEditorToolSystem;
import editor.runtime.input.EditorInputSystem;
import engine.root.ContextPackage;

public class ItemEditorContext extends ContextPackage {

    /*
     * Editor tab for building items from sub-voxel cubes. Reads input, orbits
     * the camera, casts the pointer, draws the active item, and shows the
     * toolbar and parts panel — all state and cube logic live in the editor and
     * engine.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private ItemEditorCameraSystem itemEditorCameraSystem;
    private ItemEditorToolSystem itemEditorToolSystem;
    private ItemEditorRenderSystem itemEditorRenderSystem;
    private ItemEditorMenuSystem itemEditorMenuSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.itemEditorCameraSystem = create(ItemEditorCameraSystem.class);
        this.itemEditorToolSystem = create(ItemEditorToolSystem.class);
        this.itemEditorRenderSystem = create(ItemEditorRenderSystem.class);
        this.itemEditorMenuSystem = create(ItemEditorMenuSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
