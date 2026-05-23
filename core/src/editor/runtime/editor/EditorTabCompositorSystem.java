package editor.runtime.editor;

import application.bootstrap.menupipeline.canvas.CanvasInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.tabpipeline.tab.TabContext;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class EditorTabCompositorSystem extends SystemPackage {

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
            tabManager.setDockRect(x, y, w, h);
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
            WindowInstance tabWindow = tabContext.getWindow();

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