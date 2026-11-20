package application.bootstrap.menupipeline.hierarchy;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class HierarchyInstance extends InstancePackage {

    /*
     * One open hierarchy panel: its menu, active tab, toggled node keys, and the
     * tab rows and node rows it has injected, with each tab's whole width at
     * full and at the smallest readable size and where its rows end. Needs layout when marked dirty, when its provider's
     * revision moves, or when its width breaks the tab rows elsewhere; a width
     * that only shrinks or widens its tabs resizes them in place.
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
    private float layoutBarWidth;
    private FloatArrayList tabWidths;
    private FloatArrayList tabNarrowest;
    private IntArrayList tabRowEnds;
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
        this.layoutBarWidth = EngineSetting.INDEX_NOT_FOUND;
        this.tabWidths = new FloatArrayList();
        this.tabNarrowest = new FloatArrayList();
        this.tabRowEnds = new IntArrayList();
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

    public boolean needsLayout(int providerRevision) {
        return layoutDirty || layoutRevision != providerRevision;
    }

    public void markLaidOut(int providerRevision) {

        this.layoutDirty = false;
        this.layoutRevision = providerRevision;
    }

    public boolean needsTabResize(float barWidth) {
        return layoutBarWidth != barWidth;
    }

    public void markTabsResized(float barWidth) {
        this.layoutBarWidth = barWidth;
    }

    public FloatArrayList getTabWidths() {
        return tabWidths;
    }

    public FloatArrayList getTabNarrowest() {
        return tabNarrowest;
    }

    public IntArrayList getTabRowEnds() {
        return tabRowEnds;
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
