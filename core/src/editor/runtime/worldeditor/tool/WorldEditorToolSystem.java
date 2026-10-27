package editor.runtime.worldeditor.tool;

import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import editor.bootstrap.settlementplannerpipeline.settlementplannermanager.SettlementPlannerManager;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.worldeditor.WorldEditorSetting;
import editor.runtime.worldeditor.view.WorldEditorViewSystem;
import engine.root.SystemPackage;

public class WorldEditorToolSystem extends SystemPackage {

    /*
     * Turns the pointer into the image pixel under it each frame and drives
     * the active tool: a click on the image begins a stroke, dragging carries
     * it on wherever the pointer goes, and releasing ends it as one undo step.
     * A placement tool instead acts once per click, on the exact position
     * under the pointer. Nothing begins while the view is being panned. While
     * the Settlement Planner carries a design over this window, focused or
     * not, the pointer is followed for the drop instead, the design previewed
     * on every map at the column under it, and a drop stands it there. The
     * hovered pixel is kept for the brush outline and status line, and the
     * exact image position under the pointer for the placement footprint.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private InputManager inputManager;
    private WindowManager windowManager;
    private WorldEditorManager worldEditorManager;
    private SettlementPlannerManager settlementPlannerManager;
    private WorldEditorViewSystem worldEditorViewSystem;

    // Pointer
    private boolean hovering;
    private boolean dropping;
    private int hoveredX;
    private int hoveredY;
    private double hoveredImageX;
    private double hoveredImageY;

    // Stroke
    private boolean stroking;

    // Base \\

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.inputManager = get(InputManager.class);
        this.windowManager = get(WindowManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
        this.settlementPlannerManager = get(SettlementPlannerManager.class);
        this.worldEditorViewSystem = get(WorldEditorViewSystem.class);
    }

    @Override
    protected void dispose() {
        endStroke();
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();
        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();

        dropping = settlementPlannerManager.isDragging() && windowManager.getHoveredWindows().contains(window);

        if (dropping) {
            hover(inputManager.getHoverMouseX(window), inputManager.getHoverMouseY(window), true);
            hoverDrop();
            return;
        }

        hover(rawInput.getMouseX(), rawInput.getMouseY(), editorInputSystem.isPointerActive());

        if (stroking && !rawInput.isButtonHeld(WorldEditorSetting.BUTTON_APPLY))
            endStroke();

        if (stroking) {
            worldEditorManager.continueStroke(hoveredX, hoveredY);
            return;
        }

        if (!hovering || worldEditorViewSystem.isPanning()
                || !editorInputSystem.isClicked(WorldEditorSetting.BUTTON_APPLY))
            return;

        if (worldEditorManager.getTool().isPlacement()) {
            worldEditorManager.applyPlacement(hoveredImageX, hoveredImageY);
            return;
        }

        worldEditorManager.beginStroke(hoveredX, hoveredY);
        stroking = true;
    }

    // The image position under a window position, hovering while it is on the image and the pointer counts
    private void hover(float screenX, float screenY, boolean pointerActive) {

        ImageDocumentInstance image = worldEditorManager.getWorldImage();

        hoveredImageX = worldEditorViewSystem.screenToImageX(screenX);
        hoveredImageY = worldEditorViewSystem.screenToImageY(screenY);
        hoveredX = (int) Math.floor(hoveredImageX);
        hoveredY = (int) Math.floor(hoveredImageY);
        hovering = pointerActive
                && hoveredX >= 0 && hoveredY >= 0 && hoveredX < image.getWidth() && hoveredY < image.getHeight();
    }

    private void endStroke() {

        if (!stroking)
            return;

        worldEditorManager.endStroke();
        stroking = false;
    }

    // Drop \\

    private void hoverDrop() {

        if (hovering)
            settlementPlannerManager.hoverDrop(
                    hoveredX,
                    hoveredY,
                    worldEditorManager.toBlock(hoveredImageX),
                    worldEditorManager.toBlock(hoveredImageY));
    }

    // The carried design stood under the pointer and this window focused, nothing done off the image
    public void drop() {

        WindowInstance window = context.getWindow();

        hover(inputManager.getHoverMouseX(window), inputManager.getHoverMouseY(window), true);

        if (!hovering)
            return;

        worldEditorManager.placeDesign(hoveredImageX, hoveredImageY);
        windowManager.setFocusedWindow(window);
    }

    // Accessible \\

    public boolean isHovering() {
        return hovering;
    }

    // True while the Settlement Planner carries a design over this window
    public boolean isDropping() {
        return dropping;
    }

    public int getHoveredX() {
        return hoveredX;
    }

    public int getHoveredY() {
        return hoveredY;
    }

    public double getHoveredImageX() {
        return hoveredImageX;
    }

    public double getHoveredImageY() {
        return hoveredImageY;
    }
}
