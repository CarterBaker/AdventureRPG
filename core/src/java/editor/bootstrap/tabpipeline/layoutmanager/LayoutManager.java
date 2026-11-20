package editor.bootstrap.tabpipeline.layoutmanager;

import java.io.File;

import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class LayoutManager extends ManagerPackage {

    /*
     * Owns editor layout persistence. The live arrangement is written to the
     * session layout at most once per frame after any structural change and
     * restored on startup; named layouts are created, saved over, loaded and
     * deleted on request. A requested load is rebuilt in this manager's own
     * update, never inside the menu event that asked for it, and the last
     * request in a frame wins. LayoutSaveBranch captures windows and dock
     * trees, LayoutRestoreBranch rebuilds them, and a layout that cannot be
     * read is logged and skipped without touching the open editor. The
     * editor ships task layouts under assets/layouts (Sculpt, Paint, Animate,
     * World), listed beside the user's as a design tool lists its
     * workspaces: saving over one writes the user's own copy, which is the one
     * loaded from then on, and deleting that copy resets it to the one
     * shipped; a shipped layout itself is never written or deleted.
     */

    // Internal
    private LayoutSaveBranch layoutSaveBranch;
    private LayoutRestoreBranch layoutRestoreBranch;

    // Directory — the user's layouts and the ones the editor ships
    private File layoutDirectory;
    private File defaultDirectory;

    // Session
    private boolean sessionChanged;

    // Load
    private File pendingLayoutFile;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.layoutSaveBranch = create(LayoutSaveBranch.class);
        this.layoutRestoreBranch = create(LayoutRestoreBranch.class);

        // Directory
        this.layoutDirectory = new File(
                internal.path,
                EngineSetting.BIN_DIRECTORY + "/" + EngineSetting.EDITOR_LAYOUT_DIRECTORY);
        this.defaultDirectory = new File(EditorSetting.LAYOUT_DEFAULT_PATH);
    }

    @Override
    protected void awake() {

        File sessionFile = getLayoutFile(EditorSetting.LAYOUT_SESSION_NAME);

        if (sessionFile.exists())
            restore(sessionFile);
    }

    // Update \\

    @Override
    protected void update() {

        if (pendingLayoutFile != null)
            restorePendingLayout();

        if (sessionChanged)
            saveSession();
    }

    private void restorePendingLayout() {

        File layoutFile = pendingLayoutFile;
        pendingLayoutFile = null;

        if (restore(layoutFile))
            notifyLayoutChanged();
    }

    private void saveSession() {
        sessionChanged = false;
        layoutSaveBranch.save(getLayoutFile(EditorSetting.LAYOUT_SESSION_NAME));
    }

    // Management \\

    public void notifyLayoutChanged() {
        sessionChanged = true;
    }

    public void createLayout(String layoutName) {

        if (!isLayoutNameAvailable(layoutName))
            throwException("Cannot create a layout under the name '" + layoutName + "'.");

        layoutSaveBranch.save(getLayoutFile(layoutName));
    }

    // Always the user's own file, so saving over a shipped layout keeps the user's copy beside it
    public void saveLayout(String layoutName) {
        if (getSavedLayoutFile(layoutName) != null)
            layoutSaveBranch.save(getLayoutFile(layoutName));
    }

    public void loadLayout(String layoutName) {

        File layoutFile = getSavedLayoutFile(layoutName);

        if (layoutFile != null)
            pendingLayoutFile = layoutFile;
    }

    // The user's own file only: a shipped layout's copy deleted resets it, and the shipped file itself stays
    public void deleteLayout(String layoutName) {

        File layoutFile = getLayoutFile(layoutName);

        if (!layoutFile.exists()) {
            errorLog("Layout '" + layoutName + "' is one the editor ships and holds no changes to reset.");
            return;
        }

        if (!layoutFile.delete())
            errorLog("Layout '" + layoutName + "' could not be deleted: " + layoutFile.getAbsolutePath());
    }

    // Load \\

    private boolean restore(File layoutFile) {

        ArpgObjectStruct layoutArpg = ArpgUtility.tryLoadObject(layoutFile);

        if (layoutArpg != null && layoutRestoreBranch.restore(layoutArpg))
            return true;

        errorLog("Layout '" + FileUtility.getFileName(layoutFile) + "' is unreadable or malformed and was skipped: "
                + layoutFile.getAbsolutePath());
        return false;
    }

    // Utility \\

    private File getLayoutFile(String layoutName) {
        return ArpgUtility.resolveFile(layoutDirectory, layoutName);
    }

    // The user's copy of a layout when there is one, else the one the editor ships, else none
    private File getSavedLayoutFile(String layoutName) {

        if (!isLayoutNameValid(layoutName))
            throwException("No layout can exist under the name '" + layoutName + "'.");

        File layoutFile = getLayoutFile(layoutName);

        if (layoutFile.exists())
            return layoutFile;

        File defaultFile = ArpgUtility.resolveFile(defaultDirectory, layoutName);

        if (defaultFile.exists())
            return defaultFile;

        errorLog("Layout '" + layoutName + "' no longer exists: " + layoutFile.getAbsolutePath());
        return null;
    }

    private boolean isSessionName(String layoutName) {
        return layoutName.equalsIgnoreCase(EditorSetting.LAYOUT_SESSION_NAME);
    }

    // Accessible \\

    public boolean isLayoutNameValid(String layoutName) {
        return FileUtility.isValidFileName(layoutName, EditorSetting.LAYOUT_NAME_MAX_LENGTH)
                && !isSessionName(layoutName);
    }

    public boolean isLayoutNameAvailable(String layoutName) {
        return isLayoutNameValid(layoutName)
                && !getLayoutFile(layoutName).exists()
                && !ArpgUtility.resolveFile(defaultDirectory, layoutName).exists();
    }

    public ObjectArrayList<String> getLayoutNames() {

        ObjectArrayList<String> layoutNames = new ObjectArrayList<>();

        addLayoutNames(defaultDirectory, layoutNames);
        addLayoutNames(layoutDirectory, layoutNames);
        layoutNames.sort(String.CASE_INSENSITIVE_ORDER);
        return layoutNames;
    }

    // Every layout a directory holds, the session's and any already listed left out
    private void addLayoutNames(File directory, ObjectArrayList<String> layoutNames) {

        File[] layoutFiles = directory.listFiles();

        if (layoutFiles == null)
            return;

        for (File layoutFile : layoutFiles) {

            if (!ArpgUtility.isArpgFile(layoutFile))
                continue;

            String layoutName = FileUtility.getFileName(layoutFile);

            if (!isSessionName(layoutName) && !layoutNames.contains(layoutName))
                layoutNames.add(layoutName);
        }
    }
}
