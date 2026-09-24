package editor.itemeditor.tool;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelHitStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.itemeditorpipeline.itemeditormanager.ItemEditorManager;
import editor.itemeditor.ItemEditorSetting;
import engine.editor.EditorInputSystem;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;

public class ItemEditorToolSystem extends SystemPackage {

    /*
     * Casts the pointer into the active item each frame and applies the active
     * tool on click. Nothing is cast while the pointer is off the viewport or
     * the orbit button is held. The last hit is kept for the cursor.
     */

    // Internal
    private ItemEditorManager itemEditorManager;
    private SubVoxelManager subVoxelManager;
    private EditorInputSystem editorInputSystem;

    // Pointer
    private SubVoxelHitStruct hit;
    private Vector3 rayOrigin;
    private Vector3 rayDirection;

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
    }

    // Update \\

    @Override
    protected void update() {

        hit.clear();

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();

        if (!editorInputSystem.isPointerActive()
                || !itemEditorManager.hasActiveDocument()
                || rawInput.isButtonHeld(ItemEditorSetting.BUTTON_ORBIT))
            return;

        castPointer(context.getWindow(), rawInput);

        if (editorInputSystem.isClicked(ItemEditorSetting.BUTTON_APPLY))
            itemEditorManager.applyTool(hit);
    }

    // Pointer \\

    private void castPointer(WindowInstance window, RawInputHandle rawInput) {

        float width = window.getWidth();
        float height = window.getHeight();

        if (width <= 0f || height <= 0f)
            return;

        float ndcX = rawInput.getMouseX() / width * 2f - 1f;
        float ndcY = rawInput.getMouseY() / height * 2f - 1f;

        window.getActiveCamera().getPickRay(ndcX, ndcY, rayOrigin, rayDirection);
        subVoxelManager.raycast(itemEditorManager.getActiveDocument().getModel(), rayOrigin, rayDirection, hit);
    }

    // Accessible \\

    public SubVoxelHitStruct getHit() {
        return hit;
    }
}
