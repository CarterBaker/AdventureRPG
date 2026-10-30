package editor.runtime.treeplanner.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.ThemeColor;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.treeplannerpipeline.treeplanner.TreePlannerNode;
import editor.bootstrap.treeplannerpipeline.treeplanner.TreePlannerParameter;
import editor.bootstrap.treeplannerpipeline.treeplannermanager.TreePlannerManager;
import editor.runtime.treeplanner.TreePlannerSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TreePlannerMenuSystem extends SystemPackage {

    /*
     * Opens the Tree Planner menu. Its catalogue lists every tree species the
     * Info files hold, the selected one highlighted; its graph shows the
     * generator's nodes in the order a tree grows through them, each with a
     * summary of its first fields and the open node highlighted; its fields
     * panel lists the open node's fields, each with its value and a step down
     * and up; and the status line names the species, the preview's age and
     * seed, and the planner's last message. Everything is rewritten only when
     * the planner changes.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private TreePlannerManager treePlannerManager;

    // Menus
    private MenuInstance plannerMenu;

    // Rows
    private ObjectArrayList<ElementInstance> catalogueElements;
    private ObjectArrayList<ElementInstance> parameterElements;

    // Colors
    private MenuColorStruct nodeColor;
    private MenuColorStruct openNodeColor;

    // Status
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // Rows
        this.catalogueElements = new ObjectArrayList<>();
        this.parameterElements = new ObjectArrayList<>();

        // Colors
        this.nodeColor = new MenuColorStruct(ThemeColor.CONTROL, TreePlannerSetting.NODE_ALPHA);
        this.openNodeColor = new MenuColorStruct(ThemeColor.ACCENT, TreePlannerSetting.NODE_OPEN_ALPHA);

        // Status
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.treePlannerManager = get(TreePlannerManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.plannerMenu = menuManager.openMenu(TreePlannerSetting.MENU_PLANNER, window);
    }

    // Update \\

    @Override
    protected void update() {

        int revision = treePlannerManager.getRevision() + treePlannerManager.getCatalogueRevision();

        if (revision == shownRevision)
            return;

        shownRevision = revision;
        refreshCatalogue();
        refreshNodes();
        refreshParameters();
        refreshStatus();
    }

    // Catalogue \\

    private void refreshCatalogue() {

        for (int i = 0; i < catalogueElements.size(); i++)
            menuManager.eject(plannerMenu, TreePlannerSetting.ENTRY_CATALOGUE, catalogueElements.get(i));

        catalogueElements.clear();

        ObjectArrayList<String> treeNames = treePlannerManager.getTreeNames();

        for (int i = 0; i < treeNames.size(); i++)
            catalogueElements.add(injectTree(treeNames.get(i)));
    }

    private ElementInstance injectTree(String treeName) {

        String template = treeName.equals(treePlannerManager.getTreeName())
                ? TreePlannerSetting.TEMPLATE_TREE_ROW_SELECTED
                : TreePlannerSetting.TEMPLATE_TREE_ROW;

        return menuManager.inject(
                plannerMenu, TreePlannerSetting.ENTRY_CATALOGUE, template,
                element -> {
                    element.setActionArgOverride(treeName);

                    ElementInstance label = element.findChildById(TreePlannerSetting.ELEMENT_TREE_LABEL);

                    if (label != null)
                        label.setFontText(treePlannerManager.getDisplayName(treeName));
                });
    }

    // Graph \\

    // Every node shaded open or not and summed up by its first fields
    private void refreshNodes() {

        TreePlannerNode[] nodes = TreePlannerNode.values();

        for (int i = 0; i < nodes.length; i++) {

            ElementInstance element = plannerMenu.getEntryPoint(TreePlannerSetting.ENTRY_FIRST_NODE + i);

            if (element == null)
                continue;

            element.setColorOverride(nodes[i] == treePlannerManager.getNode() ? openNodeColor : nodeColor);

            ElementInstance summary = element.findChildById(TreePlannerSetting.ELEMENT_NODE_SUMMARY);

            if (summary != null)
                summary.setFontText(summarize(nodes[i]));
        }
    }

    private String summarize(TreePlannerNode node) {

        if (treePlannerManager.getTreeName() == null)
            return "";

        StringBuilder summary = new StringBuilder();
        TreePlannerParameter[] parameters = TreePlannerParameter.values();
        int named = 0;

        for (int i = 0; i < parameters.length && named < TreePlannerSetting.NODE_SUMMARY_PARAMETERS; i++) {

            if (parameters[i].node != node)
                continue;

            if (named > 0)
                summary.append(TreePlannerSetting.NODE_SUMMARY_SEPARATOR);

            summary.append(parameters[i].label).append(' ').append(treePlannerManager.formatValue(parameters[i]));
            named++;
        }

        return summary.toString();
    }

    // Fields \\

    // The open node's fields, one row each carrying its parameter to its step buttons
    private void refreshParameters() {

        for (int i = 0; i < parameterElements.size(); i++)
            menuManager.eject(plannerMenu, TreePlannerSetting.ENTRY_PARAMETERS, parameterElements.get(i));

        parameterElements.clear();

        ElementInstance title = plannerMenu.getEntryPoint(TreePlannerSetting.ENTRY_PROPERTIES_TITLE);

        if (title != null)
            title.setFontText(treePlannerManager.getNode().displayName + TreePlannerSetting.FIELDS_TITLE_SUFFIX);

        if (treePlannerManager.getTreeName() == null)
            return;

        TreePlannerParameter[] parameters = TreePlannerParameter.values();

        for (int i = 0; i < parameters.length; i++)
            if (parameters[i].node == treePlannerManager.getNode())
                parameterElements.add(injectParameter(parameters[i]));
    }

    private ElementInstance injectParameter(TreePlannerParameter parameter) {

        return menuManager.inject(
                plannerMenu, TreePlannerSetting.ENTRY_PARAMETERS, TreePlannerSetting.TEMPLATE_PARAMETER_ROW,
                element -> {
                    ElementInstance label = element.findChildById(TreePlannerSetting.ELEMENT_PARAMETER_LABEL);
                    ElementInstance value = element.findChildById(TreePlannerSetting.ELEMENT_PARAMETER_VALUE);
                    ElementInstance lower = element.findChildById(TreePlannerSetting.ELEMENT_PARAMETER_LOWER);
                    ElementInstance raise = element.findChildById(TreePlannerSetting.ELEMENT_PARAMETER_RAISE);

                    if (label != null)
                        label.setFontText(parameter.label);

                    if (value != null)
                        value.setFontText(treePlannerManager.formatValue(parameter));

                    if (lower != null)
                        lower.setActionArgOverride(parameter.name());

                    if (raise != null)
                        raise.setActionArgOverride(parameter.name());
                });
    }

    // Status \\

    private void refreshStatus() {

        ElementInstance label = plannerMenu.getEntryPoint(TreePlannerSetting.ENTRY_STATUS);

        if (label != null)
            label.setFontText(treePlannerManager.getStatusText());
    }
}
