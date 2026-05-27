package editor.runtime.editor;

import application.bootstrap.menupipeline.canvas.CanvasInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import editor.bootstrap.tabpipeline.tab.TabContext;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class EditorTabCompositorSystem extends SystemPackage {

    /*
     * Drives dock rect updates and per-frame content sync for the main OS window.
     *
     * setDockRect() triggers pushRects() which calls placeAt() on every chrome
     * window. syncContent() is then called every frame on every tab whose chrome
     * composites to this OS window — it reads the canvas bounds that
     * MenuRenderSystem just wrote and pushes them to the content window.
     *
     * The two-call split matters: placeAt() runs on structural changes,
     * syncContent() runs every frame so content placement always uses the
     * current frame's canvas bounds rather than stale ones.
     */

    private TabManager tabManager;
    private EditorMenuSystem editorMenuSystem;

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
            tabManager.setDockRect(context.getWindow(), x, y, w, h);
        }

        syncContentRects();
    }

    // Sync \\

    private void syncContentRects() {

        ObjectArrayList<TabHandle> tabs = tabManager.getOpenTabs();
        Object[] elements = tabs.elements();
        int size = tabs.size();

        for (int i = 0; i < size; i++) {

            TabHandle handle = (TabHandle) elements[i];
            TabContext tabContext = handle.getTabContext();

            if (tabContext.getWindow().getCompositeTarget() != context.getWindow())
                continue;

            tabContext.syncContent();
        }
    }
}