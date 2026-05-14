package editor.runtime;

import editor.bootstrap.docklayoutsystem.DockLayoutSystem;
import editor.bootstrap.tab.TabContext;
import editor.bootstrap.tab.TabHandle;
import editor.bootstrap.tabmanager.TabManager;
import application.bootstrap.menupipeline.canvas.CanvasInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class EditorTabCompositorSystem extends SystemPackage {

    /*
     * Feeds the dock canvas bounds to DockLayoutSystem each frame, then applies
     * the resulting per-tab rects to each tab context window.
     *
     * Canvas: read directly from the editor menu instance. Null canvas gates the
     * update — the menu writes its own bounds each frame during layout.
     *
     * Active tabs: setCompositeRect + resize so the tab window blits to the
     * correct region and TabContext.onResize pushes bounds down to the content
     * window. The change-detection guard includes !hasCompositeRect() so a tab
     * that was deactivated always fires a resize when it becomes active again —
     * without the guard, a rect matching the zero field defaults would be
     * silently skipped and the tab would stay dark.
     *
     * Inactive tabs: tabContext.deactivate() clears both the tab window and
     * content window composite rects so hasCompositeRect() returns false on
     * both. FboRenderSystem skips any logical window without a composite rect,
     * so neither the chrome FBO nor the content FBO blits while the tab is
     * hidden. Do NOT call resize(0,0) — zero-size resizes cascade through
     * TabContext into the content FBO and corrupt it.
     */

    // Internal
    private TabManager tabManager;
    private EditorMenuSystem editorMenuSystem;
    private DockLayoutSystem dockLayoutSystem;

    // Internal \\

    @Override
    protected void get() {
        this.tabManager = get(TabManager.class);
        this.editorMenuSystem = get(EditorMenuSystem.class);
        this.dockLayoutSystem = get(DockLayoutSystem.class);
    }

    // Update \\

    @Override
    protected void update() {

        MenuInstance editorMenu = editorMenuSystem.getEditorMenu();
        CanvasInstance canvas = editorMenu.getCanvas();

        if (canvas == null)
            return;

        dockLayoutSystem.computeRects(canvas.getX(), canvas.getY(), canvas.getW(), canvas.getH());

        ObjectArrayList<TabHandle> tabs = tabManager.getOpenTabs();

        for (int i = 0; i < tabs.size(); i++) {

            TabHandle handle = tabs.get(i);
            TabContext tabContext = handle.getTabContext();

            if (tabContext == null)
                continue;

            if (!dockLayoutSystem.isTabActive(handle)) {
                tabContext.deactivate();
                continue;
            }

            float tabX = dockLayoutSystem.getTabX(handle);
            float tabY = dockLayoutSystem.getTabY(handle);
            float tabW = dockLayoutSystem.getTabW(handle);
            float tabH = dockLayoutSystem.getTabH(handle);

            WindowInstance tabWindow = tabContext.getWindow();

            if (!tabWindow.hasCompositeRect()
                    || tabWindow.getCompositeX() != tabX
                    || tabWindow.getCompositeY() != tabY
                    || tabWindow.getCompositeW() != tabW
                    || tabWindow.getCompositeH() != tabH) {
                tabWindow.setCompositeRect(tabX, tabY, tabW, tabH);
                tabWindow.resize((int) tabW, (int) tabH);
            }
        }
    }
}