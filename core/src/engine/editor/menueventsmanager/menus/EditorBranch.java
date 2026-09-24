package engine.editor.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.tabpipeline.layoutmanager.LayoutManager;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import engine.editor.EditorSetting;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class EditorBranch extends BranchPackage {

    /*
     * Menu event handlers for the editor toolbar. Window and tool operations
     * open tabs in the clicked OS window; layout operations list, load, and name
     * saved layouts.
     */

    // Internal
    private MenuManager menuManager;
    private TabManager tabManager;
    private LayoutManager layoutManager;
    private NameDialogBranch nameDialogBranch;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.tabManager = get(TabManager.class);
        this.layoutManager = get(LayoutManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
    }

    // Window Operations \\

    public void openPreview(WindowInstance window) {
        tabManager.openPreview(window.getGLWindow());
    }

    public void openDebug(WindowInstance window) {
        tabManager.openDebug(window.getGLWindow());
    }

    // Tool Operations \\

    public void openHierarchy(WindowInstance window) {
        tabManager.openHierarchy(window.getGLWindow());
    }

    public void openItemEditor(WindowInstance window) {
        tabManager.openItemEditor(window.getGLWindow());
    }

    public void openTextureViewer(WindowInstance window) {
        tabManager.openTextureViewer(window.getGLWindow());
    }

    // Layout Dropdown \\

    public void refreshLayoutList(MenuInstance menu) {

        ElementInstance list = menu.getEntryPoint(EditorSetting.ENTRY_LAYOUTS_LIST);

        if (list == null)
            return;

        ObjectArrayList<ElementInstance> staleItems = new ObjectArrayList<>(list.getChildren());

        for (int i = 0; i < staleItems.size(); i++)
            menuManager.eject(menu, EditorSetting.ENTRY_LAYOUTS_LIST, staleItems.get(i));

        ObjectArrayList<String> layoutNames = layoutManager.getLayoutNames();

        for (int i = 0; i < layoutNames.size(); i++)
            injectLayoutItem(menu, layoutNames.get(i));
    }

    private void injectLayoutItem(MenuInstance menu, String layoutName) {
        menuManager.inject(
                menu, EditorSetting.ENTRY_LAYOUTS_LIST, EditorSetting.MENU_EDITOR_LAYOUT_ITEM_TEMPLATE,
                item -> {
                    item.setActionArgOverride(layoutName);
                    ElementInstance label = item.findChildById(EditorSetting.ELEMENT_LAYOUT_ITEM_LABEL);
                    if (label != null)
                        label.setFontText(layoutName);
                });
    }

    public void loadLayout(String layoutName) {
        layoutManager.loadLayout(layoutName);
    }

    // Create Layout \\

    public void openCreateLayoutDialog(WindowInstance window) {
        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_CREATE_LAYOUT,
                layoutManager::isLayoutNameValid,
                layoutManager::saveLayout);
    }
}
