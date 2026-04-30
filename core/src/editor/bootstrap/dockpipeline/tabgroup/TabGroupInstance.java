package editor.bootstrap.dockpipeline.tabgroup;

import editor.bootstrap.dockpipeline.tab.TabInstance;
import engine.root.InstancePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TabGroupInstance extends InstancePackage {

    /*
     * Owns a list of tabs and tracks which is active.
     * Mirrors how WindowManager owns windows — same list/index pattern.
     * Active tab is the one currently rendering into its FBO.
     */

    // Data
    private TabGroupData data;

    // Tabs
    private ObjectArrayList<TabInstance> tabs;
    private int activeIndex;

    // Constructor \\

    public void constructor(TabGroupData data) {
        this.data = data;
        this.tabs = new ObjectArrayList<>();
        this.activeIndex = 0;
    }

    // Mutators \\

    public void addTab(TabInstance tab) {
        tabs.add(tab);
        if (tabs.size() == 1)
            activeIndex = 0;
    }

    public void removeTab(TabInstance tab) {
        int index = tabs.indexOf(tab);
        if (index == -1)
            return;
        tabs.remove(index);
        if (activeIndex >= tabs.size())
            activeIndex = Math.max(0, tabs.size() - 1);
    }

    public void setActiveIndex(int index) {
        if (tabs.isEmpty())
            return;
        this.activeIndex = Math.max(0, Math.min(index, tabs.size() - 1));
    }

    // Accessible \\

    public TabInstance getActiveTab() {
        if (tabs.isEmpty())
            return null;
        return tabs.get(activeIndex);
    }

    public ObjectArrayList<TabInstance> getTabs() {
        return tabs;
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public TabGroupData getData() {
        return data;
    }

    public int getGroupID() {
        return data.getGroupID();
    }

    public boolean isEmpty() {
        return tabs.isEmpty();
    }
}