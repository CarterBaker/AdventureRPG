package editor.bootstrap.tabpipeline.layoutmanager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.tabpipeline.docknode.DockNodeStruct;
import editor.bootstrap.tabpipeline.docklayoutsystem.DockLayoutSystem;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import engine.root.ContextPackage;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.io.JsonUtility;
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
     * Restoration flow:
     * 1. Close all currently open tabs.
     * 2. Reset tab counters so titles reproduce deterministically.
     * 3. Open tabs in JSON order via TabManager.openTab().
     * 4. Reconstruct the BSP tree, replacing the auto-built one.
     * 5. pushRects() settles positions.
     *
     * The restoring flag suppresses re-entrant notifyLayoutChanged() calls
     * triggered by openTab/closeTab calls inside restore().
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

        DockNodeStruct bspRoot = dockLayoutSystem.getRoots().get(windowManager.getMainWindow());
        if (bspRoot != null)
            root.add("node", serializeNode(bspRoot, openTabs));

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

            if (root.has("node")) {
                DockNodeStruct restoredRoot = deserializeNode(root.getAsJsonObject("node"), handles);
                dockLayoutSystem.restoreRoot(windowManager.getMainWindow(), restoredRoot);
                tabManager.pushRects();
            }

        } finally {
            restoring = false;
        }
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
        return new File(internal.path,
                EngineSetting.BIN_DIRECTORY + "/" + EngineSetting.EDITOR_LAYOUT_DIRECTORY);
    }

    private File getSessionFile() {
        return new File(getEditorLayoutDir(), EngineSetting.EDITOR_LAYOUT_SESSION_FILE);
    }
}