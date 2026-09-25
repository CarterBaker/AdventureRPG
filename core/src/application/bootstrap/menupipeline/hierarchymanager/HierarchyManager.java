package application.bootstrap.menupipeline.hierarchymanager;

import application.bootstrap.menupipeline.hierarchy.HierarchyInstance;
import application.bootstrap.menupipeline.hierarchy.HierarchyTabProvider;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class HierarchyManager extends ManagerPackage {

    /*
     * Owns the hierarchy UI. Providers register once and become a tab on every
     * panel, and hear when a panel switches to them. openHierarchy() and
     * closeHierarchy() are the one pair for panels, and each frame any panel
     * whose tab, expansion, or revision moved is laid out again.
     */

    // Internal
    private MenuManager menuManager;
    private HierarchyLayoutBranch hierarchyLayoutBranch;

    // Palette
    private Object2ObjectLinkedOpenHashMap<String, HierarchyTabProvider> tabName2HierarchyTabProvider;

    // Active
    private ObjectArrayList<HierarchyInstance> openHierarchies;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.hierarchyLayoutBranch = create(HierarchyLayoutBranch.class);
        create(HierarchyBranch.class);

        // Palette
        this.tabName2HierarchyTabProvider = new Object2ObjectLinkedOpenHashMap<>();

        // Active
        this.openHierarchies = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        for (int i = 0; i < openHierarchies.size(); i++)
            layoutIfNeeded(openHierarchies.get(i));
    }

    private void layoutIfNeeded(HierarchyInstance hierarchy) {

        HierarchyTabProvider activeProvider = resolveActiveProvider(hierarchy);
        int revision = activeProvider != null ? activeProvider.getRevision() : EngineSetting.INDEX_NOT_FOUND;

        if (!hierarchy.needsLayout(revision))
            return;

        hierarchyLayoutBranch.layout(
                hierarchy,
                new ObjectArrayList<>(tabName2HierarchyTabProvider.values()),
                activeProvider);
        hierarchy.markLaidOut(revision);
    }

    // Management \\

    public void registerTabProvider(HierarchyTabProvider provider) {

        String tabName = provider.getTabName();

        if (tabName2HierarchyTabProvider.containsKey(tabName))
            throwException("Hierarchy tab '" + tabName + "' is already registered.");

        tabName2HierarchyTabProvider.put(tabName, provider);
    }

    public HierarchyInstance openHierarchy(WindowInstance window) {

        MenuInstance menu = menuManager.openMenu(EngineSetting.MENU_HIERARCHY, window);

        HierarchyInstance hierarchy = create(HierarchyInstance.class);
        hierarchy.constructor(menu);
        openHierarchies.add(hierarchy);

        return hierarchy;
    }

    public void closeHierarchy(HierarchyInstance hierarchy) {

        if (hierarchy == null || !openHierarchies.remove(hierarchy))
            return;

        menuManager.closeMenu(hierarchy.getMenu());
    }

    // Events \\

    void selectTab(MenuInstance menu, String tabName) {

        HierarchyInstance hierarchy = getHierarchyForMenu(menu);
        HierarchyTabProvider provider = tabName2HierarchyTabProvider.get(tabName);

        if (hierarchy == null || provider == null)
            return;

        hierarchy.setActiveTabName(tabName);
        provider.selectTab();
    }

    void selectNode(MenuInstance menu, String nodeKey) {

        HierarchyInstance hierarchy = getHierarchyForMenu(menu);

        if (hierarchy == null)
            return;

        HierarchyTabProvider activeProvider = resolveActiveProvider(hierarchy);

        if (activeProvider != null)
            activeProvider.selectNode(nodeKey);
    }

    void toggleNode(MenuInstance menu, String nodeKey) {

        HierarchyInstance hierarchy = getHierarchyForMenu(menu);

        if (hierarchy != null)
            hierarchy.toggleExpanded(nodeKey);
    }

    // Utility \\

    private HierarchyInstance getHierarchyForMenu(MenuInstance menu) {

        for (int i = 0; i < openHierarchies.size(); i++)
            if (openHierarchies.get(i).getMenu() == menu)
                return openHierarchies.get(i);

        return null;
    }

    private HierarchyTabProvider resolveActiveProvider(HierarchyInstance hierarchy) {

        HierarchyTabProvider provider = hierarchy.getActiveTabName() != null
                ? tabName2HierarchyTabProvider.get(hierarchy.getActiveTabName())
                : null;

        if (provider != null || tabName2HierarchyTabProvider.isEmpty())
            return provider;

        provider = tabName2HierarchyTabProvider.values().iterator().next();
        hierarchy.setActiveTabName(provider.getTabName());
        return provider;
    }
}
