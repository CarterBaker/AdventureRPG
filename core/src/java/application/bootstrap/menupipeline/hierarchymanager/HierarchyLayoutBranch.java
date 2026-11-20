package application.bootstrap.menupipeline.hierarchymanager;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.hierarchy.HierarchyInstance;
import application.bootstrap.menupipeline.hierarchy.HierarchyNodeStruct;
import application.bootstrap.menupipeline.hierarchy.HierarchyTabProvider;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.StripLayoutUtility;
import application.bootstrap.menupipeline.util.ThemeColor;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class HierarchyLayoutBranch extends BranchPackage {

    /*
     * Lays a hierarchy panel out into its menu: ejects the old tabs and rows,
     * then injects one tab per provider and one indented row per visible node,
     * each carrying its node key as the click argument. Each tab is as wide as
     * its whole name, the widest shrinking first as the panel narrows, then
     * the tabs wrap onto as many rows as its width needs (StripLayoutUtility),
     * so every tab stays visible; the tab bar
     * grows to hold them and the node list shrinks beneath it. A tab or node
     * holding a problem has its label drawn in the danger color.
     */

    // Internal
    private MenuManager menuManager;

    // Colors
    private MenuColorStruct problemColor;

    // Base \\

    @Override
    protected void create() {

        // Colors
        this.problemColor = new MenuColorStruct(ThemeColor.DANGER, EngineSetting.COLOR_CHANNEL_MAX);
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
    }

    // Layout \\

    void layout(
            HierarchyInstance hierarchy,
            ObjectArrayList<HierarchyTabProvider> providers,
            HierarchyTabProvider activeProvider,
            float barWidth) {

        MenuInstance menu = hierarchy.getMenu();

        ejectAll(menu, EngineSetting.HIERARCHY_ENTRY_TABS, hierarchy.getTabElements());
        ejectAll(menu, EngineSetting.HIERARCHY_ENTRY_ROWS, hierarchy.getRowElements());

        injectTabs(hierarchy, providers, barWidth);
        resizeTabBar(menu, Math.max(1, hierarchy.getTabElements().size()));

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

    float resolveTabBarWidth(HierarchyInstance hierarchy) {
        return hierarchy.getMenu().getWindow().getWidth() - 2f * EngineSetting.HIERARCHY_TAB_BAR_PADDING_PIXELS;
    }

    // Whether the tab rows laid out break the same way at the width given
    boolean tabsBreakAt(HierarchyInstance hierarchy, float barWidth) {
        return StripLayoutUtility.breaksAt(
                hierarchy.getTabNarrowest(),
                hierarchy.getTabRowEnds(),
                barWidth,
                EngineSetting.HIERARCHY_TAB_SPACING_PIXELS);
    }

    // Every tab its whole name's width, capped by the width its row's widest share, in place
    void resizeTabs(HierarchyInstance hierarchy, float barWidth) {

        FloatArrayList tabWidths = hierarchy.getTabWidths();
        FloatArrayList tabNarrowest = hierarchy.getTabNarrowest();
        IntArrayList rowEnds = hierarchy.getTabRowEnds();
        ObjectArrayList<ElementInstance> tabRows = hierarchy.getTabElements();
        int rowStart = 0;

        for (int r = 0; r < tabRows.size(); r++) {

            int rowEnd = rowEnds.getInt(r);
            ObjectArrayList<ElementInstance> tabs = tabRows.get(r).getChildren();
            float cap = StripLayoutUtility.resolveRowCap(
                    tabWidths,
                    tabNarrowest,
                    rowStart,
                    rowEnd,
                    barWidth,
                    EngineSetting.HIERARCHY_TAB_SPACING_PIXELS);

            for (int j = 0; j < tabs.size(); j++)
                tabs.get(j).setSizeOverride(new DimensionVector2Struct(
                        DimensionValueStruct.ofAbsolute(StripLayoutUtility.capWidth(
                                tabWidths.getFloat(rowStart + j), tabNarrowest.getFloat(rowStart + j), cap)),
                        DimensionValueStruct.ofAbsolute(EngineSetting.HIERARCHY_TAB_ROW_HEIGHT_PIXELS)));

            rowStart = rowEnd;
        }
    }

    // Each tab measured as it is injected and started on a new row when the row it joins cannot hold it at its
    // narrowest
    private void injectTabs(
            HierarchyInstance hierarchy,
            ObjectArrayList<HierarchyTabProvider> providers,
            float barWidth) {

        FloatArrayList tabNarrowest = hierarchy.getTabNarrowest();
        IntArrayList rowEnds = hierarchy.getTabRowEnds();
        ElementInstance tabRow = null;
        int rowStart = 0;

        hierarchy.getTabWidths().clear();
        tabNarrowest.clear();
        rowEnds.clear();

        for (int i = 0; i < providers.size(); i++) {

            if (tabRow == null)
                tabRow = injectTabRow(hierarchy);

            ElementInstance tab = injectTab(hierarchy, tabRow, providers.get(i));
            measureTab(hierarchy, tab);

            int rowEnd = StripLayoutUtility.resolveRowEnd(
                    tabNarrowest,
                    rowStart,
                    barWidth,
                    EngineSetting.HIERARCHY_TAB_SPACING_PIXELS);

            if (rowEnd > i)
                continue;

            menuManager.eject(tabRow, tab);
            rowEnds.add(i);
            rowStart = i;
            tabRow = injectTabRow(hierarchy);
            injectTab(hierarchy, tabRow, providers.get(i));
        }

        rowEnds.add(providers.size());
    }

    // A tab's whole name's width at full size and at the smallest readable size
    private void measureTab(HierarchyInstance hierarchy, ElementInstance tab) {

        ElementInstance label = tab.findChildById(EngineSetting.HIERARCHY_ELEMENT_TAB_LABEL);
        float preferred = label.resolveTextWidth(label.resolveFontSize(EngineSetting.HIERARCHY_TAB_ROW_HEIGHT_PIXELS));

        hierarchy.getTabWidths().add(preferred);
        hierarchy.getTabNarrowest().add(Math.min(preferred, label.resolveTextWidth(settings.minTextSize)));
    }

    private ElementInstance injectTabRow(HierarchyInstance hierarchy) {

        ElementInstance tabRow = menuManager.inject(
                hierarchy.getMenu(), EngineSetting.HIERARCHY_ENTRY_TABS,
                EngineSetting.HIERARCHY_TAB_ROW_TEMPLATE);
        hierarchy.getTabElements().add(tabRow);

        return tabRow;
    }

    private ElementInstance injectTab(
            HierarchyInstance hierarchy,
            ElementInstance tabRow,
            HierarchyTabProvider provider) {

        String tabName = provider.getTabName();
        String template = tabName.equals(hierarchy.getActiveTabName())
                ? EngineSetting.HIERARCHY_TAB_ACTIVE_TEMPLATE
                : EngineSetting.HIERARCHY_TAB_TEMPLATE;

        return menuManager.inject(
                tabRow, template,
                element -> {
                    element.setActionArgOverride(tabName);
                    setChildText(element, EngineSetting.HIERARCHY_ELEMENT_TAB_LABEL, tabName);

                    ElementInstance label = element.findChildById(EngineSetting.HIERARCHY_ELEMENT_TAB_LABEL);

                    if (label != null && provider.hasProblem())
                        label.setColorOverride(problemColor);
                });
    }

    private void resizeTabBar(MenuInstance menu, int rowCount) {

        float stripHeight = StripLayoutUtility.resolveStripHeight(
                rowCount,
                EngineSetting.HIERARCHY_TAB_ROW_HEIGHT_PIXELS,
                EngineSetting.HIERARCHY_TAB_SPACING_PIXELS);
        float barHeight = stripHeight + EngineSetting.HIERARCHY_TAB_BAR_PADDING_PIXELS;

        resizeEntry(menu, EngineSetting.HIERARCHY_ENTRY_TAB_BAR, new DimensionVector2Struct(
                DimensionValueStruct.ofPercent(EngineSetting.PERCENT_MAX),
                DimensionValueStruct.ofAbsolute(barHeight)));
        resizeEntry(menu, EngineSetting.HIERARCHY_ENTRY_TABS, new DimensionVector2Struct(
                DimensionValueStruct.ofPercentWithOffset(
                        EngineSetting.PERCENT_MAX,
                        -2f * EngineSetting.HIERARCHY_TAB_BAR_PADDING_PIXELS),
                DimensionValueStruct.ofAbsolute(stripHeight)));
        resizeEntry(menu, EngineSetting.HIERARCHY_ENTRY_ROWS, new DimensionVector2Struct(
                DimensionValueStruct.ofPercentWithOffset(
                        EngineSetting.PERCENT_MAX,
                        -EngineSetting.HIERARCHY_ROWS_MARGIN_PIXELS),
                DimensionValueStruct.ofPercentWithOffset(
                        EngineSetting.PERCENT_MAX,
                        -(barHeight + EngineSetting.HIERARCHY_ROWS_MARGIN_PIXELS))));
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

                    if (label != null && node.hasProblem())
                        label.setColorOverride(problemColor);

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

    private void resizeEntry(MenuInstance menu, int entryPoint, DimensionVector2Struct size) {

        ElementInstance entry = menu.getEntryPoint(entryPoint);

        if (entry != null)
            entry.setSizeOverride(size);
    }

    private void setChildText(ElementInstance element, String childId, String text) {

        ElementInstance child = element.findChildById(childId);

        if (child != null)
            child.setFontText(text);
    }

    private DimensionVector2Struct toOffset(float x) {
        return new DimensionVector2Struct(DimensionValueStruct.ofAbsolute(x), DimensionValueStruct.ofAbsolute(0f));
    }
}
