package editor.runtime.editor;

import application.bootstrap.menupipeline.canvas.CanvasInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.tabpipeline.tab.TabContext;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SecondaryTabCompositorSystem extends SystemPackage {

    /*
     * Drives dock rect updates and content rect sync for a secondary OS window.
     * Mirrors EditorTabCompositorSystem exactly — the only differences are:
     *
     * 1. Reads base menu from SecondaryMenuSystem instead of EditorMenuSystem.
     * 2. Passes context.getWindow() (the secondary OS window) to
     * tabManager.setDockRect() so the correct per-window BSP tree is updated.
     * 3. syncContentRects() filters to tabs whose chrome window composites to
     * this secondary OS window only — tabs on other windows are handled by
     * their own compositor instance.
     */

    private TabManager tabManager;
    private SecondaryMenuSystem secondaryMenuSystem;

    private float lastX;
    private float lastY;
    private float lastW;
    private float lastH;

    // Internal \\

    @Override
    protected void get() {
        tabManager = get(TabManager.class);
        secondaryMenuSystem = get(SecondaryMenuSystem.class);
    }

    @Override
    protected void update() {

        MenuInstance baseMenu = secondaryMenuSystem.getBaseMenu();
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

        WindowInstance myOsWindow = context.getWindow();
        ObjectArrayList<TabHandle> tabs = tabManager.getOpenTabs();
        Object[] elements = tabs.elements();
        int size = tabs.size();

        for (int i = 0; i < size; i++) {

            TabHandle handle = (TabHandle) elements[i];
            TabContext tabContext = handle.getTabContext();
            WindowInstance tabWindow = tabContext.getWindow();

            // Only sync tabs that belong to this secondary OS window.
            if (tabWindow.getCompositeTarget() != myOsWindow)
                continue;

            if (!tabWindow.hasCompositeRect())
                continue;

            int targetW = (int) tabWindow.getCompositeW();
            int targetH = (int) tabWindow.getCompositeH();

            if (tabWindow.getWidth() != targetW || tabWindow.getHeight() != targetH) {
                tabWindow.resize(targetW, targetH);
                continue;
            }

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