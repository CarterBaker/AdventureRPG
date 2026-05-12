package editor.runtime;

import application.bootstrap.menupipeline.menumanager.MenuManager;
import editor.bootstrap.docklayoutsystem.DockLayoutSystem;
import editor.bootstrap.tab.TabContext;
import editor.bootstrap.tab.TabHandle;
import editor.bootstrap.tabmanager.TabManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class EditorTabCompositorSystem extends SystemPackage {

    /*
     * Feeds the dock canvas bounds to DockLayoutSystem each frame, then applies
     * the resulting per-tab rects to each tab context window. Inactive tabs in
     * a shared panel receive a zero-size rect so they do not composite.
     */

    // Internal
    private MenuManager menuManager;
    private TabManager tabManager;
    private EditorMenuSystem editorMenuSystem;
    private DockLayoutSystem dockLayoutSystem;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.menuManager = get(MenuManager.class);
        this.tabManager = get(TabManager.class);
        this.editorMenuSystem = get(EditorMenuSystem.class);
        this.dockLayoutSystem = get(DockLayoutSystem.class);
    }

    // Update \\

    @Override
    protected void update() {

        int[] canvas = menuManager.getCanvas(editorMenuSystem.getEditorMenu());

        if (canvas == null)
            return;

        if (canvas.length != EngineSetting.TAB_CANVAS_BOUNDS_LENGTH)
            return;

        float x = canvas[EngineSetting.TAB_CANVAS_X_INDEX];
        float y = canvas[EngineSetting.TAB_CANVAS_Y_INDEX];
        float w = canvas[EngineSetting.TAB_CANVAS_W_INDEX];
        float h = canvas[EngineSetting.TAB_CANVAS_H_INDEX];

        dockLayoutSystem.computeRects(x, y, w, h);

        ObjectArrayList<TabHandle> tabs = tabManager.getOpenTabs();

        for (int i = 0; i < tabs.size(); i++) {

            TabHandle handle = tabs.get(i);
            TabContext tabContext = handle.getTabContext();

            if (tabContext == null)
                continue;

            if (!dockLayoutSystem.isTabActive(handle)) {
                tabContext.getWindow().setCompositeRect(0f, 0f, 0f, 0f);
                tabContext.getWindow().resize(0, 0);
                continue;
            }

            float tabX = dockLayoutSystem.getTabX(handle);
            float tabY = dockLayoutSystem.getTabY(handle);
            float tabW = dockLayoutSystem.getTabW(handle);
            float tabH = dockLayoutSystem.getTabH(handle);

            tabContext.getWindow().setCompositeRect(tabX, tabY, tabW, tabH);
            tabContext.getWindow().resize((int) tabW, (int) tabH);
        }
    }
}