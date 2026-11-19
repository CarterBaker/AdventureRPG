package editor.runtime.itemeditor.tool;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelHitStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.itemeditorpipeline.itemeditormanager.ItemEditorManager;
import editor.runtime.itemeditor.ItemEditorSetting;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.viewcamera.ViewCameraSystem;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;

public class ItemEditorToolSystem extends SystemPackage {

    /*
     * Casts the pointer into the active item each frame and applies the tool
     * chosen for the item on a press that lands on it, claiming the press
     * from the view camera; a press on nothing is the camera's. Brush and
     * Erase then stroke for as long as the button is held, the ray handed on
     * every frame so the stroke follows the layer it began on, and end when
     * it is let go. Nothing is cast while the pointer is off the viewport or
     * the camera holds a drag. The last hit is kept for the cursor, and a
     * material dropped on the window is laid on what the pointer strikes.
     */

    // Internal
    private ItemEditorManager itemEditorManager;
    private SubVoxelManager subVoxelManager;
    private EditorInputSystem editorInputSystem;
    private ViewCameraSystem viewCameraSystem;

    // Pointer
    private SubVoxelHitStruct hit;
    private Vector3 rayOrigin;
    private Vector3 rayDirection;

    // Stroke
    private boolean stroking;

    // Base \\

    @Override
    protected void create() {

        // Pointer
        this.hit = new SubVoxelHitStruct();
        this.rayOrigin = new Vector3();
        this.rayDirection = new Vector3();
    }

    @Override
    protected void get() {
        this.itemEditorManager = get(ItemEditorManager.class);
        this.subVoxelManager = get(SubVoxelManager.class);
        this.editorInputSystem = get(EditorInputSystem.class);
        this.viewCameraSystem = get(ViewCameraSystem.class);
    }

    // Update \\

    @Override
    protected void update() {

        hit.clear();

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();

        if (stroking) {
            updateStroke(rawInput);
            return;
        }

        if (!editorInputSystem.isPointerActive()
                || !itemEditorManager.hasActiveDocument()
                || viewCameraSystem.isDragging())
            return;

        castPointer(context.getWindow(), rawInput);

        if (!hit.hasHit() || !editorInputSystem.takeClick(ItemEditorSetting.BUTTON_APPLY))
            return;

        itemEditorManager.pressTool(hit);
        this.stroking = itemEditorManager.isStampTool();
    }

    // The held stroke follows the pointer's ray until the button is let go, the press kept from the camera throughout
    private void updateStroke(RawInputHandle rawInput) {

        if (!rawInput.isButtonHeld(ItemEditorSetting.BUTTON_APPLY) || !itemEditorManager.hasActiveDocument()) {
            this.stroking = false;
            itemEditorManager.releaseTool();
            return;
        }

        editorInputSystem.claim(ItemEditorSetting.BUTTON_APPLY);

        if (castPointer(context.getWindow(), rawInput))
            itemEditorManager.dragTool(rayOrigin, rayDirection);
    }

    // Pointer \\

    // The pointer's ray cast into the item, kept in rayOrigin and rayDirection; false while the window has no size
    private boolean castPointer(WindowInstance window, RawInputHandle rawInput) {

        float width = window.getWidth();
        float height = window.getHeight();

        if (width <= 0f || height <= 0f)
            return false;

        float ndcX = rawInput.getMouseX() / width * 2f - 1f;
        float ndcY = rawInput.getMouseY() / height * 2f - 1f;

        window.getActiveCamera().getPickRay(ndcX, ndcY, rayOrigin, rayDirection);
        subVoxelManager.raycast(itemEditorManager.getActiveDocument().getModel(), rayOrigin, rayDirection, hit);
        return true;
    }

    // Drop \\

    // The material laid on the part under the pointer, wherever the window's focus is
    public boolean dropMaterial(String materialName) {

        if (!itemEditorManager.hasActiveDocument())
            return false;

        hit.clear();
        castPointer(context.getWindow(), editorInputSystem.getRawInputHandle());

        return itemEditorManager.dropMaterial(hit, materialName);
    }

    // Accessible \\

    public SubVoxelHitStruct getHit() {
        return hit;
    }
}
