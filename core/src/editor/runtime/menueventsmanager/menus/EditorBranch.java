// core/src/editor/runtime/menueventsmanager/menus/EditorBranch.java
package editor.runtime.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.tab.TabContext;
import engine.root.BranchPackage;
import engine.root.ContextPackage;

public class EditorBranch extends BranchPackage {

    private MenuManager menuManager;
    private WindowManager windowManager;
    private MenuInstance editorMenu;

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.windowManager = get(WindowManager.class);
    }

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

    public void toggleTestingDropdown(MenuInstance parent) {
        ElementInstance dropdown = parent.getEntryPoint(0);
        if (dropdown == null)
            return;

        dropdown.toggleExpanded();
    }

    public void openTab(String title, Class<? extends ContextPackage> contextClass) {
        WindowInstance mainWindow = windowManager.getMainWindow();
        WindowInstance tabWindow = windowManager.createLogicalWindow(title, mainWindow);
        TabContext tabContext = internal.createTabContext(TabContext.class, tabWindow);
        WindowInstance contentWindow = windowManager.createLogicalWindow(title, mainWindow);
        ContextPackage contentContext = internal.createChildContext(tabContext, contextClass, contentWindow);
        tabContext.mountContent(contentWindow, contentContext);
    }
}
