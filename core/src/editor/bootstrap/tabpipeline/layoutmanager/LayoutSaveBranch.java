package editor.bootstrap.tabpipeline.layoutmanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.tabpipeline.docklayoutsystem.DockLayoutSystem;
import editor.bootstrap.tabpipeline.docknode.DockNodeStruct;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import engine.root.BranchPackage;
import engine.util.io.JsonUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class LayoutSaveBranch extends BranchPackage {

    /*
     * Captures the editor's current arrangement as layout JSON and writes it to
     * disk. Every open tab is listed once by id, every OS window records its
     * dock tree with leaves referencing those ids, and secondary windows also
     * record their screen position and size.
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

    void save(File layoutFile) {

        JsonObject layoutJson = new JsonObject();
        layoutJson.add("tabs", buildTabs());
        layoutJson.add("windows", buildWindows());

        JsonUtility.writeJsonObject(layoutFile, layoutJson, internal.gson);
    }

    // Build \\

    private JsonArray buildTabs() {

        JsonArray tabsJson = new JsonArray();
        ObjectArrayList<TabHandle> openTabs = tabManager.getOpenTabs();

        for (int i = 0; i < openTabs.size(); i++) {

            TabHandle tabHandle = openTabs.get(i);
            JsonObject tabJson = new JsonObject();

            tabJson.addProperty("id", tabHandle.getTabId());
            tabJson.addProperty("baseTitle", tabHandle.getTabData().getBaseTitle());
            tabJson.addProperty("contentClass", tabHandle.getContentContextClass().getName());
            tabsJson.add(tabJson);
        }

        return tabsJson;
    }

    private JsonArray buildWindows() {

        JsonArray windowsJson = new JsonArray();

        for (Object2ObjectMap.Entry<WindowInstance, DockNodeStruct> entry : dockLayoutSystem.getRoots()
                .object2ObjectEntrySet()) {

            if (entry.getValue() == null)
                continue;

            windowsJson.add(buildWindow(entry.getKey(), entry.getValue()));
        }

        return windowsJson;
    }

    private JsonObject buildWindow(WindowInstance osWindow, DockNodeStruct root) {

        JsonObject windowJson = new JsonObject();
        boolean isMain = osWindow == windowManager.getMainWindow();

        windowJson.addProperty("isMain", isMain);

        if (!isMain) {
            windowJson.addProperty("screenX", (int) osWindow.getScreenX());
            windowJson.addProperty("screenY", (int) osWindow.getScreenY());
            windowJson.addProperty("width", osWindow.getWidth());
            windowJson.addProperty("height", osWindow.getHeight());
        }

        windowJson.add("node", buildNode(root));
        return windowJson;
    }

    private JsonObject buildNode(DockNodeStruct node) {

        JsonObject nodeJson = new JsonObject();
        nodeJson.addProperty("split", node.isSplit());

        if (!node.isSplit()) {
            nodeJson.addProperty("tab", node.getTab().getTabId());
            return nodeJson;
        }

        nodeJson.addProperty("splitHorizontal", node.isSplitHorizontal());
        nodeJson.addProperty("ratio", node.getRatio());
        nodeJson.add("first", buildNode(node.getFirst()));
        nodeJson.add("second", buildNode(node.getSecond()));
        return nodeJson;
    }
}
