package editor.bootstrap.tabpipeline.layoutmanager;

import java.io.File;

import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.tabpipeline.docklayoutsystem.DockLayoutSystem;
import editor.bootstrap.tabpipeline.docknode.DockNodeStruct;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import engine.root.BranchPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class LayoutSaveBranch extends BranchPackage {

    /*
     * Captures the editor's current arrangement as an ARPG layout and writes it to
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

        ArpgObjectStruct layoutArpg = new ArpgObjectStruct();
        layoutArpg.add("tabs", buildTabs());
        layoutArpg.add("windows", buildWindows());

        ArpgUtility.writeObject(layoutFile, layoutArpg);
    }

    // Build \\

    private ArpgArrayStruct buildTabs() {

        ArpgArrayStruct tabsArpg = new ArpgArrayStruct();
        ObjectArrayList<TabHandle> openTabs = tabManager.getOpenTabs();

        for (int i = 0; i < openTabs.size(); i++) {

            TabHandle tabHandle = openTabs.get(i);
            ArpgObjectStruct tabArpg = new ArpgObjectStruct();

            tabArpg.addProperty("id", tabHandle.getTabId());
            tabArpg.addProperty("baseTitle", tabHandle.getTabData().getBaseTitle());
            tabArpg.addProperty("contentClass", tabHandle.getContentContextClass().getName());
            tabsArpg.add(tabArpg);
        }

        return tabsArpg;
    }

    private ArpgArrayStruct buildWindows() {

        ArpgArrayStruct windowsArpg = new ArpgArrayStruct();

        for (Object2ObjectMap.Entry<WindowInstance, DockNodeStruct> entry : dockLayoutSystem.getRoots()
                .object2ObjectEntrySet()) {

            if (entry.getValue() == null)
                continue;

            windowsArpg.add(buildWindow(entry.getKey(), entry.getValue()));
        }

        return windowsArpg;
    }

    private ArpgObjectStruct buildWindow(WindowInstance osWindow, DockNodeStruct root) {

        ArpgObjectStruct windowArpg = new ArpgObjectStruct();
        boolean isMain = osWindow == windowManager.getMainWindow();

        windowArpg.addProperty("isMain", isMain);

        if (!isMain) {
            windowArpg.addProperty("screenX", (int) osWindow.getScreenX());
            windowArpg.addProperty("screenY", (int) osWindow.getScreenY());
            windowArpg.addProperty("width", osWindow.getWidth());
            windowArpg.addProperty("height", osWindow.getHeight());
        }

        windowArpg.add("node", buildNode(root));
        return windowArpg;
    }

    private ArpgObjectStruct buildNode(DockNodeStruct node) {

        ArpgObjectStruct nodeArpg = new ArpgObjectStruct();
        nodeArpg.addProperty("split", node.isSplit());

        if (!node.isSplit()) {
            nodeArpg.addProperty("tab", node.getTab().getTabId());
            return nodeArpg;
        }

        nodeArpg.addProperty("splitHorizontal", node.isSplitHorizontal());
        nodeArpg.addProperty("ratio", node.getRatio());
        nodeArpg.add("first", buildNode(node.getFirst()));
        nodeArpg.add("second", buildNode(node.getSecond()));
        return nodeArpg;
    }
}
