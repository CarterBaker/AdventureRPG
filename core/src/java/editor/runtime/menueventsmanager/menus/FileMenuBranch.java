package editor.runtime.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.ThemeColor;
import editor.bootstrap.filemenupipeline.filemenumanager.FileMenuManager;
import editor.bootstrap.filemenupipeline.util.FileMenuAction;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgUtility;

public class FileMenuBranch extends BranchPackage {

    /*
     * Menu event handlers for the editor's File and Edit menus. Opening
     * either names the tool it acts on at its head and mutes every item that
     * tool does not take; an item runs its action on that tool through
     * FileMenuManager, the one path every menu item takes.
     */

    // Internal
    private FileMenuManager fileMenuManager;

    // Colors
    private MenuColorStruct takenColor;
    private MenuColorStruct untakenColor;

    // Base \\

    @Override
    protected void create() {

        // Colors
        this.takenColor = new MenuColorStruct(ThemeColor.TEXT, EngineSetting.COLOR_CHANNEL_MAX);
        this.untakenColor = new MenuColorStruct(ThemeColor.TEXT_MUTED, EngineSetting.COLOR_CHANNEL_MAX);
    }

    @Override
    protected void get() {
        this.fileMenuManager = get(FileMenuManager.class);
    }

    // Dropdowns \\

    public void refreshFileMenu(MenuInstance menu) {
        refresh(menu.getEntryPoint(EditorSetting.ENTRY_FILE_MENU_LIST), EditorSetting.ELEMENT_FILE_MENU_TARGET);
    }

    public void refreshEditMenu(MenuInstance menu) {
        refresh(menu.getEntryPoint(EditorSetting.ENTRY_EDIT_MENU_LIST), EditorSetting.ELEMENT_EDIT_MENU_TARGET);
    }

    private void refresh(ElementInstance list, String targetElement) {

        if (list == null)
            return;

        ElementInstance target = list.findChildById(targetElement);

        if (target != null)
            target.setFontText(fileMenuManager.getTargetTitle());

        for (FileMenuAction action : FileMenuAction.VALUES) {

            ElementInstance label = list.findChildById(EditorSetting.FILE_MENU_LABELS[action.ordinal()]);

            if (label != null)
                label.setColorOverride(fileMenuManager.takes(action) ? takenColor : untakenColor);
        }
    }

    // Actions \\

    public void run(String actionName) {
        fileMenuManager.run(ArpgUtility.toEnum(actionName, FileMenuAction.class));
    }
}
