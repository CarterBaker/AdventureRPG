package application.bootstrap.menupipeline.hierarchy;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class HierarchyInstance extends InstancePackage {

    /*
     * One open hierarchy panel: its menu, active tab, expanded node keys, and the
     * tab and row elements it has injected. Needs layout when marked dirty or
     * when its provider's revision moves.
     */

    // Menu
    private MenuInstance menu;

    // Tabs
    private String activeTabName;

    // Expansion
    private ObjectOpenHashSet<String> expandedNodeKeys;

    // Layout
    private boolean layoutDirty;
    private int layoutRevision;
    private ObjectArrayList<ElementInstance> tabElements;
    private ObjectArrayList<ElementInstance> rowElements;

    // Constructor \\

    public void constructor(MenuInstance menu) {

        // Menu
        this.menu = menu;

        // Expansion
        this.expandedNodeKeys = new ObjectOpenHashSet<>();

        // Layout
        this.layoutDirty = true;
        this.layoutRevision = EngineSetting.INDEX_NOT_FOUND;
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

    public boolean isExpanded(String nodeKey) {
        return expandedNodeKeys.contains(nodeKey);
    }

    public void toggleExpanded(String nodeKey) {

        if (!expandedNodeKeys.remove(nodeKey))
            expandedNodeKeys.add(nodeKey);

        this.layoutDirty = true;
    }

    // Layout \\

    public boolean needsLayout(int providerRevision) {
        return layoutDirty || layoutRevision != providerRevision;
    }

    public void markLaidOut(int providerRevision) {

        this.layoutDirty = false;
        this.layoutRevision = providerRevision;
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
