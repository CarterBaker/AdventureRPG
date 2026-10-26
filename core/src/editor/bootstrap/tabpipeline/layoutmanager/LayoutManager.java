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
     * deleted on request. LayoutSaveBranch captures windows and dock trees, LayoutRestoreBranch
     * rebuilds them, and a layout that cannot be read is logged and skipped
     * without touching the open editor.
     */

    // Internal
    private LayoutSaveBranch layoutSaveBranch;
    private LayoutRestoreBranch layoutRestoreBranch;

    // Directory
    private File layoutDirectory;

    // Session
    private boolean sessionChanged;

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

        if (!sessionChanged)
            return;

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

    public void saveLayout(String layoutName) {

        File layoutFile = getSavedLayoutFile(layoutName);

        if (layoutFile != null)
            layoutSaveBranch.save(layoutFile);
    }

    public void loadLayout(String layoutName) {

        File layoutFile = getSavedLayoutFile(layoutName);

        if (layoutFile != null && restore(layoutFile))
            notifyLayoutChanged();
    }

    public void deleteLayout(String layoutName) {

        File layoutFile = getSavedLayoutFile(layoutName);

        if (layoutFile != null && !layoutFile.delete())
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

    private File getSavedLayoutFile(String layoutName) {

        if (!isLayoutNameValid(layoutName))
            throwException("No layout can exist under the name '" + layoutName + "'.");

        File layoutFile = getLayoutFile(layoutName);

        if (layoutFile.exists())
            return layoutFile;

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
        return isLayoutNameValid(layoutName) && !getLayoutFile(layoutName).exists();
    }

    public ObjectArrayList<String> getLayoutNames() {

        ObjectArrayList<String> layoutNames = new ObjectArrayList<>();
        File[] layoutFiles = layoutDirectory.listFiles();

        if (layoutFiles == null)
            return layoutNames;

        for (File layoutFile : layoutFiles) {

            if (!ArpgUtility.isArpgFile(layoutFile))
                continue;

            String layoutName = FileUtility.getFileName(layoutFile);

            if (!isSessionName(layoutName))
                layoutNames.add(layoutName);
        }

        layoutNames.sort(String.CASE_INSENSITIVE_ORDER);
        return layoutNames;
    }
}
