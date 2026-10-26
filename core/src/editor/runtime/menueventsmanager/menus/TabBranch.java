package editor.runtime.menueventsmanager.menus;

import application.bootstrap.shaderpipeline.sprite.SpriteHandle;
import application.bootstrap.shaderpipeline.spritemanager.SpriteManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.tabpipeline.docklayoutsystem.DockLayoutSystem;
import editor.bootstrap.tabpipeline.docknode.DockNodeStruct;
import editor.bootstrap.tabpipeline.tabdragmanager.TabDragManager;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import editor.runtime.EditorSetting;
import engine.input.Buttons;
import engine.root.BranchPackage;

public class TabBranch extends BranchPackage {

    /*
     * Menu callbacks for tab chrome. Every gesture receives the window it
     * happened on from ElementHitSystem. Frame drags resize the BSP divider
     * found under the press and persist the ratio on release; presses on a
     * strip entry or the toolbar behind it hand that tab to TabDragManager,
     * which shows it on a click and moves it on a drag; strip close buttons
     * name the tab they close; hover callbacks keep the resize cursor in sync.
     */

    // Internal
    private TabManager tabManager;
    private InputManager inputManager;
    private SpriteManager spriteManager;
    private DockLayoutSystem dockLayoutSystem;
    private TabDragManager tabDragManager;

    // Cursor sprites
    private SpriteHandle cursorResizeH;
    private SpriteHandle cursorResizeV;

    // Divider drag state
    private DockNodeStruct hoveredDivider;
    private DockNodeStruct dragNode;
    private WindowInstance dragWindow;
    private boolean isDividerDrag;

    // Internal \\

    @Override
    protected void get() {
        this.tabManager = get(TabManager.class);
        this.inputManager = get(InputManager.class);
        this.spriteManager = get(SpriteManager.class);
        this.dockLayoutSystem = get(DockLayoutSystem.class);
        this.tabDragManager = get(TabDragManager.class);
    }

    @Override
    protected void awake() {
        this.cursorResizeH = spriteManager.getSpriteHandleFromSpriteName(EditorSetting.CURSOR_RESIZE_H);
        this.cursorResizeV = spriteManager.getSpriteHandleFromSpriteName(EditorSetting.CURSOR_RESIZE_V);
    }

    @Override
    protected void update() {
        if (isDividerDrag && inputManager.getRawInput(dragWindow.getGLWindow()).isMouseReleased(Buttons.LEFT))
            endDividerDrag();
    }

    // Tab Events \\

    public void closeTab(String tabTitle) {
        if (tabManager.hasTab(tabTitle))
            tabManager.closeTab(tabManager.getTabHandleFromTabName(tabTitle));
    }

    public void onTabFrameHover(WindowInstance window) {

        float mouseX = inputManager.getHoverMouseX(window);
        float mouseY = inputManager.getHoverMouseY(window);
        float localX = window.getCompositeX() + mouseX;
        float localY = window.getCompositeY() + mouseY;

        hoveredDivider = dockLayoutSystem.findDividerAt(window.getGLWindow(), localX, localY);

        if (hoveredDivider != null) {
            inputManager.setCursorSprite(hoveredDivider.isSplitHorizontal() ? cursorResizeV : cursorResizeH);
            return;
        }

        checkResizeCursor(window, mouseX, mouseY);
    }

    public void onTabFrameHoverExit() {
        hoveredDivider = null;
        inputManager.clearCursor();
    }

    // window_frame drag — BSP divider resize only.
    // If the cursor is not over a divider the gesture is ignored.
    // Tab dragging is handled by onTabToolbarDrag below.
    public void onTabFrameDrag(WindowInstance window) {

        if (!isDividerDrag) {

            if (hoveredDivider == null)
                return; // Not a divider — nothing for the background drag to do.

            dragNode = hoveredDivider;
            dragWindow = window;
            isDividerDrag = true;
        }

        boolean released = inputManager.getRawInput(dragWindow.getGLWindow()).isMouseReleased(Buttons.LEFT);
        handleDividerDrag(released);
    }

    // tab_strip_entry and tab_toolbar presses — TabDragManager takes the tab
    // on the first call and then owns the press and drag via update();
    // subsequent callbacks are no-ops. The toolbar behind the strip stands
    // for the tab it shows.
    public void onTabStripDrag(String tabTitle) {
        if (tabManager.hasTab(tabTitle))
            tabDragManager.onTabPressed(tabManager.getTabHandleFromTabName(tabTitle));
    }

    public void onTabToolbarDrag(WindowInstance window) {
        tabDragManager.onTabPressed(tabManager.getTabHandleForWindow(window));
    }

    // Divider Drag \\

    private void handleDividerDrag(boolean released) {

        if (released) {
            endDividerDrag();
            return;
        }

        if (dragNode == null)
            return;

        float localX = dragWindow.getCompositeX() + inputManager.getHoverMouseX(dragWindow);
        float localY = dragWindow.getCompositeY() + inputManager.getHoverMouseY(dragWindow);

        float ratio = dragNode.isSplitHorizontal()
                ? (localY - dragNode.getY()) / dragNode.getH()
                : (localX - dragNode.getX()) / dragNode.getW();

        dockLayoutSystem.setSplitRatio(dragNode, ratio);
        tabManager.pushRects();
    }

    private void endDividerDrag() {
        dragNode = null;
        dragWindow = null;
        isDividerDrag = false;
        tabManager.notifyLayoutChanged();
    }

    // Resize Cursor \\

    private void checkResizeCursor(WindowInstance window, float mouseX, float mouseY) {

        float w = window.getWidth();
        float h = window.getHeight();
        float t = EditorSetting.RESIZE_EDGE_TOLERANCE;

        boolean onHEdge = mouseX <= t || mouseX >= w - t;
        boolean onVEdge = mouseY <= t || mouseY >= h - t;

        if (onHEdge)
            inputManager.setCursorSprite(cursorResizeH);
        else if (onVEdge)
            inputManager.setCursorSprite(cursorResizeV);
        else
            inputManager.clearCursor();
    }
}