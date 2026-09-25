package engine.editor.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.infopipeline.util.InfoFieldType;
import engine.editor.EditorSetting;
import engine.root.BranchPackage;

public class InfoPanelBranch extends BranchPackage {

    /*
     * Menu event handlers for the Info Panel. Every action works on the
     * shared selection in InfoManager; row buttons carry the path of the value
     * they act on. Booleans toggle and enums cycle in place, every other value
     * is typed into the text dialog, new entries, files, and map keys are
     * named through the name dialog, and deleting asks for the selection's
     * name to be typed back as confirmation.
     */

    // Internal
    private InfoManager infoManager;
    private NameDialogBranch nameDialogBranch;

    // Base \\

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
    }

    // Files \\

    public void newEntry(WindowInstance window) {

        if (!infoManager.hasActiveSchema())
            return;

        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_NEW_ENTRY,
                infoManager::isNewEntryNameValid,
                infoManager::createEntry);
    }

    public void newFile(WindowInstance window) {

        if (!infoManager.hasActiveSchema())
            return;

        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_NEW_FILE,
                infoManager::isNewFileNameValid,
                infoManager::createFile);
    }

    public void deleteSelection(WindowInstance window) {

        String selectionName = infoManager.getSelectionName();

        if (selectionName == null)
            return;

        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_DELETE_PREFIX + selectionName + EditorSetting.DIALOG_TITLE_DELETE_SUFFIX,
                "",
                infoManager::isSelectionName,
                confirmedName -> infoManager.deleteSelection());
    }

    public void save() {
        infoManager.saveSelection();
    }

    public void saveAll() {
        infoManager.saveAll();
    }

    public void revert() {
        infoManager.revertSelection();
    }

    // Fields \\

    public void editValue(String path, WindowInstance window) {

        InfoFieldType type = infoManager.getValueType(path);

        if (type == null)
            return;

        switch (type) {
            case BOOLEAN -> infoManager.toggleValue(path);
            case ENUM -> infoManager.cycleValue(path);
            default -> nameDialogBranch.openText(
                    window,
                    EditorSetting.DIALOG_TITLE_EDIT_PREFIX + toFieldLabel(path),
                    infoManager.getValueText(path),
                    text -> infoManager.isValueValid(path, text),
                    text -> infoManager.editValue(path, text));
        }
    }

    public void addField(String path) {
        infoManager.addField(path);
    }

    public void removeValue(String path) {
        infoManager.removeValue(path);
    }

    public void addElement(String path, WindowInstance window) {

        if (!infoManager.isMap(path)) {
            infoManager.addElement(path);
            return;
        }

        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_NEW_KEY,
                key -> infoManager.isMapKeyValid(path, key),
                key -> infoManager.addMapEntry(path, key));
    }

    public void toggleGroup(String path) {
        infoManager.toggleExpanded(path);
    }

    // Utility \\

    private String toFieldLabel(String path) {
        return path.substring(path.lastIndexOf(EditorSetting.INFO_PATH_SEPARATOR) + 1);
    }
}
