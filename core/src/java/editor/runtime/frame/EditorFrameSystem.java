package editor.runtime.frame;

import application.bootstrap.menupipeline.menumanager.ElementHitSystem;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.runtime.EditorSetting;
import editor.runtime.dock.EditorDockSystem;
import editor.runtime.menueventsmanager.menus.EditorFrameBranch;
import engine.input.Buttons;
import engine.input.Input;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class EditorFrameSystem extends SystemPackage {

    /*
     * Gives an editor OS window the body the OS frame would. Pressing on the
     * title bar where no control sits grabs the window, a second press there
     * soon after maximizes or restores it, and pressing along an edge or near
     * a corner resizes it from there. Every gesture moves the edges it holds
     * by what the cursor did, in screen units, until released, and an edge
     * stops where the window would shrink below its minimum. A window let go
     * of against its monitor's work area edge snaps to it: the top maximizes,
     * a side takes that half and a corner that quarter. A maximized or snapped
     * window dragged by its title bar restores under the cursor at the same
     * share of its width. The cursor takes the resize shape of the edges under
     * it, and a fullscreen window has no frame to work.
     */

    // Internal
    private InputManager inputManager;
    private ElementHitSystem elementHitSystem;
    private EditorDockSystem editorDockSystem;
    private EditorFrameBranch editorFrameBranch;

    // Gesture
    private int heldEdges;
    private boolean moved;
    private float grabX;
    private float grabY;
    private float[] grabBounds;

    // Snap
    private boolean snapped;
    private float[] unsnappedBounds;

    // Title Bar
    private long lastTitlePressMillis;

    // Cursor
    private int cursorEdges;

    // Scratch
    private float[] cursorScratch;
    private float[] boundsScratch;
    private float[] areaScratch;

    // Base \\

    @Override
    protected void create() {

        // Gesture
        this.grabBounds = new float[4];

        // Snap
        this.unsnappedBounds = new float[4];

        // Scratch
        this.cursorScratch = new float[2];
        this.boundsScratch = new float[4];
        this.areaScratch = new float[4];
    }

    @Override
    protected void get() {
        this.inputManager = get(InputManager.class);
        this.elementHitSystem = get(ElementHitSystem.class);
        this.editorDockSystem = get(EditorDockSystem.class);
        this.editorFrameBranch = get(EditorFrameBranch.class);
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();
        Input rawInput = inputManager.getRawInput(window);

        internal.windowPlatform.getCursorPos(window, cursorScratch);
        internal.windowPlatform.getScreenBounds(window, boundsScratch);

        boolean framed = !internal.windowPlatform.isFullscreen(window);
        boolean hovered = framed && internal.windowPlatform.isWindowHovered(window);

        if (hovered && rawInput.isMouseClicked(Buttons.LEFT))
            press(window);

        if (heldEdges == EditorSetting.FRAME_EDGES_ALL && moved && rawInput.isMouseReleased(Buttons.LEFT))
            snap(window);

        if (!framed || !rawInput.isMouseDown(Buttons.LEFT))
            this.heldEdges = EditorSetting.FRAME_EDGE_NONE;

        if (heldEdges != EditorSetting.FRAME_EDGE_NONE)
            drag(window);

        int hoveredEdges = hovered && !rawInput.isMouseDown(Buttons.LEFT)
                ? resolveResizeEdges(window)
                : EditorSetting.FRAME_EDGE_NONE;

        showCursor(window, heldEdges != EditorSetting.FRAME_EDGE_NONE ? heldEdges : hoveredEdges);
    }

    // Press \\

    private void press(WindowInstance window) {

        int edges = resolveResizeEdges(window);

        if (edges == EditorSetting.FRAME_EDGE_NONE && isOverTitleBar(window))
            edges = pressTitleBar(window);

        if (edges != EditorSetting.FRAME_EDGE_NONE && edges != EditorSetting.FRAME_EDGES_ALL)
            this.snapped = false;

        this.heldEdges = edges;
        this.moved = false;
        this.grabX = boundsScratch[0] + cursorScratch[0];
        this.grabY = boundsScratch[1] + cursorScratch[1];
        System.arraycopy(boundsScratch, 0, grabBounds, 0, grabBounds.length);
    }

    // A second press soon after the first maximizes or restores the window instead of grabbing it
    private int pressTitleBar(WindowInstance window) {

        long now = internal.getTime();
        boolean secondPress = now - lastTitlePressMillis <= EditorSetting.FRAME_DOUBLE_PRESS_MILLIS;
        this.lastTitlePressMillis = secondPress ? 0L : now;

        if (!secondPress)
            return EditorSetting.FRAME_EDGES_ALL;

        editorFrameBranch.toggleMaximized(window);
        return EditorSetting.FRAME_EDGE_NONE;
    }

    // The strip above the dock wherever no control is hovered
    private boolean isOverTitleBar(WindowInstance window) {
        return inputManager.getHoverMouseY(window) >= editorDockSystem.getDockTop()
                && elementHitSystem.getHoveredMenu() == null;
    }

    // Drag \\

    private void drag(WindowInstance window) {

        float dx = boundsScratch[0] + cursorScratch[0] - grabX;
        float dy = boundsScratch[1] + cursorScratch[1] - grabY;
        boolean maximized = internal.windowPlatform.isMaximized(window);

        if (maximized || snapped) {

            if (Math.abs(dx) + Math.abs(dy) < EditorSetting.FRAME_RESTORE_DISTANCE)
                return;

            restoreUnderGrab(window, maximized);
        }

        float left = grabBounds[0] + heldDelta(EditorSetting.FRAME_EDGE_LEFT, dx);
        float top = grabBounds[1] + heldDelta(EditorSetting.FRAME_EDGE_TOP, dy);
        float right = grabBounds[0] + grabBounds[2] + heldDelta(EditorSetting.FRAME_EDGE_RIGHT, dx);
        float bottom = grabBounds[1] + grabBounds[3] + heldDelta(EditorSetting.FRAME_EDGE_BOTTOM, dy);

        // A dragged edge stops at the minimum size; the edge across from it never moves for it
        if ((heldEdges & EditorSetting.FRAME_EDGE_LEFT) != 0)
            left = Math.min(left, right - EngineSetting.MIN_WINDOW_DIMENSION);
        else
            right = Math.max(right, left + EngineSetting.MIN_WINDOW_DIMENSION);

        if ((heldEdges & EditorSetting.FRAME_EDGE_TOP) != 0)
            top = Math.min(top, bottom - EngineSetting.MIN_WINDOW_DIMENSION);
        else
            bottom = Math.max(bottom, top + EngineSetting.MIN_WINDOW_DIMENSION);

        placeWindow(window, left, top, right - left, bottom - top);
    }

    private float heldDelta(int edge, float delta) {
        return (heldEdges & edge) != 0 ? delta : 0f;
    }

    // The grabbed point keeps its share of the width and its height in the title bar at the size the window had
    // before it was maximized or snapped
    private void restoreUnderGrab(WindowInstance window, boolean maximized) {

        float shareX = (grabX - grabBounds[0]) / grabBounds[2];
        float offsetY = grabY - grabBounds[1];

        if (maximized) {
            internal.windowPlatform.setMaximized(window, false);
            internal.windowPlatform.getScreenBounds(window, grabBounds);
        }

        else
            System.arraycopy(unsnappedBounds, 0, grabBounds, 0, grabBounds.length);

        this.snapped = false;
        grabBounds[0] = grabX - shareX * grabBounds[2];
        grabBounds[1] = grabY - offsetY;
    }

    private void placeWindow(WindowInstance window, float x, float y, float width, float height) {

        int screenX = Math.round(x);
        int screenY = Math.round(y);
        int screenWidth = Math.round(width);
        int screenHeight = Math.round(height);

        if (screenX == boundsScratch[0] && screenY == boundsScratch[1]
                && screenWidth == boundsScratch[2] && screenHeight == boundsScratch[3])
            return;

        this.moved = true;
        internal.windowPlatform.setScreenBounds(window, screenX, screenY, screenWidth, screenHeight);
    }

    // Snap \\

    // A window let go of against its monitor's work area edge fills the area that edge names, keeping its own
    // bounds to restore to once dragged away
    private void snap(WindowInstance window) {

        float cursorX = boundsScratch[0] + cursorScratch[0];
        float cursorY = boundsScratch[1] + cursorScratch[1];

        if (!internal.windowPlatform.getWorkArea(cursorX, cursorY, areaScratch))
            return;

        int edges = resolveEdges(
                cursorX - areaScratch[0],
                cursorY - areaScratch[1],
                areaScratch[2],
                areaScratch[3],
                EditorSetting.FRAME_SNAP_REACH,
                EditorSetting.FRAME_SNAP_CORNER);

        if (edges == EditorSetting.FRAME_SNAP_MAXIMIZE) {
            internal.windowPlatform.setMaximized(window, true);
            return;
        }

        float[] area = EditorSetting.FRAME_SNAP_AREAS[edges];

        if (area == null)
            return;

        System.arraycopy(boundsScratch, 0, unsnappedBounds, 0, unsnappedBounds.length);
        this.snapped = true;

        placeWindow(
                window,
                areaScratch[0] + area[0] * areaScratch[2],
                areaScratch[1] + area[1] * areaScratch[3],
                (area[2] - area[0]) * areaScratch[2],
                (area[3] - area[1]) * areaScratch[3]);
    }

    // Edges \\

    // The window's edges under the cursor; none while maximized
    private int resolveResizeEdges(WindowInstance window) {

        if (internal.windowPlatform.isMaximized(window))
            return EditorSetting.FRAME_EDGE_NONE;

        return resolveEdges(
                cursorScratch[0],
                cursorScratch[1],
                boundsScratch[2],
                boundsScratch[3],
                EditorSetting.FRAME_RESIZE_BORDER,
                EditorSetting.FRAME_RESIZE_CORNER);
    }

    // The edges of a rect within reach of a point inside it, an edge near a corner taking the edge it meets there
    private static int resolveEdges(float x, float y, float width, float height, float reach, float cornerReach) {

        int edges = edgesWithin(x, y, width, height, reach);
        int corners = edgesWithin(x, y, width, height, cornerReach);

        if ((edges & EditorSetting.FRAME_EDGES_X) != 0)
            edges |= corners & EditorSetting.FRAME_EDGES_Y;

        if ((edges & EditorSetting.FRAME_EDGES_Y) != 0)
            edges |= corners & EditorSetting.FRAME_EDGES_X;

        return edges;
    }

    private static int edgesWithin(float x, float y, float width, float height, float reach) {
        return (x < reach ? EditorSetting.FRAME_EDGE_LEFT : EditorSetting.FRAME_EDGE_NONE)
                | (x >= width - reach ? EditorSetting.FRAME_EDGE_RIGHT : EditorSetting.FRAME_EDGE_NONE)
                | (y < reach ? EditorSetting.FRAME_EDGE_TOP : EditorSetting.FRAME_EDGE_NONE)
                | (y >= height - reach ? EditorSetting.FRAME_EDGE_BOTTOM : EditorSetting.FRAME_EDGE_NONE);
    }

    // Cursor \\

    private void showCursor(WindowInstance window, int edges) {

        if (edges == cursorEdges)
            return;

        this.cursorEdges = edges;
        internal.windowPlatform.setCursorShape(window.getNativeHandle(), EditorSetting.FRAME_EDGE_CURSORS[edges]);
    }
}
