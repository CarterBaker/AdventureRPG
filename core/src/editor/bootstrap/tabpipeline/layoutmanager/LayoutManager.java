package editor.bootstrap.tabpipeline.layoutmanager;

import java.io.File;

import com.google.gson.JsonObject;

import engine.editor.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class LayoutManager extends ManagerPackage {

    /*
     * Owns editor layout persistence. The live arrangement is written to the
     * session layout at most once per frame after any structural change and
     * restored on startup; named layouts are saved and loaded on request.
     * LayoutSaveBranch captures windows and dock trees, LayoutRestoreBranch
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

    public void saveLayout(String layoutName) {

        if (!isLayoutNameValid(layoutName))
            throwException("Cannot save a layout under the name '" + layoutName + "'.");

        layoutSaveBranch.save(getLayoutFile(layoutName));
    }

    public void loadLayout(String layoutName) {

        File layoutFile = getLayoutFile(layoutName);

        if (!layoutFile.exists()) {
            errorLog("Layout '" + layoutName + "' no longer exists: " + layoutFile.getAbsolutePath());
            return;
        }

        if (restore(layoutFile))
            notifyLayoutChanged();
    }

    // Load \\

    private boolean restore(File layoutFile) {

        JsonObject layoutJson = JsonUtility.tryLoadJsonObject(layoutFile);

        if (layoutJson != null && layoutRestoreBranch.restore(layoutJson))
            return true;

        errorLog("Layout '" + FileUtility.getFileName(layoutFile) + "' is unreadable or malformed and was skipped: "
                + layoutFile.getAbsolutePath());
        return false;
    }

    // Utility \\

    private File getLayoutFile(String layoutName) {
        return new File(layoutDirectory, layoutName + "." + EditorSetting.LAYOUT_FILE_EXTENSION);
    }

    private boolean isSessionName(String layoutName) {
        return layoutName.equalsIgnoreCase(EditorSetting.LAYOUT_SESSION_NAME);
    }

    // Accessible \\

    public boolean isLayoutNameValid(String layoutName) {
        return FileUtility.isValidFileName(layoutName, EditorSetting.LAYOUT_NAME_MAX_LENGTH)
                && !isSessionName(layoutName);
    }

    public ObjectArrayList<String> getLayoutNames() {

        ObjectArrayList<String> layoutNames = new ObjectArrayList<>();
        File[] layoutFiles = layoutDirectory.listFiles();

        if (layoutFiles == null)
            return layoutNames;

        for (File layoutFile : layoutFiles) {

            if (!layoutFile.isFile() || !FileUtility.hasExtension(layoutFile, EditorSetting.LAYOUT_FILE_EXTENSION))
                continue;

            String layoutName = FileUtility.getFileName(layoutFile);

            if (!isSessionName(layoutName))
                layoutNames.add(layoutName);
        }

        layoutNames.sort(String.CASE_INSENSITIVE_ORDER);
        return layoutNames;
    }
}
