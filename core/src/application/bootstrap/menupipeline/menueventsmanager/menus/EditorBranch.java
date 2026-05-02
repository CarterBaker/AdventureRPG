package application.bootstrap.menupipeline.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import application.runtime.RuntimeContext;
import editor.bootstrap.dockpipeline.container.ContainerInstance;
import editor.bootstrap.dockpipeline.dockmanager.DockManager;
import editor.bootstrap.dockpipeline.tab.TabInstance;
import editor.bootstrap.dockpipeline.tabmanager.TabManager;
import editor.runtime.EditorWindowSecondary;
import engine.root.BranchPackage;

public class EditorBranch extends BranchPackage {

    /*
     * Handles editor menu lifecycle and toolbar button actions.
     *
     * openEditorMenu() — called by EditorMenuSystem on awake, opens the
     * main editor menu against the main window.
     *
     * toggleTestingDropdown() — $parent click handler on the Testing toolbar
     * button. Reads entry point 0 (testing_dropdown) from the live MenuInstance
     * and flips its visibility.
     *
     * openSecondaryWindow() — opens a new tab in the main window's dock
     * container. Dragging the tab outside the window will detach it to a
     * new OS window via DockInputSystem.
     *
     * openPreview() — spawns a new OS window bound to RuntimeContext,
     * running the full game runtime in a separate window.
     */

    // Internal
    private MenuManager menuManager;
    private WindowManager windowManager;
    private DockManager dockManager;
    private TabManager tabManager;

    // State
    private MenuInstance editorMenu;

    // Internal \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.windowManager = get(WindowManager.class);
        this.dockManager = get(DockManager.class);
        this.tabManager = get(TabManager.class);
    }

    // Open / Close \\

    public void openEditorMenu(WindowInstance window) {
        if (editorMenu != null)
            return;
        editorMenu = menuManager.openMenu("EditorWindow/Main", window);
    }

    public void closeEditorMenu() {
        if (editorMenu == null)
            return;
        editorMenu = menuManager.closeMenu(editorMenu);
    }

    // Toolbar Actions \\

    public void toggleTestingDropdown(MenuInstance parent) {
        ElementInstance dropdown = parent.getEntryPoint(0);
        if (dropdown == null)
            return;
        dropdown.toggleExpanded();
    }

    public void openSecondaryWindow() {
        WindowInstance mainWindow = windowManager.getMainWindow();
        ContainerInstance container = dockManager.getContainerForWindow(mainWindow);

        if (container == null || container.getRootNode() == null)
            return;

        TabInstance tab = tabManager.createTab(
                "Editor",
                EditorWindowSecondary.class,
                0, 0,
                mainWindow.getWidth(),
                mainWindow.getHeight() - 28);

        dockManager.addTab(tab, container.getRootNode().getTabGroup());
        tabManager.activateTab(tab, container.getRootNode().getTabGroup());
    }

    public void openPreview() {
        windowManager.openWindow("Preview", RuntimeContext.class);
    }
}
