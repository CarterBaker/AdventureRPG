package application.bootstrap.menupipeline.hierarchy;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class HierarchyInstance extends InstancePackage {

    /*
     * One open hierarchy panel: its menu, active tab, toggled node keys, and the
     * tab rows and node rows it has injected. Needs layout when marked dirty,
     * when its provider's revision moves, or when its width fits a different
     * number of tabs per row.
     */

    // Menu
    private MenuInstance menu;

    // Tabs
    private String activeTabName;

    // Expansion
    private ObjectOpenHashSet<String> toggledNodeKeys;

    // Layout
    private boolean layoutDirty;
    private int layoutRevision;
    private int layoutTabsPerRow;
    private ObjectArrayList<ElementInstance> tabElements;
    private ObjectArrayList<ElementInstance> rowElements;

    // Constructor \\

    public void constructor(MenuInstance menu) {

        // Menu
        this.menu = menu;

        // Expansion
        this.toggledNodeKeys = new ObjectOpenHashSet<>();

        // Layout
        this.layoutDirty = true;
        this.layoutRevision = EngineSetting.INDEX_NOT_FOUND;
        this.layoutTabsPerRow = EngineSetting.INDEX_NOT_FOUND;
        this.tabElements = new ObjectArrayList<>();
        this.rowElements = new ObjectArrayList<>();
    }

    // Tabs \\

    public String getActiveTabName() {
        return activeTabName;
    }

    public void setActiveTabName(String activeTabName) {

        this.activeTabName = activeTabName;
        this.layoutDirty = true;
    }

    // Expansion \\

    public boolean isExpanded(HierarchyNodeStruct node) {
        return node.isExpandedByDefault() != toggledNodeKeys.contains(node.getNodeKey());
    }

    public void toggleExpanded(String nodeKey) {

        if (!toggledNodeKeys.remove(nodeKey))
            toggledNodeKeys.add(nodeKey);

        this.layoutDirty = true;
    }

    // Layout \\

    public boolean needsLayout(int providerRevision, int tabsPerRow) {
        return layoutDirty || layoutRevision != providerRevision || layoutTabsPerRow != tabsPerRow;
    }

    public void markLaidOut(int providerRevision, int tabsPerRow) {

        this.layoutDirty = false;
        this.layoutRevision = providerRevision;
        this.layoutTabsPerRow = tabsPerRow;
    }

    public ObjectArrayList<ElementInstance> getTabElements() {
        return tabElements;
    }

    public ObjectArrayList<ElementInstance> getRowElements() {
        return rowElements;
    }

    // Accessible \\

    public MenuInstance getMenu() {
        return menu;
    }
}
