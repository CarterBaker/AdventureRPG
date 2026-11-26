package editor.runtime.treeplanner.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.ThemeColor;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.treeviewerpipeline.treeviewer.TreePlannerNode;
import editor.bootstrap.treeviewerpipeline.treeviewer.TreePlannerParameter;
import editor.bootstrap.treeviewerpipeline.treeviewermanager.TreeViewerManager;
import editor.runtime.treeplanner.TreePlannerSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class TreePlannerMenuSystem extends SystemPackage {

    /*
     * Opens the Tree Planner menu: the generator's nodes in the order a tree
     * grows through them, each summed up by its first fields and the open
     * node shaded, and the status line naming the species, the preview's age
     * and seed and the last message. Everything is rewritten only when the
     * tree tabs' state changes.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private TreeViewerManager treeViewerManager;

    // Menus
    private MenuInstance plannerMenu;

    // Colors
    private MenuColorStruct nodeColor;
    private MenuColorStruct openNodeColor;

    // Status
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

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
        this.treeViewerManager = get(TreeViewerManager.class);
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

        int revision = treeViewerManager.getRevision();

        if (revision == shownRevision)
            return;

        shownRevision = revision;
        refreshNodes();
        refreshStatus();
    }

    // Graph \\

    // Every node shaded open or not and summed up by its first fields
    private void refreshNodes() {

        TreePlannerNode[] nodes = TreePlannerNode.values();

        for (int i = 0; i < nodes.length; i++) {

            ElementInstance element = plannerMenu.getEntryPoint(TreePlannerSetting.ENTRY_FIRST_NODE + i);

            if (element == null)
                continue;

            element.setColorOverride(nodes[i] == treeViewerManager.getNode() ? openNodeColor : nodeColor);

            ElementInstance summary = element.findChildById(TreePlannerSetting.ELEMENT_NODE_SUMMARY);

            if (summary != null)
                summary.setFontText(summarize(nodes[i]));
        }
    }

    private String summarize(TreePlannerNode node) {

        if (treeViewerManager.getTreeName() == null)
            return "";

        StringBuilder summary = new StringBuilder();
        TreePlannerParameter[] parameters = TreePlannerParameter.values();
        int named = 0;

        for (int i = 0; i < parameters.length && named < TreePlannerSetting.NODE_SUMMARY_PARAMETERS; i++) {

            if (parameters[i].node != node)
                continue;

            if (named > 0)
                summary.append(TreePlannerSetting.NODE_SUMMARY_SEPARATOR);

            summary.append(parameters[i].label).append(' ').append(treeViewerManager.formatValue(parameters[i]));
            named++;
        }

        return summary.toString();
    }

    // Status \\

    private void refreshStatus() {

        ElementInstance label = plannerMenu.getEntryPoint(TreePlannerSetting.ENTRY_STATUS);

        if (label != null)
            label.setFontText(describe());
    }

    // The species, the preview's age and seed and the last message
    private String describe() {

        String treeName = treeViewerManager.getTreeName();

        if (treeName == null)
            return TreePlannerSetting.STATUS_NONE;

        StringBuilder text = new StringBuilder(treeName)
                .append(TreePlannerSetting.STATUS_SEPARATOR)
                .append(TreePlannerSetting.STATUS_AGE)
                .append(Math.round(treeViewerManager.getAge() * TreePlannerSetting.STATUS_PERCENT))
                .append(TreePlannerSetting.STATUS_PERCENT_SIGN)
                .append(TreePlannerSetting.STATUS_SEPARATOR)
                .append(TreePlannerSetting.STATUS_SEED)
                .append(treeViewerManager.getSeed());

        if (treeViewerManager.getStatusMessage() != null)
            text.append(TreePlannerSetting.STATUS_SEPARATOR).append(treeViewerManager.getStatusMessage());

        return text.toString();
    }
}
