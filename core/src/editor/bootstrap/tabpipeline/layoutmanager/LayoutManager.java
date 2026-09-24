package editor.bootstrap.tabpipeline.layoutmanager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.tabpipeline.docknode.DockNodeStruct;
import editor.bootstrap.tabpipeline.docklayoutsystem.DockLayoutSystem;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import engine.root.ContextPackage;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.io.JsonUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class LayoutManager extends ManagerPackage {
    /*
     * Owns layout persistence for the editor tab system.
     *
     * LastSession.json is written on every structural tab change and restored
     * in awake() if it exists. Named layouts are saved to / loaded from
     * editorLayout/<name>.json. listLayouts() scans the directory, excluding
     * the session file.
     *
     * Save format: a "tabs" array — each entry carries a stable "id" (see
     * TabHandle.getTabId()) plus baseTitle and contentClass — and a
     * "windows" array where each entry carries isMain, optional screen
     * metadata, and a serialized BSP node tree whose leaves reference a tab
     * by that same id, never by array position.
     *
     * Referencing tabs by id is what makes restore resilient to a single
     * tab failing to reopen (its content class no longer exists, or throws
     * during construction): deserializeNode() returns null for a leaf whose
     * id isn't in the id→handle map built during the open pass, and the
     * split above it collapses using the exact same rule
     * DockLayoutSystem.pruneTab() uses when a tab closes live — one rule,
     * used identically whether a tab disappears live or during restore.
     *
     * Restoration flow:
     * 1. tabManager.closeAll() — every secondary window and every tab goes,
     * so no window from the previous layout is left behind.
     * 2. Reset tab counters so titles reproduce deterministically.
     * 3. For each saved window: resolve its OS window (the main window, or a
     * new one from tabManager.openSecondaryOsWindow()), open every tab its
     * tree references directly on that window, and commit the rebuilt tree
     * via restoreRoot(). A tab that fails for any reason is logged and
     * skipped rather than aborting the rest of the restore. A secondary
     * window whose tabs all failed closes again through the same
     * tabManager.closeOsWindowIfEmpty() rule used everywhere else.
     * 4. endBatch() settles all positions.
     *
     * The entire restore runs inside tabManager.beginBatch()/endBatch(), and
     * both entry points (awake() and loadLayout()) go through the same
     * restoreSafely() wrapper, so a corrupt or incompatible file logs an
     * error and leaves the editor with no tabs open instead of crashing it
     * — the same defensive posture in both places, not just one.
     *
     * Divider-drag ratio changes are serialized in each BSP node's "ratio"
     * field. TabBranch calls tabManager.notifyLayoutChanged() when each
     * divider drag completes to persist those changes.
     */
    // Internal
    private TabManager tabManager;
    private DockLayoutSystem dockLayoutSystem;
    private WindowManager windowManager;

    // Base \\
    @Override
    protected void get() {
        this.tabManager = get(TabManager.class);
        this.dockLayoutSystem = get(DockLayoutSystem.class);
        this.windowManager = get(WindowManager.class);
    }

    @Override
    protected void awake() {
        File sessionFile = getSessionFile();
        if (sessionFile.exists())
            restoreSafely(sessionFile);
    }

    // Notification \\
    public void notifyLayoutChanged() {
        save(getSessionFile());
    }

    // Save \\
    public void saveLayout(String name) {
        if (name == null || name.trim().isEmpty())
            throwException("Cannot save layout with a null or empty name.");
        save(new File(getEditorLayoutDir(), name.trim() + ".json"));
    }

    private void save(File file) {
        file.getParentFile().mkdirs();
        JsonObject root = new JsonObject();
        ObjectArrayList<TabHandle> openTabs = tabManager.getOpenTabs();
        JsonArray tabsArray = new JsonArray();
        for (int i = 0; i < openTabs.size(); i++) {
            TabHandle handle = openTabs.get(i);
            JsonObject tabObj = new JsonObject();
            tabObj.addProperty("id", handle.getTabId());
            tabObj.addProperty("baseTitle", handle.getTabData().getBaseTitle());
            tabObj.addProperty("contentClass", handle.getTabData().getContentContextClass().getName());
            tabsArray.add(tabObj);
        }
        root.add("tabs", tabsArray);
        JsonArray windowsArray = new JsonArray();
        WindowInstance mainWindow = windowManager.getMainWindow();
        for (Object2ObjectOpenHashMap.Entry<WindowInstance, DockNodeStruct> entry : dockLayoutSystem.getRoots()
                .object2ObjectEntrySet()) {
            WindowInstance w = entry.getKey();
            DockNodeStruct bspRoot = entry.getValue();
            if (bspRoot == null)
                continue;
            JsonObject windowObj = new JsonObject();
            windowObj.addProperty("isMain", w == mainWindow);
            if (w != mainWindow) {
                windowObj.addProperty("screenX", w.getScreenX());
                windowObj.addProperty("screenY", w.getScreenY());
                windowObj.addProperty("width", w.getWidth());
                windowObj.addProperty("height", w.getHeight());
            }
            windowObj.add("node", serializeNode(bspRoot));
            windowsArray.add(windowObj);
        }
        root.add("windows", windowsArray);
        try (FileWriter writer = new FileWriter(file)) {
            internal.gson.toJson(root, writer);
        } catch (IOException e) {
            throwException("Failed to write layout file: " + file.getAbsolutePath(), e);
        }
    }

    private JsonObject serializeNode(DockNodeStruct node) {
        JsonObject obj = new JsonObject();
        obj.addProperty("split", node.isSplit());
        if (node.isSplit()) {
            obj.addProperty("splitHorizontal", node.isSplitHorizontal());
            obj.addProperty("ratio", node.getRatio());
            obj.add("first", serializeNode(node.getFirst()));
            obj.add("second", serializeNode(node.getSecond()));
        } else {
            obj.addProperty("tab", node.getTab().getTabId());
        }
        return obj;
    }

    // Load \\
    public void loadLayout(String name) {
        if (name == null || name.trim().isEmpty())
            throwException("Cannot load layout with a null or empty name.");
        File file = new File(getEditorLayoutDir(), name.trim() + ".json");
        if (!file.exists())
            throwException("Layout not found: " + file.getAbsolutePath());
        if (restoreSafely(file))
            save(getSessionFile());
    }

    /*
     * Attempts a restore and never lets it crash the editor. Both callers —
     * awake() on startup and loadLayout() on explicit user action — go
     * through this exact same wrapper, so a corrupt or schema-incompatible
     * file behaves identically no matter which path triggered the read:
     * logged, skipped, editor keeps running with whatever tabs (if any)
     * had already opened successfully before the failure.
     */
    private boolean restoreSafely(File file) {
        try {
            restore(file);
            return true;
        } catch (RuntimeException e) {
            errorLog("Failed to restore layout from '" + file.getName() + "': " + e.getMessage());
            return false;
        }
    }

    private void restore(File file) {
        tabManager.beginBatch();
        try {
            JsonObject root = JsonUtility.loadJsonObject(file);
            JsonArray tabsArray = root.getAsJsonArray("tabs");
            JsonArray windowsArray = root.getAsJsonArray("windows");
            if (tabsArray == null || tabsArray.size() == 0 || windowsArray == null)
                return;

            tabManager.closeAll();
            tabManager.resetCounters();

            Int2ObjectOpenHashMap<JsonObject> savedId2TabObj = new Int2ObjectOpenHashMap<>();

            for (int i = 0; i < tabsArray.size(); i++) {
                JsonObject tabObj = tabsArray.get(i).getAsJsonObject();
                savedId2TabObj.put(tabObj.get("id").getAsInt(), tabObj);
            }

            for (int i = 0; i < windowsArray.size(); i++)
                restoreWindow(windowsArray.get(i).getAsJsonObject(), savedId2TabObj);

        } finally {
            tabManager.endBatch();
        }
    }

    // Window Restore \\
    /*
     * Restores one saved window. The OS window is resolved first so every tab
     * its tree references opens directly on the window it belongs to; the
     * rebuilt tree then replaces whatever openTab() docked automatically.
     */
    private void restoreWindow(JsonObject windowObj, Int2ObjectOpenHashMap<JsonObject> savedId2TabObj) {

        if (!windowObj.has("node"))
            return;

        WindowInstance osWindow = windowObj.get("isMain").getAsBoolean()
                ? windowManager.getMainWindow()
                : tabManager.openSecondaryOsWindow();

        JsonObject node = windowObj.getAsJsonObject("node");
        Int2ObjectOpenHashMap<TabHandle> restoredById = new Int2ObjectOpenHashMap<>();

        openSavedTabs(node, savedId2TabObj, osWindow, restoredById);
        dockLayoutSystem.restoreRoot(osWindow, deserializeNode(node, restoredById));
        tabManager.closeOsWindowIfEmpty(osWindow);
    }

    private void openSavedTabs(
            JsonObject node,
            Int2ObjectOpenHashMap<JsonObject> savedId2TabObj,
            WindowInstance osWindow,
            Int2ObjectOpenHashMap<TabHandle> restoredById) {

        if (node.get("split").getAsBoolean()) {
            openSavedTabs(node.getAsJsonObject("first"), savedId2TabObj, osWindow, restoredById);
            openSavedTabs(node.getAsJsonObject("second"), savedId2TabObj, osWindow, restoredById);
            return;
        }

        int savedId = node.get("tab").getAsInt();
        JsonObject tabObj = savedId2TabObj.get(savedId);

        if (tabObj == null)
            return;

        TabHandle handle = openSavedTab(tabObj, osWindow);

        if (handle != null)
            restoredById.put(savedId, handle);
    }

    private TabHandle openSavedTab(JsonObject tabObj, WindowInstance osWindow) {

        String baseTitle = tabObj.get("baseTitle").getAsString();
        String className = tabObj.get("contentClass").getAsString();

        try {
            Class<? extends ContextPackage> contentClass = Class.forName(className).asSubclass(ContextPackage.class);
            return tabManager.openTab(baseTitle, contentClass, osWindow);
        } catch (Exception e) {
            errorLog("Layout restore: skipping tab '" + baseTitle + "' (" + className + ") — " + e.getMessage());
            return null;
        }
    }

    /*
     * Rebuilds a BSP subtree from JSON, resolving each leaf's saved tab id
     * against the tabs that actually opened successfully this restore. A
     * leaf whose id has no match returns null. A split node collapses using
     * the exact same rule DockLayoutSystem.pruneTab() uses when a tab
     * closes live — both children gone means this node is gone too, one
     * child gone means the other is promoted in its place.
     */
    private DockNodeStruct deserializeNode(JsonObject obj, Int2ObjectOpenHashMap<TabHandle> restoredById) {

        boolean split = obj.get("split").getAsBoolean();

        if (!split) {
            TabHandle handle = restoredById.get(obj.get("tab").getAsInt());
            if (handle == null)
                return null;
            DockNodeStruct leaf = new DockNodeStruct();
            leaf.setTab(handle);
            return leaf;
        }

        DockNodeStruct first = deserializeNode(obj.getAsJsonObject("first"), restoredById);
        DockNodeStruct second = deserializeNode(obj.getAsJsonObject("second"), restoredById);

        if (first == null && second == null)
            return null;
        if (first == null)
            return second;
        if (second == null)
            return first;

        DockNodeStruct node = new DockNodeStruct();
        node.setSplit(true);
        node.setSplitHorizontal(obj.get("splitHorizontal").getAsBoolean());
        node.setRatio(obj.get("ratio").getAsFloat());
        node.setFirst(first);
        node.setSecond(second);
        return node;
    }

    // List \\
    public ObjectArrayList<String> listLayouts() {
        ObjectArrayList<String> names = new ObjectArrayList<>();
        File dir = getEditorLayoutDir();
        String sessionFile = EngineSetting.EDITOR_LAYOUT_SESSION_FILE;
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json") && !name.equals(sessionFile));
        if (files != null)
            for (File f : files)
                names.add(f.getName().replace(".json", ""));
        return names;
    }

    // Paths \\
    public File getEditorLayoutDir() {
        File dir = new File(internal.path,
                EngineSetting.BIN_DIRECTORY + "/" + EngineSetting.EDITOR_LAYOUT_DIRECTORY);
        dir.mkdirs();
        return dir;
    }

    private File getSessionFile() {
        return new File(getEditorLayoutDir(), EngineSetting.EDITOR_LAYOUT_SESSION_FILE);
    }
}