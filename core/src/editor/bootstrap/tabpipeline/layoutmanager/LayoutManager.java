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
     * Save format: a "tabs" array (order defines restore order and BSP indices)
     * and a "windows" array where each entry carries isMain, optional screen
     * metadata, and a serialized BSP node tree. Legacy files with a bare "node"
     * key instead of "windows" are still loadable.
     *
     * Restoration flow:
     * 1. Close all currently open tabs.
     * 2. Reset tab counters so titles reproduce deterministically.
     * 3. Open tabs in JSON order via TabManager.openTab() — all land on main.
     * 4. Pass 1: restore the main window BSP via restoreRoot().
     * 5. Pass 2: for each secondary window entry, open an OS window, collect
     * the tab handles referenced in that BSP via collectTabs(), call
     * moveTabToOsWindow() for each (which reparents and registers the
     * window's dockRect entry as a side effect), then commit the BSP via
     * restoreRoot().
     * 6. pushRects() settles all positions.
     *
     * The restoring flag suppresses re-entrant notifyLayoutChanged() calls
     * triggered by openTab/closeTab inside restore().
     *
     * Divider-drag ratio changes are serialized in each BSP node's "ratio"
     * field. The divider drag manager must call tabManager.notifyLayoutChanged()
     * after each drag completes to persist those changes.
     */
    // Internal
    private TabManager tabManager;
    private DockLayoutSystem dockLayoutSystem;
    private WindowManager windowManager;
    // State
    private boolean restoring;

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
            restore(sessionFile);
    }

    // Notification \\
    public void notifyLayoutChanged() {
        if (restoring)
            return;
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
            windowObj.add("node", serializeNode(bspRoot, openTabs));
            windowsArray.add(windowObj);
        }
        root.add("windows", windowsArray);
        try (FileWriter writer = new FileWriter(file)) {
            internal.gson.toJson(root, writer);
        } catch (IOException e) {
            throwException("Failed to write layout file: " + file.getAbsolutePath(), e);
        }
    }

    private JsonObject serializeNode(DockNodeStruct node, ObjectArrayList<TabHandle> openTabs) {
        JsonObject obj = new JsonObject();
        obj.addProperty("split", node.isSplit());
        if (node.isSplit()) {
            obj.addProperty("splitHorizontal", node.isSplitHorizontal());
            obj.addProperty("ratio", node.getRatio());
            obj.add("first", serializeNode(node.getFirst(), openTabs));
            obj.add("second", serializeNode(node.getSecond(), openTabs));
        } else {
            obj.addProperty("activeIndex", node.getActiveIndex());
            JsonArray tabIndices = new JsonArray();
            ObjectArrayList<TabHandle> nodeTabs = node.getTabs();
            for (int i = 0; i < nodeTabs.size(); i++) {
                int idx = openTabs.indexOf(nodeTabs.get(i));
                if (idx >= 0)
                    tabIndices.add(idx);
            }
            obj.add("tabs", tabIndices);
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
        restore(file);
        save(getSessionFile());
    }

    private void restore(File file) {
        restoring = true;
        try {
            JsonObject root = JsonUtility.loadJsonObject(file);
            JsonArray tabsArray = root.getAsJsonArray("tabs");
            if (tabsArray == null || tabsArray.size() == 0)
                return;
            ObjectArrayList<TabHandle> existing = new ObjectArrayList<>(tabManager.getOpenTabs());
            for (int i = 0; i < existing.size(); i++)
                tabManager.closeTab(existing.get(i));
            tabManager.resetCounters();
            TabHandle[] handles = new TabHandle[tabsArray.size()];
            for (int i = 0; i < tabsArray.size(); i++) {
                JsonObject tabObj = tabsArray.get(i).getAsJsonObject();
                String baseTitle = tabObj.get("baseTitle").getAsString();
                String className = tabObj.get("contentClass").getAsString();
                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends ContextPackage> contentClass = (Class<? extends ContextPackage>) Class
                            .forName(className);
                    handles[i] = tabManager.openTab(baseTitle, contentClass);
                } catch (ClassNotFoundException e) {
                    throwException("Layout restore failed — class not found: " + className, e);
                }
            }
            if (root.has("windows")) {
                restoreWindows(root.getAsJsonArray("windows"), handles);
            } else if (root.has("node")) {
                // Legacy single-window format.
                DockNodeStruct restoredRoot = deserializeNode(root.getAsJsonObject("node"), handles);
                dockLayoutSystem.restoreRoot(windowManager.getMainWindow(), restoredRoot);
                tabManager.pushRects();
            }
        } finally {
            restoring = false;
        }
    }

    // Window Restore \\
    /*
     * Two-pass restore so the main window BSP is committed before any secondary
     * window is opened. Pass 1 iterates windowsArray for the main entry and
     * calls restoreRoot. Pass 2 opens each secondary OS window, deserializes
     * its BSP, moves every tab in that BSP to the new window via
     * moveTabToOsWindow (which also registers the dockRect entry), then
     * commits the BSP. pushRects() settles everything at the end.
     */
    private void restoreWindows(JsonArray windowsArray, TabHandle[] handles) {
        for (int i = 0; i < windowsArray.size(); i++) {
            JsonObject windowObj = windowsArray.get(i).getAsJsonObject();
            if (!windowObj.get("isMain").getAsBoolean())
                continue;
            if (!windowObj.has("node"))
                continue;
            DockNodeStruct restoredRoot = deserializeNode(windowObj.getAsJsonObject("node"), handles);
            dockLayoutSystem.restoreRoot(windowManager.getMainWindow(), restoredRoot);
        }
        for (int i = 0; i < windowsArray.size(); i++) {
            JsonObject windowObj = windowsArray.get(i).getAsJsonObject();
            if (windowObj.get("isMain").getAsBoolean())
                continue;
            if (!windowObj.has("node"))
                continue;
            WindowInstance osWindow = windowManager.openWindow(
                    EngineSetting.WINDOW_TITLE_EDITOR_SECONDARY,
                    engine.editor.EditorWindowSecondary.class);
            dockLayoutSystem.initWindow(osWindow);
            DockNodeStruct restoredRoot = deserializeNode(windowObj.getAsJsonObject("node"), handles);
            ObjectArrayList<TabHandle> windowTabs = new ObjectArrayList<>();
            collectTabs(restoredRoot, windowTabs);
            for (int j = 0; j < windowTabs.size(); j++)
                tabManager.moveTabToOsWindow(windowTabs.get(j), osWindow);
            dockLayoutSystem.restoreRoot(osWindow, restoredRoot);
        }
        tabManager.pushRects();
    }

    private void collectTabs(DockNodeStruct node, ObjectArrayList<TabHandle> result) {
        if (node == null)
            return;
        if (!node.isSplit()) {
            ObjectArrayList<TabHandle> nodeTabs = node.getTabs();
            for (int i = 0; i < nodeTabs.size(); i++)
                result.add(nodeTabs.get(i));
            return;
        }
        collectTabs(node.getFirst(), result);
        collectTabs(node.getSecond(), result);
    }

    private DockNodeStruct deserializeNode(JsonObject obj, TabHandle[] handles) {
        DockNodeStruct node = new DockNodeStruct();
        boolean split = obj.get("split").getAsBoolean();
        if (split) {
            node.setSplit(true);
            node.setSplitHorizontal(obj.get("splitHorizontal").getAsBoolean());
            node.setRatio(obj.get("ratio").getAsFloat());
            node.setFirst(deserializeNode(obj.getAsJsonObject("first"), handles));
            node.setSecond(deserializeNode(obj.getAsJsonObject("second"), handles));
            node.setTabs(null);
        } else {
            JsonArray tabIndices = obj.getAsJsonArray("tabs");
            ObjectArrayList<TabHandle> nodeTabs = new ObjectArrayList<>();
            for (int i = 0; i < tabIndices.size(); i++) {
                int idx = tabIndices.get(i).getAsInt();
                if (idx >= 0 && idx < handles.length && handles[idx] != null)
                    nodeTabs.add(handles[idx]);
            }
            node.setTabs(nodeTabs);
            int activeIndex = obj.get("activeIndex").getAsInt();
            node.setActiveIndex(Math.min(activeIndex, Math.max(0, nodeTabs.size() - 1)));
        }
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