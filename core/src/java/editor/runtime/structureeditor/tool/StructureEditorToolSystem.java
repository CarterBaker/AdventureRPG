package editor.runtime.structureeditor.tool;

import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.structureeditorpipeline.structureeditormanager.StructureEditorManager;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.structureeditor.StructureEditorSetting;
import editor.runtime.structureeditor.view.StructureEditorViewSystem;
import editor.runtime.viewcamera.ViewCameraSystem;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;

public class StructureEditorToolSystem extends SystemPackage {

    /*
     * Casts the pointer into the structure each frame and hands the tool
     * chosen a press that lands on a cell or the ground under it, claiming
     * the press from the view camera; a press on nothing, and every press
     * under the Hand, is the camera's. The Brush and Erase stroke for as long
     * as the button is held, the ray cast again every frame so the stroke
     * follows the pointer, and end when it is let go. Nothing is cast while
     * the pointer is off the viewport or the camera holds a drag.
     */

    // Internal
    private StructureEditorManager structureEditorManager;
    private ToolManager toolManager;
    private EditorInputSystem editorInputSystem;
    private ViewCameraSystem viewCameraSystem;
    private StructureEditorViewSystem structureEditorViewSystem;

    // Pointer
    private StructureEditorHitStruct hit;
    private Vector3 rayOrigin;
    private Vector3 rayDirection;

    // Base \\

    @Override
    protected void create() {

        // Pointer
        this.hit = new StructureEditorHitStruct();
        this.rayOrigin = new Vector3();
        this.rayDirection = new Vector3();
    }

    @Override
    protected void get() {
        this.structureEditorManager = get(StructureEditorManager.class);
        this.toolManager = get(ToolManager.class);
        this.editorInputSystem = get(EditorInputSystem.class);
        this.viewCameraSystem = get(ViewCameraSystem.class);
        this.structureEditorViewSystem = get(StructureEditorViewSystem.class);
    }

    // Update \\

    @Override
    protected void update() {

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();

        if (structureEditorManager.isStroking()) {
            updateStroke(rawInput);
            return;
        }

        if (!editorInputSystem.isPointerActive()
                || structureEditorManager.getStructureName() == null
                || viewCameraSystem.isDragging()
                || toolManager.getTool(ToolCanvas.STRUCTURE) == EditorTool.HAND)
            return;

        if (!castPointer(context.getWindow(), rawInput) || !hit.hasHit()
                || !editorInputSystem.takeClick(StructureEditorSetting.BUTTON_APPLY))
            return;

        structureEditorManager.pressTool(hit.getCellX(), hit.getCellY(), hit.getCellZ(), hit.getFace());
    }

    // The held stroke follows the pointer until the button is let go, the press kept from the camera throughout
    private void updateStroke(RawInputHandle rawInput) {

        if (!rawInput.isButtonHeld(StructureEditorSetting.BUTTON_APPLY)) {
            structureEditorManager.releaseTool();
            return;
        }

        editorInputSystem.claim(StructureEditorSetting.BUTTON_APPLY);

        if (castPointer(context.getWindow(), rawInput) && hit.hasHit())
            structureEditorManager.dragTool(hit.getCellX(), hit.getCellY(), hit.getCellZ(), hit.getFace());
    }

    // Pointer \\

    // The pointer's ray cast into the structure — false while the window has no size
    private boolean castPointer(WindowInstance window, RawInputHandle rawInput) {

        float width = window.getWidth();
        float height = window.getHeight();

        if (width <= 0f || height <= 0f)
            return false;

        float ndcX = rawInput.getMouseX() / width * 2f - 1f;
        float ndcY = rawInput.getMouseY() / height * 2f - 1f;

        window.getActiveCamera().getPickRay(ndcX, ndcY, rayOrigin, rayDirection);
        structureEditorViewSystem.cast(rayOrigin, rayDirection, hit);

        return true;
    }
}
