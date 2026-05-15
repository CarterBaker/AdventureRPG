package editor.runtime;

import application.bootstrap.menupipeline.canvas.CanvasInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.tab.TabContext;
import editor.bootstrap.tab.TabHandle;
import editor.bootstrap.tabmanager.TabManager;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class EditorTabCompositorSystem extends SystemPackage {

    /*
     * Two responsibilities:
     *
     * 1. Dock resize — detects dock canvas bound changes and forwards them to
     * TabManager via setDockRect(). TabManager owns BSP rect propagation and
     * tab window composite rect assignment. Only fires when bounds change.
     *
     * 2. Content rect sync — every frame, reads the current chrome canvas
     * bounds from each tab's TabContext and pushes them to the content window.
     * This runs after menu layout has settled, which is why it lives here
     * rather than in TabContext.onResize(). onResize() fires at resize() call
     * time, before layout; this system fires each frame after layout, so it
     * always reads correct canvas values.
     *
     * The sync is change-guarded: contentWindow.resize() only fires when the
     * computed rect actually differs from what the content window already holds,
     * avoiding redundant resize cascades each frame.
     */

    // Internal
    private TabManager tabManager;
    private EditorMenuSystem editorMenuSystem;

    // Cached Dock Rect
    private float lastX;
    private float lastY;
    private float lastW;
    private float lastH;

    // Internal \\

    @Override
    protected void get() {
        tabManager = get(TabManager.class);
        editorMenuSystem = get(EditorMenuSystem.class);
    }

    // Update \\

    @Override
    protected void update() {

        MenuInstance baseMenu = editorMenuSystem.getBaseMenu();
        CanvasInstance canvas = baseMenu.getCanvas();

        if (canvas == null)
            return;

        float x = canvas.getX();
        float y = canvas.getY();
        float w = canvas.getW();
        float h = canvas.getH();

        if (x != lastX || y != lastY || w != lastW || h != lastH) {
            lastX = x;
            lastY = y;
            lastW = w;
            lastH = h;
            tabManager.setDockRect(x, y, w, h);
        }

        syncContentRects();
    }

    /*
     * Reads each tab's chrome canvas bounds (post-layout) and propagates them
     * to the content window. Skips tabs whose tab window has no composite rect
     * yet (split-frame guard) and tabs whose canvas is still zero-size (awake
     * guard). Change-guarded so resize() only fires when the rect has moved.
     */
    private void syncContentRects() {

        ObjectArrayList<TabHandle> tabs = tabManager.getOpenTabs();
        Object[] elements = tabs.elements();
        int size = tabs.size();

        for (int i = 0; i < size; i++) {

            TabHandle handle = (TabHandle) elements[i];
            TabContext tabContext = handle.getTabContext();
            WindowInstance tabWindow = tabContext.getWindow();

            if (!tabWindow.hasCompositeRect())
                continue;

            MenuInstance chromeMenu = tabContext.getChromeMenu();
            if (chromeMenu == null)
                continue;

            CanvasInstance tabCanvas = chromeMenu.getCanvas();
            if (tabCanvas == null || tabCanvas.getW() <= 0 || tabCanvas.getH() <= 0)
                continue;

            float cx = tabWindow.getCompositeX() + tabCanvas.getX();
            float cy = tabWindow.getCompositeY() + tabCanvas.getY();
            float cw = tabCanvas.getW();
            float ch = tabCanvas.getH();

            WindowInstance contentWindow = handle.getContentContext().getWindow();

            // Skip if content window already holds this exact rect — avoids
            // firing resize() and its downstream cascade every frame.
            if (cx == contentWindow.getCompositeX()
                    && cy == contentWindow.getCompositeY()
                    && cw == contentWindow.getCompositeW()
                    && ch == contentWindow.getCompositeH())
                continue;

            contentWindow.setCompositeRect(cx, cy, cw, ch);
            contentWindow.resize((int) cw, (int) ch);
        }
    }
}