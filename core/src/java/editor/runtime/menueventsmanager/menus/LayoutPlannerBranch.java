package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.layoutplannerpipeline.layoutplannermanager.LayoutPlannerManager;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;

public class LayoutPlannerBranch extends BranchPackage {

    /*
     * Menu event handlers for the Layout Planner. Palette rows carry a
     * settlement type's or an architecture's name and Properties rows the
     * field's; a value is typed through the name dialog into the same setter
     * the slider and step buttons use. New and Duplicate first open the type
     * designed in the Hierarchy, so the Info Panel's own New and Duplicate
     * make the type, named through the same dialogs; Save and Revert act on
     * its file.
     */

    // Internal
    private LayoutPlannerManager layoutPlannerManager;
    private InfoPanelBranch infoPanelBranch;
    private NameDialogBranch nameDialogBranch;

    // Base \\

    @Override
    protected void get() {
        this.layoutPlannerManager = get(LayoutPlannerManager.class);
        this.infoPanelBranch = get(InfoPanelBranch.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
    }

    // Selection \\

    public void selectRow(String key) {
        layoutPlannerManager.choosePaletteRow(key);
    }

    // Fields \\

    public void decreaseRow(String key) {
        layoutPlannerManager.stepField(key, EditorSetting.LAYOUT_PLANNER_STEP_DOWN);
    }

    public void increaseRow(String key) {
        layoutPlannerManager.stepField(key, EditorSetting.LAYOUT_PLANNER_STEP_UP);
    }

    public void editRow(String key, WindowInstance window) {
        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_EDIT_VALUE,
                layoutPlannerManager.getFieldText(key),
                text -> layoutPlannerManager.isFieldValid(key, text),
                text -> layoutPlannerManager.setField(key, text));
    }

    public void previousRow(String key) {
        layoutPlannerManager.stepField(key, EditorSetting.LAYOUT_PLANNER_STEP_DOWN);
    }

    public void nextRow(String key) {
        layoutPlannerManager.stepField(key, EditorSetting.LAYOUT_PLANNER_STEP_UP);
    }

    // File \\

    public void save() {
        layoutPlannerManager.save();
    }

    public void revert() {
        layoutPlannerManager.revert();
    }

    public void newSettlement(WindowInstance window) {
        if (layoutPlannerManager.focusSettlement())
            infoPanelBranch.newEntry(window);
    }

    public void duplicateSettlement(WindowInstance window) {
        if (layoutPlannerManager.focusSettlement())
            infoPanelBranch.copySelection(window);
    }
}
