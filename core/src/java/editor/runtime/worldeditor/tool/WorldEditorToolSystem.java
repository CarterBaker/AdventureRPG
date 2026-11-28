package editor.runtime.worldeditor.tool;

import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import editor.bootstrap.settlementplannerpipeline.settlementplannermanager.SettlementPlannerManager;
import editor.bootstrap.shortcutpipeline.shortcutmanager.ShortcutManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.worldeditorpipeline.util.WorldEditorLayer;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.worldeditor.WorldEditorSetting;
import editor.runtime.worldeditor.layer.WorldEditorLayerSystem;
import editor.runtime.worldeditor.view.WorldEditorViewSystem;
import engine.root.SystemPackage;

public class WorldEditorToolSystem extends SystemPackage {

    /*
     * Turns the pointer into the image pixel under it each frame and drives
     * the tool ToolManager keeps for the layer picked in the Layers tab; a
     * new layer picked ends the stroke or press under way on the one before.
     * A click on the image begins a stroke, dragging carries it on wherever
     * the pointer goes, and releasing ends it as one undo step. While the
     * biome layer places, Place and Erase act once per click on the exact
     * position under the pointer, and Select follows the editor's selection
     * conventions: a click picks the placement nearest the pointer, Shift and
     * Ctrl adding and taking away, a drag from a placement moves everything
     * selected with it once released, and a drag from nothing draws a box.
     * Move begins nothing: its press drags the view as the pan button does.
     * On a globe's Cube layer Select picks the world's entry and Rotate turns
     * the cube from the point pressed on the ball to the point under the
     * pointer, written once let go. Every press a tool takes is claimed, so
     * the view camera never orbits under a stroke.
     * Nothing begins while the view is being panned. While the Settlement
     * Planner carries a design over the window while its picked layer places,
     * focused or not, the pointer is followed for the drop instead, the
     * design previewed on every map at the column under it, and a drop stands
     * it there. The hovered pixel is kept for the brush outline and status
     * line, and the exact image position under the pointer for the placement
     * footprint, the box and the move preview.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private InputManager inputManager;
    private WindowManager windowManager;
    private ShortcutManager shortcutManager;
    private WorldEditorManager worldEditorManager;
    private SettlementPlannerManager settlementPlannerManager;
    private WorldEditorViewSystem worldEditorViewSystem;
    private WorldEditorLayerSystem worldEditorLayerSystem;

    // Layer
    private WorldEditorLayer layer;

    // Shortcuts
    private int addSelectionShortcutID;
    private int subtractSelectionShortcutID;
    private int invertStrokeShortcutID;

    // Pointer
    private boolean hovering;
    private boolean dropping;
    private int hoveredX;
    private int hoveredY;
    private double hoveredImageX;
    private double hoveredImageY;
    private float mouseX;
    private float mouseY;

    // Stroke
    private boolean stroking;
    private boolean turning;

    // Selection
    private boolean pressed;
    private boolean boxing;
    private boolean moving;
    private float pressX;
    private float pressY;
    private double pressImageX;
    private double pressImageY;
    private WorldPlacementStruct pressedPlacement;

    // Base \\

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.inputManager = get(InputManager.class);
        this.windowManager = get(WindowManager.class);
        this.shortcutManager = get(ShortcutManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
        this.settlementPlannerManager = get(SettlementPlannerManager.class);
        this.worldEditorViewSystem = get(WorldEditorViewSystem.class);
        this.worldEditorLayerSystem = get(WorldEditorLayerSystem.class);
    }

    @Override
    protected void awake() {
        this.layer = worldEditorLayerSystem.getLayer();
    }

    @Override
    protected void start() {

        // Shortcuts
        this.addSelectionShortcutID = shortcutManager.getShortcutIDFromShortcutName(
                WorldEditorSetting.SHORTCUT_ADD_SELECTION);
        this.subtractSelectionShortcutID = shortcutManager.getShortcutIDFromShortcutName(
                WorldEditorSetting.SHORTCUT_SUBTRACT_SELECTION);
        this.invertStrokeShortcutID = shortcutManager.getShortcutIDFromShortcutName(
                WorldEditorSetting.SHORTCUT_INVERT_STROKE);
    }

    @Override
    protected void dispose() {
        endStroke();
        endTurn();
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();
        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();

        adoptLayer();

        dropping = layer.isPlacing()
                && settlementPlannerManager.isDragging()
                && windowManager.getHoveredWindows().contains(window);

        if (dropping) {
            hover(inputManager.getHoverMouseX(window), inputManager.getHoverMouseY(window), true);
            hoverDrop();
            return;
        }

        hover(rawInput.getMouseX(), rawInput.getMouseY(), editorInputSystem.isPointerActive());

        if (stroking && !rawInput.isButtonHeld(WorldEditorSetting.BUTTON_APPLY))
            endStroke();

        if (stroking) {
            worldEditorManager.continueStroke(layer, hoveredX, hoveredY);
            return;
        }

        if (turning) {
            updateTurn(rawInput);
            return;
        }

        if (pressed) {
            updatePress(rawInput);
            return;
        }

        if (!hovering || worldEditorViewSystem.isPanning()
                || !editorInputSystem.takeClick(WorldEditorSetting.BUTTON_APPLY))
            return;

        if (layer.isCube()) {
            pressCube();
            return;
        }

        if (worldEditorManager.getTool(layer) == EditorTool.SELECT) {
            press();
            return;
        }

        if (!worldEditorManager.isPainting(layer)) {
            worldEditorManager.applyPlacement(hoveredImageX, hoveredImageY);
            return;
        }

        worldEditorManager.beginStroke(layer, hoveredX, hoveredY, isShortcutHeld(invertStrokeShortcutID));
        stroking = true;
    }

    // The layer picked in the Layers tab, the stroke or press under way on the one before ended first
    private void adoptLayer() {

        WorldEditorLayer picked = worldEditorLayerSystem.getLayer();

        if (picked == layer)
            return;

        endStroke();
        endTurn();
        pressed = false;
        boxing = false;
        moving = false;
        pressedPlacement = null;
        this.layer = picked;
    }

    // The image position under a window position, hovering while it is on the image and the pointer counts
    private void hover(float screenX, float screenY, boolean pointerActive) {

        ImageDocumentInstance image = worldEditorManager.getWorldImage();

        worldEditorViewSystem.locate(screenX, screenY);

        mouseX = screenX;
        mouseY = screenY;
        hoveredImageX = worldEditorViewSystem.getLocatedImageX();
        hoveredImageY = worldEditorViewSystem.getLocatedImageY();
        hoveredX = (int) Math.floor(hoveredImageX);
        hoveredY = (int) Math.floor(hoveredImageY);
        hovering = pointerActive
                && worldEditorViewSystem.isLocated()
                && hoveredX >= 0 && hoveredY >= 0 && hoveredX < image.getWidth() && hoveredY < image.getHeight();
    }

    // Cube \\

    // Select picks the world's entry; Rotate begins turning the cube from the point pressed
    private void pressCube() {

        EditorTool tool = worldEditorManager.getTool(layer);

        if (tool == EditorTool.SELECT) {
            worldEditorManager.selectCube();
            return;
        }

        if (tool != EditorTool.ROTATE)
            return;

        worldEditorManager.beginCubeTurn(worldEditorViewSystem.getLocatedDirection());
        turning = true;
    }

    // The cube follows the pointer over the ball while the button is held, and is written once let go
    private void updateTurn(RawInputHandle rawInput) {

        if (!rawInput.isButtonHeld(WorldEditorSetting.BUTTON_APPLY)) {
            endTurn();
            return;
        }

        if (hovering)
            worldEditorManager.dragCubeTurn(worldEditorViewSystem.getLocatedDirection());
    }

    private void endTurn() {

        if (!turning)
            return;

        worldEditorManager.endCubeTurn();
        turning = false;
    }

    private void endStroke() {

        if (!stroking)
            return;

        worldEditorManager.endStroke(layer);
        stroking = false;
    }

    // Selection \\

    private void press() {

        double reach = WorldEditorSetting.SELECT_REACH_PIXELS
                * worldEditorViewSystem.resolveImagePixelsPerScreenPixel();

        pressed = true;
        boxing = false;
        moving = false;
        pressX = mouseX;
        pressY = mouseY;
        pressImageX = hoveredImageX;
        pressImageY = hoveredImageY;
        pressedPlacement = worldEditorManager.findPlacementNear(hoveredImageX, hoveredImageY, reach);
    }

    // A press held past the drag distance moves from a placement or boxes from nothing; letting go applies it
    private void updatePress(RawInputHandle rawInput) {

        boolean add = isShortcutHeld(addSelectionShortcutID);
        boolean subtract = isShortcutHeld(subtractSelectionShortcutID);

        if (rawInput.isButtonHeld(WorldEditorSetting.BUTTON_APPLY)) {

            float dx = mouseX - pressX;
            float dy = mouseY - pressY;
            float drag = WorldEditorSetting.DRAG_PIXELS;

            if (!boxing && !moving && dx * dx + dy * dy > drag * drag)
                beginDrag(add, subtract);

            return;
        }

        if (moving)
            worldEditorManager.moveSelection(hoveredImageX - pressImageX, hoveredImageY - pressImageY);
        else if (boxing)
            worldEditorManager.boxSelection(
                    Math.min(pressImageX, hoveredImageX),
                    Math.min(pressImageY, hoveredImageY),
                    Math.max(pressImageX, hoveredImageX),
                    Math.max(pressImageY, hoveredImageY),
                    add,
                    subtract);
        else
            worldEditorManager.clickSelection(pressedPlacement, add, subtract);

        pressed = false;
        boxing = false;
        moving = false;
        pressedPlacement = null;
    }

    // Dragging a placement picks it alone first unless it is already selected; a modifier always boxes
    private void beginDrag(boolean add, boolean subtract) {

        if (pressedPlacement == null || add || subtract) {
            boxing = true;
            return;
        }

        if (!worldEditorManager.isPlacementSelected(pressedPlacement))
            worldEditorManager.clickSelection(pressedPlacement, false, false);

        moving = true;
    }

    private boolean isShortcutHeld(int shortcutID) {
        return shortcutManager.isHeld(shortcutID, context.getWindow());
    }

    // Drop \\

    private void hoverDrop() {

        if (hovering)
            settlementPlannerManager.hoverDrop(
                    hoveredX,
                    hoveredY,
                    worldEditorManager.toBlockX(hoveredImageX, hoveredImageY),
                    worldEditorManager.toBlockZ(hoveredImageX, hoveredImageY));
    }

    // The carried design stood under the pointer and this window focused, nothing done off the image or while the
    // layer picked does not place
    public void drop() {

        WindowInstance window = context.getWindow();

        if (!layer.isPlacing())
            return;

        hover(inputManager.getHoverMouseX(window), inputManager.getHoverMouseY(window), true);

        if (!hovering)
            return;

        worldEditorManager.placeDesign(hoveredImageX, hoveredImageY);
        windowManager.setFocusedWindow(window);
    }

    // Accessible \\

    public WorldEditorLayer getLayer() {
        return layer;
    }

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

    public boolean isBoxing() {
        return boxing;
    }

    // True while the selection is dragged, drawn where it would stand
    public boolean isMoving() {
        return moving;
    }

    public double getPressImageX() {
        return pressImageX;
    }

    public double getPressImageY() {
        return pressImageY;
    }
}
