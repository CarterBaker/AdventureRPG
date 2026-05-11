// core/src/editor/runtime/menueventsmanager/menus/EditorBranch.java
package editor.runtime.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import editor.bootstrap.tabmanager.TabManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

public class EditorBranch extends BranchPackage {

    private MenuManager menuManager;
    private TabManager tabManager;

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.tabManager = get(TabManager.class);
    }

    public void toggleTestingDropdown(MenuInstance parent) {
        ElementInstance dropdown = parent.getEntryPoint(0);
        if (dropdown == null)
            return;

        dropdown.toggleExpanded();
    }

    public void openPreview() {
        if (tabManager.hasTab(EngineSetting.TAB_TITLE_PREVIEW))
            return;

        tabManager.openPreview();
    }

    public void openSecondaryWindow() {
        tabManager.openSecondaryWindow();
    }
}
