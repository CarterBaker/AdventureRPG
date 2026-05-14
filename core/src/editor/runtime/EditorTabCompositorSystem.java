package editor.runtime;

import application.bootstrap.menupipeline.canvas.CanvasInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
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
     * TabManager via setDockRect(). TabManager owns full rect propagation.
     * Only fires when bounds actually change.
     *
     * 2. First-frame flush — on the frame after a tab is opened, TabContext.awake()
     * has now run and chromeMenu is valid, but the content window has no
     * composite rect because the pushRects() call inside openTab() fired before
     * awake(). Any tab window that has a composite rect while its content window
     * does not gets a resize() re-fire to flush TabContext.onResize() now that
     * the canvas is ready. This fires exactly once per tab — content has a rect
     * from that point forward so the condition never triggers again.
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

        // Internal
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

        flushPendingContentRects();
    }

    private void flushPendingContentRects() {

        ObjectArrayList<TabHandle> tabs = tabManager.getOpenTabs();
        Object[] elements = tabs.elements();
        int size = tabs.size();

        for (int i = 0; i < size; i++) {

            TabHandle handle = (TabHandle) elements[i];
            TabContext tabContext = handle.getTabContext();

            if (!tabContext.getWindow().hasCompositeRect())
                continue;

            if (handle.getContentContext().getWindow().hasCompositeRect())
                continue;

            tabContext.getWindow().resize(
                    (int) tabContext.getWindow().getCompositeW(),
                    (int) tabContext.getWindow().getCompositeH());
        }
    }
}