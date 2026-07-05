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
     * 1. Close all currently open tabs.
     * 2. Reset tab counters so titles reproduce deterministically.
     * 3. Open each saved tab. A tab that fails for any reason is logged and
     * skipped rather than aborting the rest of the restore. Every tab that
     * opens successfully is recorded in an id→handle map.
     * 4. Pass 1: restore the main window BSP via restoreRoot().
     * 5. Pass 2: for each secondary window entry, deserialize its BSP first
     * — if every tab it referenced failed to restore, the whole tree
     * collapses to null and no empty window is ever created for it —
     * otherwise open the secondary OS window via
     * tabManager.openSecondaryOsWindow(), move every surviving tab onto it,
     * and commit the BSP via restoreRoot().
     * 6. pushRects() settles all positions.
     *
     * The entire restore runs inside tabManager.beginBatch()/endBatch(), and
     * both entry points (awake() and loadLayout()) go through the same
     * restoreSafely() wrapper, so a corrupt or incompatible file logs an
     * error and leaves the editor with no tabs open instead of crashing it
     * — the same defensive posture in both places, not just one.
     *
     * Divider-drag ratio changes are serialized in each BSP node's "ratio"
     * field. The divider drag manager must call tabManager.notifyLayoutChanged()
     * after each drag completes to persist those changes.
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
            if (tabsArray == null || tabsArray.size() == 0)
                return;

            ObjectArrayList<TabHandle> existing = new ObjectArrayList<>(tabManager.getOpenTabs());
            for (int i = 0; i < existing.size(); i++)
                tabManager.closeTab(existing.get(i));
            tabManager.resetCounters();

            Int2ObjectOpenHashMap<TabHandle> restoredById = new Int2ObjectOpenHashMap<>();

            for (int i = 0; i < tabsArray.size(); i++) {

                JsonObject tabObj = tabsArray.get(i).getAsJsonObject();
                int savedId = tabObj.get("id").getAsInt();
                String baseTitle = tabObj.get("baseTitle").getAsString();
                String className = tabObj.get("contentClass").getAsString();

                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends ContextPackage> contentClass = (Class<? extends ContextPackage>) Class
                            .forName(className);
                    restoredById.put(savedId, tabManager.openTab(baseTitle, contentClass));
                } catch (Exception e) {
                    errorLog("Layout restore: skipping tab '" + baseTitle
                            + "' (" + className + ") — " + e.getMessage());
                }
            }

            if (root.has("windows"))
                restoreWindows(root.getAsJsonArray("windows"), restoredById);

        } finally {
            tabManager.endBatch();
        }
    }

    // Window Restore \\
    /*
     * Two-pass restore so the main window BSP is committed before any
     * secondary window is opened. Pass 1 restores the main entry's tree
     * directly. Pass 2 deserializes each secondary entry's tree first — a
     * tree that collapses to null (every tab it referenced failed to
     * restore) is discarded before any OS window is opened for it —
     * otherwise opens the OS window via tabManager.openSecondaryOsWindow(),
     * moves every surviving tab onto it, and commits the tree.
     */
    private void restoreWindows(JsonArray windowsArray, Int2ObjectOpenHashMap<TabHandle> restoredById) {

        for (int i = 0; i < windowsArray.size(); i++) {
            JsonObject windowObj = windowsArray.get(i).getAsJsonObject();
            if (!windowObj.get("isMain").getAsBoolean())
                continue;
            if (!windowObj.has("node"))
                continue;
            DockNodeStruct restoredRoot = deserializeNode(windowObj.getAsJsonObject("node"), restoredById);
            dockLayoutSystem.restoreRoot(windowManager.getMainWindow(), restoredRoot);
        }

        for (int i = 0; i < windowsArray.size(); i++) {

            JsonObject windowObj = windowsArray.get(i).getAsJsonObject();

            if (windowObj.get("isMain").getAsBoolean())
                continue;
            if (!windowObj.has("node"))
                continue;

            DockNodeStruct restoredRoot = deserializeNode(windowObj.getAsJsonObject("node"), restoredById);

            if (restoredRoot == null)
                continue;

            WindowInstance osWindow = tabManager.openSecondaryOsWindow();

            ObjectArrayList<TabHandle> windowTabs = new ObjectArrayList<>();
            collectTabs(restoredRoot, windowTabs);

            for (int j = 0; j < windowTabs.size(); j++)
                tabManager.moveTabToOsWindow(windowTabs.get(j), osWindow);

            dockLayoutSystem.restoreRoot(osWindow, restoredRoot);
        }
    }

    private void collectTabs(DockNodeStruct node, ObjectArrayList<TabHandle> result) {
        if (node == null)
            return;
        if (!node.isSplit()) {
            result.add(node.getTab());
            return;
        }
        collectTabs(node.getFirst(), result);
        collectTabs(node.getSecond(), result);
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