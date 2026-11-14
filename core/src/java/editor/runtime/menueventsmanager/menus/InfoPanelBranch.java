package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InfoPanelBranch extends BranchPackage {

    /*
     * Menu event handlers for the Info Panel. Every action works on the
     * shared selection in InfoManager; row buttons carry the path of the value
     * they act on. A number steps down and up, a choice steps back and forth
     * through what it may name, and either is also typed into the text dialog
     * like every other value; new entries, files, map keys and duplicated map
     * entries are named through the name dialog, and deleting asks for the
     * selection's name to be typed back as confirmation. Copying asks for a new name,
     * then a new value for each unique field, before anything is created.
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

    public void copySelection(WindowInstance window) {

        if (!infoManager.beginCopy())
            return;

        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_COPY_PREFIX + infoManager.getCopySourceName()
                        + EditorSetting.DIALOG_TITLE_COPY_SUFFIX,
                infoManager::isCopyNameValid,
                copyName -> requestCopyValue(
                        window, copyName, infoManager.getCopyUniqueKeys(), new ObjectArrayList<>()));
    }

    private void requestCopyValue(
            WindowInstance window,
            String copyName,
            ObjectArrayList<String> copyKeys,
            ObjectArrayList<String> copyTexts) {

        if (copyTexts.size() == copyKeys.size()) {
            infoManager.copy(copyName, copyTexts);
            return;
        }

        String key = copyKeys.get(copyTexts.size());

        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_UNIQUE_PREFIX + key,
                infoManager.getCopyValueText(key),
                text -> infoManager.isCopyValueValid(key, text),
                text -> {
                    copyTexts.add(text);
                    requestCopyValue(window, copyName, copyKeys, copyTexts);
                });
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

        if (infoManager.getValueType(path) == null)
            return;

        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_EDIT_PREFIX + toFieldLabel(path),
                infoManager.getValueText(path),
                text -> infoManager.isValueValid(path, text),
                text -> infoManager.editValue(path, text));
    }

    public void decreaseValue(String path) {
        infoManager.stepValue(path, -1);
    }

    public void increaseValue(String path) {
        infoManager.stepValue(path, 1);
    }

    public void duplicateValue(String path, WindowInstance window) {

        if (!infoManager.isMapEntry(path)) {
            infoManager.duplicateValue(path);
            return;
        }

        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_DUPLICATE_KEY,
                key -> infoManager.isDuplicateKeyValid(path, key),
                key -> infoManager.duplicateMapEntry(path, key));
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
