package editor.runtime.editor.menueventsmanager.menus;

import application.bootstrap.menupipeline.menu.MenuInstance;
import editor.bootstrap.tab.TabHandle;
import editor.bootstrap.tabmanager.TabManager;
import engine.root.BranchPackage;

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
     */

    private TabManager tabManager;

    @Override
    protected void get() {
        tabManager = get(TabManager.class);
    }

    public void closeTab(MenuInstance menu) {

        TabHandle handle = tabManager.getTabHandleForWindow(menu.getWindow());

        if (handle == null)
            return;

        tabManager.closeTab(handle);
    }
}