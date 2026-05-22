package editor.runtime.editor.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.shaderpipeline.sprite.SpriteHandle;
import application.bootstrap.shaderpipeline.spritemanager.SpriteManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.tab.TabHandle;
import editor.bootstrap.tabmanager.TabManager;
import editor.runtime.editor.EditorWindowSetting;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TabBranch extends BranchPackage {

    /*
     * Menu event handlers for tab chrome menus (TabFrame).
     *
     * Each TabFrame menu instance is owned by a TabContext whose window is
     * registered in TabManager. Resolving the MenuInstance back to a TabHandle
     * goes through getTabHandleForWindow() — the same resolver that InputSystem
     * uses for authority mapping — so there is no separate lookup table to
     * maintain here.
     *
     * closeTab() is intentionally a no-op when the handle is not found rather
     * than throwing, because a rapid double-click could fire the handler a
     * second time after the tab is already gone.
     *
     * Cursor sprites for resize handles are loaded once at startup from the
     * paths declared in EditorWindowSetting. checkResizeCursor is a general
     * helper — it takes any window + element and sets the appropriate directional
     * cursor based on how close the mouse is to each edge. Intended to be reused
     * for other resizable elements later.
     *
     * Mouse Y from GLFW is already converted to Y-up in Lwjgl3Input.onCursor
     * before it reaches getMouseY(), so element computed bounds and mouse
     * coordinates are in the same space — no flip needed here.
     */

    private TabManager tabManager;
    private InputManager inputManager;
    private SpriteManager spriteManager;

    private SpriteHandle cursorResizeH;
    private SpriteHandle cursorResizeV;

    @Override
    protected void get() {
        this.tabManager = get(TabManager.class);
        this.inputManager = get(InputManager.class);
        this.spriteManager = get(SpriteManager.class);
        this.cursorResizeH = spriteManager.getSpriteHandleFromSpriteName(EditorWindowSetting.CURSOR_RESIZE_H);
        this.cursorResizeV = spriteManager.getSpriteHandleFromSpriteName(EditorWindowSetting.CURSOR_RESIZE_V);
    }

    // Tab Events \\

    public void closeTab(MenuInstance menu) {
        TabHandle handle = tabManager.getTabHandleForWindow(menu.getWindow());
        if (handle == null)
            return;
        tabManager.closeTab(handle);
    }

    public void onTabFrameHover(MenuInstance menu) {
        ElementInstance frame = findElement(menu, "window_frame");
        if (frame == null)
            return;
        checkResizeCursor(menu.getWindow(), frame);
    }

    public void onTabFrameHoverExit(MenuInstance menu) {
        inputManager.clearCursor();
    }

    // Resize Cursor — reusable \\

    private void checkResizeCursor(WindowInstance window, ElementInstance element) {

        float mouseX = inputManager.getMouseX(window);
        float mouseY = inputManager.getMouseY(window);

        float left = element.getComputedLeft();
        float bottom = element.getComputedTop();
        float right = left + element.getComputedW();
        float top = bottom + element.getComputedH();

        float t = EditorWindowSetting.RESIZE_EDGE_TOLERANCE;

        boolean onHEdge = mouseX <= left + t || mouseX >= right - t;
        boolean onVEdge = mouseY <= bottom + t || mouseY >= top - t;

        if (onHEdge)
            inputManager.setCursorSprite(cursorResizeH);
        else if (onVEdge)
            inputManager.setCursorSprite(cursorResizeV);
        else
            inputManager.clearCursor();
    }

    // Element Lookup \\

    private ElementInstance findElement(MenuInstance menu, String id) {
        return findElement(menu.getElements(), id);
    }

    private ElementInstance findElement(ObjectArrayList<ElementInstance> elements, String id) {
        for (int i = 0; i < elements.size(); i++) {
            ElementInstance el = elements.get(i);
            if (el.getElementData().getId().equals(id))
                return el;
            if (el.hasChildren()) {
                ElementInstance found = findElement(el.getChildren(), id);
                if (found != null)
                    return found;
            }
        }
        return null;
    }
}