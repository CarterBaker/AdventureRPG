package editor.bootstrap.tabpipeline.layoutmanager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.tabpipeline.docklayoutsystem.DockLayoutSystem;
import editor.bootstrap.tabpipeline.docknode.DockNodeStruct;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import engine.root.BranchPackage;
import engine.root.ContextPackage;
import engine.util.io.JsonUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class LayoutRestoreBranch extends BranchPackage {

    /*
     * Rebuilds a saved layout. The whole file is validated before anything
     * closes, so a malformed layout leaves the editor untouched. Every tab and
     * secondary window then closes, each saved window reopens at its saved
     * bounds with its tabs opened directly on it, and its dock tree is rebuilt
     * from their ids. A tab whose content class no longer exists is skipped and
     * the split above it collapses through DockLayoutSystem.
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

    // Management \\

    boolean restore(JsonObject layoutJson) {

        if (!isLayoutValid(layoutJson))
            return false;

        Int2ObjectOpenHashMap<JsonObject> tabId2TabJson = mapTabs(JsonUtility.validateArray(layoutJson, "tabs"));
        JsonArray windowsJson = JsonUtility.validateArray(layoutJson, "windows");

        tabManager.beginBatch();

        try {

            tabManager.closeAll();
            tabManager.resetCounters();

            for (int i = 0; i < windowsJson.size(); i++)
                restoreWindow(windowsJson.get(i).getAsJsonObject(), tabId2TabJson);
        } finally {
            tabManager.endBatch();
        }

        return true;
    }

    // Window \\

    private void restoreWindow(JsonObject windowJson, Int2ObjectOpenHashMap<JsonObject> tabId2TabJson) {

        WindowInstance osWindow = openWindow(windowJson);
        JsonObject nodeJson = JsonUtility.validateObject(windowJson, "node");
        Int2ObjectOpenHashMap<TabHandle> tabId2TabHandle = new Int2ObjectOpenHashMap<>();

        openTabs(nodeJson, tabId2TabJson, osWindow, tabId2TabHandle);
        dockLayoutSystem.restoreRoot(osWindow, buildNode(nodeJson, tabId2TabHandle));
        tabManager.closeOsWindowIfEmpty(osWindow);
    }

    private WindowInstance openWindow(JsonObject windowJson) {

        if (JsonUtility.validateBoolean(windowJson, "isMain"))
            return windowManager.getMainWindow();

        WindowInstance osWindow = tabManager.openSecondaryOsWindow();
        windowManager.placeOsWindow(
                osWindow,
                JsonUtility.validateInt(windowJson, "screenX"),
                JsonUtility.validateInt(windowJson, "screenY"),
                JsonUtility.validateInt(windowJson, "width"),
                JsonUtility.validateInt(windowJson, "height"));

        return osWindow;
    }

    // Tabs \\

    private Int2ObjectOpenHashMap<JsonObject> mapTabs(JsonArray tabsJson) {

        Int2ObjectOpenHashMap<JsonObject> tabId2TabJson = new Int2ObjectOpenHashMap<>();

        for (int i = 0; i < tabsJson.size(); i++) {
            JsonObject tabJson = tabsJson.get(i).getAsJsonObject();
            tabId2TabJson.put(JsonUtility.validateInt(tabJson, "id"), tabJson);
        }

        return tabId2TabJson;
    }

    private void openTabs(
            JsonObject nodeJson,
            Int2ObjectOpenHashMap<JsonObject> tabId2TabJson,
            WindowInstance osWindow,
            Int2ObjectOpenHashMap<TabHandle> tabId2TabHandle) {

        if (JsonUtility.validateBoolean(nodeJson, "split")) {
            openTabs(JsonUtility.validateObject(nodeJson, "first"), tabId2TabJson, osWindow, tabId2TabHandle);
            openTabs(JsonUtility.validateObject(nodeJson, "second"), tabId2TabJson, osWindow, tabId2TabHandle);
            return;
        }

        int tabId = JsonUtility.validateInt(nodeJson, "tab");
        JsonObject tabJson = tabId2TabJson.get(tabId);

        if (tabJson == null)
            return;

        TabHandle tabHandle = openTab(tabJson, osWindow);

        if (tabHandle != null)
            tabId2TabHandle.put(tabId, tabHandle);
    }

    private TabHandle openTab(JsonObject tabJson, WindowInstance osWindow) {

        String baseTitle = JsonUtility.validateString(tabJson, "baseTitle");
        String contentClassName = JsonUtility.validateString(tabJson, "contentClass");
        Class<? extends ContextPackage> contentClass = resolveContentClass(contentClassName);

        if (contentClass == null) {
            errorLog("Layout restore skipped tab '" + baseTitle + "': '" + contentClassName
                    + "' is not a context class.");
            return null;
        }

        return tabManager.openTab(baseTitle, contentClass, osWindow);
    }

    private Class<? extends ContextPackage> resolveContentClass(String contentClassName) {

        try {
            Class<?> contentClass = Class.forName(contentClassName);
            return ContextPackage.class.isAssignableFrom(contentClass)
                    ? contentClass.asSubclass(ContextPackage.class)
                    : null;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    // Build \\

    private DockNodeStruct buildNode(JsonObject nodeJson, Int2ObjectOpenHashMap<TabHandle> tabId2TabHandle) {

        if (!JsonUtility.validateBoolean(nodeJson, "split")) {
            TabHandle tabHandle = tabId2TabHandle.get(JsonUtility.validateInt(nodeJson, "tab"));
            return tabHandle != null ? dockLayoutSystem.createLeaf(tabHandle) : null;
        }

        return dockLayoutSystem.createSplit(
                buildNode(JsonUtility.validateObject(nodeJson, "first"), tabId2TabHandle),
                buildNode(JsonUtility.validateObject(nodeJson, "second"), tabId2TabHandle),
                JsonUtility.validateBoolean(nodeJson, "splitHorizontal"),
                JsonUtility.validateFloat(nodeJson, "ratio"));
    }

    // Validation \\

    private boolean isLayoutValid(JsonObject layoutJson) {

        if (!JsonUtility.hasArray(layoutJson, "tabs") || !JsonUtility.hasArray(layoutJson, "windows"))
            return false;

        for (JsonElement tabElement : layoutJson.getAsJsonArray("tabs"))
            if (!tabElement.isJsonObject() || !isTabValid(tabElement.getAsJsonObject()))
                return false;

        for (JsonElement windowElement : layoutJson.getAsJsonArray("windows"))
            if (!windowElement.isJsonObject() || !isWindowValid(windowElement.getAsJsonObject()))
                return false;

        return true;
    }

    private boolean isTabValid(JsonObject tabJson) {
        return JsonUtility.hasNumber(tabJson, "id")
                && JsonUtility.hasString(tabJson, "baseTitle")
                && JsonUtility.hasString(tabJson, "contentClass");
    }

    private boolean isWindowValid(JsonObject windowJson) {

        if (!JsonUtility.hasBoolean(windowJson, "isMain") || !JsonUtility.hasObject(windowJson, "node"))
            return false;

        boolean hasBounds = windowJson.get("isMain").getAsBoolean()
                || (JsonUtility.hasNumber(windowJson, "screenX")
                        && JsonUtility.hasNumber(windowJson, "screenY")
                        && JsonUtility.hasNumber(windowJson, "width")
                        && JsonUtility.hasNumber(windowJson, "height"));

        return hasBounds && isNodeValid(windowJson.getAsJsonObject("node"));
    }

    private boolean isNodeValid(JsonObject nodeJson) {

        if (!JsonUtility.hasBoolean(nodeJson, "split"))
            return false;

        if (!nodeJson.get("split").getAsBoolean())
            return JsonUtility.hasNumber(nodeJson, "tab");

        return JsonUtility.hasBoolean(nodeJson, "splitHorizontal")
                && JsonUtility.hasNumber(nodeJson, "ratio")
                && JsonUtility.hasObject(nodeJson, "first")
                && JsonUtility.hasObject(nodeJson, "second")
                && isNodeValid(nodeJson.getAsJsonObject("first"))
                && isNodeValid(nodeJson.getAsJsonObject("second"));
    }
}
