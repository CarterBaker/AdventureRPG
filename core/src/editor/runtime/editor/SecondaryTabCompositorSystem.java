package editor.runtime.editor;

import application.bootstrap.menupipeline.canvas.CanvasInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import editor.bootstrap.tabpipeline.tab.TabContext;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SecondaryTabCompositorSystem extends SystemPackage {

    /*
     * Mirrors EditorTabCompositorSystem exactly for secondary OS windows.
     * See that class for the placeAt/syncContent split rationale.
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