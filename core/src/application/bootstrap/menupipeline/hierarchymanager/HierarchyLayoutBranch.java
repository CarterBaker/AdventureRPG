package application.bootstrap.menupipeline.hierarchymanager;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.hierarchy.HierarchyInstance;
import application.bootstrap.menupipeline.hierarchy.HierarchyNodeStruct;
import application.bootstrap.menupipeline.hierarchy.HierarchyTabProvider;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValue;
import application.bootstrap.menupipeline.util.DimensionVector2;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class HierarchyLayoutBranch extends BranchPackage {

    /*
     * Lays a hierarchy panel out into its menu: ejects the old tabs and rows,
     * then injects one tab per provider and one indented row per visible node,
     * each carrying its node key as the click argument.
     */

    // Internal
    private MenuManager menuManager;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
    }

    // Layout \\

    void layout(
            HierarchyInstance hierarchy,
            ObjectArrayList<HierarchyTabProvider> providers,
            HierarchyTabProvider activeProvider) {

        MenuInstance menu = hierarchy.getMenu();

        ejectAll(menu, EngineSetting.HIERARCHY_ENTRY_TABS, hierarchy.getTabElements());
        ejectAll(menu, EngineSetting.HIERARCHY_ENTRY_ROWS, hierarchy.getRowElements());

        for (int i = 0; i < providers.size(); i++)
            injectTab(hierarchy, providers.get(i).getTabName());

        if (activeProvider == null)
            return;

        ObjectArrayList<HierarchyNodeStruct> roots = new ObjectArrayList<>();
        activeProvider.buildNodes(roots);
        injectRows(hierarchy, roots, 0);
    }

    private void ejectAll(MenuInstance menu, int entryPoint, ObjectArrayList<ElementInstance> elements) {

        for (int i = 0; i < elements.size(); i++)
            menuManager.eject(menu, entryPoint, elements.get(i));

        elements.clear();
    }

    // Tabs \\

    private void injectTab(HierarchyInstance hierarchy, String tabName) {

        String template = tabName.equals(hierarchy.getActiveTabName())
                ? EngineSetting.HIERARCHY_TAB_ACTIVE_TEMPLATE
                : EngineSetting.HIERARCHY_TAB_TEMPLATE;

        ElementInstance tab = menuManager.inject(
                hierarchy.getMenu(), EngineSetting.HIERARCHY_ENTRY_TABS, template,
                element -> {
                    element.setActionArgOverride(tabName);
                    setChildText(element, EngineSetting.HIERARCHY_ELEMENT_TAB_LABEL, tabName);
                });

        hierarchy.getTabElements().add(tab);
    }

    // Rows \\

    private void injectRows(HierarchyInstance hierarchy, ObjectArrayList<HierarchyNodeStruct> nodes, int depth) {

        for (int i = 0; i < nodes.size(); i++) {

            HierarchyNodeStruct node = nodes.get(i);
            injectRow(hierarchy, node, depth);

            if (node.hasChildren() && hierarchy.isExpanded(node))
                injectRows(hierarchy, node.getChildren(), depth + 1);
        }
    }

    private void injectRow(HierarchyInstance hierarchy, HierarchyNodeStruct node, int depth) {

        String template = node.isSelected()
                ? EngineSetting.HIERARCHY_ROW_SELECTED_TEMPLATE
                : EngineSetting.HIERARCHY_ROW_TEMPLATE;
        float indent = depth * EngineSetting.HIERARCHY_INDENT_PIXELS;

        ElementInstance row = menuManager.inject(
                hierarchy.getMenu(), EngineSetting.HIERARCHY_ENTRY_ROWS, template,
                element -> {
                    element.setActionArgOverride(node.getNodeKey());

                    ElementInstance toggle = element.findChildById(EngineSetting.HIERARCHY_ELEMENT_TOGGLE);

                    if (toggle != null) {
                        toggle.setActionArgOverride(node.getNodeKey());
                        toggle.setPositionOverride(toOffset(indent));
                    }

                    ElementInstance label = element.findChildById(EngineSetting.HIERARCHY_ELEMENT_LABEL);

                    if (label != null)
                        label.setPositionOverride(toOffset(indent + EngineSetting.HIERARCHY_TOGGLE_WIDTH_PIXELS));

                    setChildText(element, EngineSetting.HIERARCHY_ELEMENT_TOGGLE_LABEL, resolveMarker(hierarchy, node));
                    setChildText(element, EngineSetting.HIERARCHY_ELEMENT_LABEL, node.getLabel());
                });

        hierarchy.getRowElements().add(row);
    }

    private String resolveMarker(HierarchyInstance hierarchy, HierarchyNodeStruct node) {

        if (!node.hasChildren())
            return EngineSetting.HIERARCHY_LEAF_MARKER;

        return hierarchy.isExpanded(node)
                ? EngineSetting.HIERARCHY_EXPANDED_MARKER
                : EngineSetting.HIERARCHY_COLLAPSED_MARKER;
    }

    // Utility \\

    private void setChildText(ElementInstance element, String childId, String text) {

        ElementInstance child = element.findChildById(childId);

        if (child != null)
            child.setFontText(text);
    }

    private DimensionVector2 toOffset(float x) {
        return new DimensionVector2(DimensionValue.ofAbsolute(x), DimensionValue.ofAbsolute(0f));
    }
}
