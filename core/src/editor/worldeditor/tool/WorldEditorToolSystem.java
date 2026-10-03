package editor.worldeditor.tool;

import application.kernel.inputpipeline.input.RawInputHandle;
import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.EditorInputSystem;
import editor.worldeditor.WorldEditorSetting;
import editor.worldeditor.view.WorldEditorViewSystem;
import engine.root.SystemPackage;

public class WorldEditorToolSystem extends SystemPackage {

    /*
     * Turns the pointer into the image pixel under it each frame and drives
     * the active tool: a click on the image begins a stroke, dragging carries
     * it on wherever the pointer goes, and releasing ends it as one undo step.
     * Nothing begins while the view is being panned. The hovered pixel is
     * kept for the brush outline and status line.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private WorldEditorManager worldEditorManager;
    private WorldEditorViewSystem worldEditorViewSystem;

    // Pointer
    private boolean hovering;
    private int hoveredX;
    private int hoveredY;

    // Stroke
    private boolean stroking;

    // Base \\

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.worldEditorManager = get(WorldEditorManager.class);
        this.worldEditorViewSystem = get(WorldEditorViewSystem.class);
    }

    @Override
    protected void dispose() {
        endStroke();
    }

    // Update \\

    @Override
    protected void update() {

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();
        ImageDocumentInstance image = worldEditorManager.getWorldImage();

        hoveredX = (int) Math.floor(worldEditorViewSystem.screenToImageX(rawInput.getMouseX()));
        hoveredY = (int) Math.floor(worldEditorViewSystem.screenToImageY(rawInput.getMouseY()));
        hovering = editorInputSystem.isPointerActive()
                && hoveredX >= 0 && hoveredY >= 0 && hoveredX < image.getWidth() && hoveredY < image.getHeight();

        if (stroking && !rawInput.isButtonHeld(WorldEditorSetting.BUTTON_APPLY))
            endStroke();

        if (stroking)
            worldEditorManager.continueStroke(hoveredX, hoveredY);
        else if (hovering && !worldEditorViewSystem.isPanning()
                && editorInputSystem.isClicked(WorldEditorSetting.BUTTON_APPLY)) {
            worldEditorManager.beginStroke(hoveredX, hoveredY);
            stroking = true;
        }
    }

    private void endStroke() {

        if (!stroking)
            return;

        worldEditorManager.endStroke();
        stroking = false;
    }

    // Accessible \\

    public boolean isHovering() {
        return hovering;
    }

    public int getHoveredX() {
        return hoveredX;
    }

    public int getHoveredY() {
        return hoveredY;
    }
}
