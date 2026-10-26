package editor.bootstrap.tabpipeline.layoutmanager;

import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.tabpipeline.docklayoutsystem.DockLayoutSystem;
import editor.bootstrap.tabpipeline.docknode.DockNodeStruct;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import engine.root.BranchPackage;
import engine.root.ContextPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class LayoutRestoreBranch extends BranchPackage {

    /*
     * Rebuilds a saved layout. The whole file is validated before anything
     * closes, so a malformed layout leaves the editor untouched. Every tab and
     * secondary window then closes, each saved window reopens at its saved
     * bounds with its tabs opened directly on it, and its dock tree is rebuilt
     * from their ids, each leaf restacked in order and showing the tab it
     * showed. A leaf saved before stacks holds just its one tab. A tab whose
     * content class no longer exists is skipped, and a leaf left with none
     * collapses the split above it through DockLayoutSystem.
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

    boolean restore(ArpgObjectStruct layoutArpg) {

        if (!isLayoutValid(layoutArpg))
            return false;

        Int2ObjectOpenHashMap<ArpgObjectStruct> tabId2TabArpg = mapTabs(ArpgUtility.validateArray(layoutArpg, "tabs"));
        ArpgArrayStruct windowsArpg = ArpgUtility.validateArray(layoutArpg, "windows");

        tabManager.beginBatch();

        try {

            tabManager.closeAll();
            tabManager.resetCounters();

            for (int i = 0; i < windowsArpg.size(); i++)
                restoreWindow(windowsArpg.get(i).getAsObject(), tabId2TabArpg);
        } finally {
            tabManager.endBatch();
        }

        return true;
    }

    // Window \\

    private void restoreWindow(ArpgObjectStruct windowArpg, Int2ObjectOpenHashMap<ArpgObjectStruct> tabId2TabArpg) {

        WindowInstance osWindow = openWindow(windowArpg);
        ArpgObjectStruct nodeArpg = ArpgUtility.validateObject(windowArpg, "node");
        Int2ObjectOpenHashMap<TabHandle> tabId2TabHandle = new Int2ObjectOpenHashMap<>();

        openTabs(nodeArpg, tabId2TabArpg, osWindow, tabId2TabHandle);
        dockLayoutSystem.restoreRoot(osWindow, buildNode(nodeArpg, tabId2TabHandle));
        tabManager.closeOsWindowIfEmpty(osWindow);
    }

    private WindowInstance openWindow(ArpgObjectStruct windowArpg) {

        if (ArpgUtility.validateBoolean(windowArpg, "isMain"))
            return windowManager.getMainWindow();

        WindowInstance osWindow = tabManager.openSecondaryOsWindow();
        windowManager.placeOsWindow(
                osWindow,
                ArpgUtility.validateInt(windowArpg, "screenX"),
                ArpgUtility.validateInt(windowArpg, "screenY"),
                ArpgUtility.validateInt(windowArpg, "width"),
                ArpgUtility.validateInt(windowArpg, "height"));

        return osWindow;
    }

    // Tabs \\

    private Int2ObjectOpenHashMap<ArpgObjectStruct> mapTabs(ArpgArrayStruct tabsArpg) {

        Int2ObjectOpenHashMap<ArpgObjectStruct> tabId2TabArpg = new Int2ObjectOpenHashMap<>();

        for (int i = 0; i < tabsArpg.size(); i++) {
            ArpgObjectStruct tabArpg = tabsArpg.get(i).getAsObject();
            tabId2TabArpg.put(ArpgUtility.validateInt(tabArpg, "id"), tabArpg);
        }

        return tabId2TabArpg;
    }

    private void openTabs(
            ArpgObjectStruct nodeArpg,
            Int2ObjectOpenHashMap<ArpgObjectStruct> tabId2TabArpg,
            WindowInstance osWindow,
            Int2ObjectOpenHashMap<TabHandle> tabId2TabHandle) {

        if (ArpgUtility.validateBoolean(nodeArpg, "split")) {
            openTabs(ArpgUtility.validateObject(nodeArpg, "first"), tabId2TabArpg, osWindow, tabId2TabHandle);
            openTabs(ArpgUtility.validateObject(nodeArpg, "second"), tabId2TabArpg, osWindow, tabId2TabHandle);
            return;
        }

        ArpgArrayStruct stackArpg = readStack(nodeArpg);

        for (int i = 0; i < stackArpg.size(); i++)
            openStackedTab(stackArpg.get(i).getAsInt(), tabId2TabArpg, osWindow, tabId2TabHandle);
    }

    private void openStackedTab(
            int tabId,
            Int2ObjectOpenHashMap<ArpgObjectStruct> tabId2TabArpg,
            WindowInstance osWindow,
            Int2ObjectOpenHashMap<TabHandle> tabId2TabHandle) {

        ArpgObjectStruct tabArpg = tabId2TabArpg.get(tabId);

        if (tabArpg == null)
            return;

        TabHandle tabHandle = openTab(tabArpg, osWindow);

        if (tabHandle != null)
            tabId2TabHandle.put(tabId, tabHandle);
    }

    private TabHandle openTab(ArpgObjectStruct tabArpg, WindowInstance osWindow) {

        String baseTitle = ArpgUtility.validateString(tabArpg, "baseTitle");
        String contentClassName = ArpgUtility.validateString(tabArpg, "contentClass");
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

    private DockNodeStruct buildNode(ArpgObjectStruct nodeArpg, Int2ObjectOpenHashMap<TabHandle> tabId2TabHandle) {

        if (!ArpgUtility.validateBoolean(nodeArpg, "split"))
            return buildLeaf(nodeArpg, tabId2TabHandle);

        return dockLayoutSystem.createSplit(
                buildNode(ArpgUtility.validateObject(nodeArpg, "first"), tabId2TabHandle),
                buildNode(ArpgUtility.validateObject(nodeArpg, "second"), tabId2TabHandle),
                ArpgUtility.validateBoolean(nodeArpg, "splitHorizontal"),
                ArpgUtility.validateFloat(nodeArpg, "ratio"));
    }

    private DockNodeStruct buildLeaf(ArpgObjectStruct nodeArpg, Int2ObjectOpenHashMap<TabHandle> tabId2TabHandle) {

        ArpgArrayStruct stackArpg = readStack(nodeArpg);
        DockNodeStruct leaf = new DockNodeStruct();

        for (int i = 0; i < stackArpg.size(); i++) {

            TabHandle tabHandle = tabId2TabHandle.get(stackArpg.get(i).getAsInt());

            if (tabHandle != null)
                dockLayoutSystem.stackTab(leaf, tabHandle);
        }

        if (leaf.getTabs().isEmpty())
            return null;

        TabHandle activeTab = tabId2TabHandle.get(ArpgUtility.validateInt(nodeArpg, "tab"));

        if (activeTab != null)
            dockLayoutSystem.activateTab(leaf, activeTab);

        return leaf;
    }

    // A leaf's stack in strip order, or for a leaf saved before stacks the one tab it held
    private ArpgArrayStruct readStack(ArpgObjectStruct nodeArpg) {

        if (nodeArpg.has("tabs"))
            return ArpgUtility.validateArray(nodeArpg, "tabs");

        ArpgArrayStruct stackArpg = new ArpgArrayStruct();
        stackArpg.add(ArpgUtility.validateInt(nodeArpg, "tab"));

        return stackArpg;
    }

    // Validation \\

    private boolean isLayoutValid(ArpgObjectStruct layoutArpg) {

        if (!ArpgUtility.hasArray(layoutArpg, "tabs") || !ArpgUtility.hasArray(layoutArpg, "windows"))
            return false;

        for (ArpgElementStruct tabElement : layoutArpg.getAsArray("tabs"))
            if (!tabElement.isObject() || !isTabValid(tabElement.getAsObject()))
                return false;

        for (ArpgElementStruct windowElement : layoutArpg.getAsArray("windows"))
            if (!windowElement.isObject() || !isWindowValid(windowElement.getAsObject()))
                return false;

        return true;
    }

    private boolean isTabValid(ArpgObjectStruct tabArpg) {
        return ArpgUtility.hasNumber(tabArpg, "id")
                && ArpgUtility.hasString(tabArpg, "baseTitle")
                && ArpgUtility.hasString(tabArpg, "contentClass");
    }

    private boolean isWindowValid(ArpgObjectStruct windowArpg) {

        if (!ArpgUtility.hasBoolean(windowArpg, "isMain") || !ArpgUtility.hasObject(windowArpg, "node"))
            return false;

        boolean hasBounds = windowArpg.get("isMain").getAsBoolean()
                || (ArpgUtility.hasNumber(windowArpg, "screenX")
                        && ArpgUtility.hasNumber(windowArpg, "screenY")
                        && ArpgUtility.hasNumber(windowArpg, "width")
                        && ArpgUtility.hasNumber(windowArpg, "height"));

        return hasBounds && isNodeValid(windowArpg.getAsObject("node"));
    }

    private boolean isNodeValid(ArpgObjectStruct nodeArpg) {

        if (!ArpgUtility.hasBoolean(nodeArpg, "split"))
            return false;

        if (!nodeArpg.get("split").getAsBoolean())
            return ArpgUtility.hasNumber(nodeArpg, "tab") && isStackValid(nodeArpg);

        return ArpgUtility.hasBoolean(nodeArpg, "splitHorizontal")
                && ArpgUtility.hasNumber(nodeArpg, "ratio")
                && ArpgUtility.hasObject(nodeArpg, "first")
                && ArpgUtility.hasObject(nodeArpg, "second")
                && isNodeValid(nodeArpg.getAsObject("first"))
                && isNodeValid(nodeArpg.getAsObject("second"));
    }

    // A stack lists whole tab ids, the shown tab among them
    private boolean isStackValid(ArpgObjectStruct nodeArpg) {

        if (!nodeArpg.has("tabs"))
            return true;

        if (!ArpgUtility.hasArray(nodeArpg, "tabs"))
            return false;

        int tabId = nodeArpg.get("tab").getAsInt();
        boolean holdsTab = false;

        for (ArpgElementStruct tabElement : nodeArpg.getAsArray("tabs")) {

            if (!tabElement.isValue() || !tabElement.getAsValue().isInteger())
                return false;

            holdsTab |= tabElement.getAsInt() == tabId;
        }

        return holdsTab;
    }
}
