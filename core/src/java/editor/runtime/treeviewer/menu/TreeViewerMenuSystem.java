package editor.runtime.treeviewer.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.treeplannerpipeline.treeplanner.TreePreviewTierStruct;
import editor.bootstrap.treeplannerpipeline.treeplannermanager.TreePlannerManager;
import editor.runtime.treeviewer.TreeViewerSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class TreeViewerMenuSystem extends SystemPackage {

    /*
     * Opens the Tree Viewer's toolbar and status line over the viewport. The
     * toolbar frames the whole row or one tier; the status line names the
     * species and how many vertices each tier draws it with, from the
     * fullest to the plainest, rewritten only when the planner or its row
     * changes.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private TreePlannerManager treePlannerManager;

    // Menus
    private MenuInstance viewerMenu;

    // Status
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {
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
        this.viewerMenu = menuManager.openMenu(TreeViewerSetting.MENU_VIEWER, window);
    }

    // Update \\

    @Override
    protected void update() {

        int revision = treePlannerManager.getRevision() + treePlannerManager.getPreviewRevision();

        if (revision == shownRevision)
            return;

        shownRevision = revision;

        ElementInstance label = viewerMenu.getEntryPoint(TreeViewerSetting.ENTRY_STATUS);

        if (label != null)
            label.setFontText(TreeViewerSetting.STATUS_PREFIX + describeRow());
    }

    // The species and every tier's vertex count, left to right
    private String describeRow() {

        String treeName = treePlannerManager.getTreeName();

        if (treeName == null)
            return TreeViewerSetting.STATUS_NONE;

        StringBuilder text = new StringBuilder(treePlannerManager.getDisplayName(treeName));

        for (int i = 0; i < treePlannerManager.getPreviewTierCount(); i++) {

            TreePreviewTierStruct tier = treePlannerManager.getPreviewTier(i);

            text.append(TreeViewerSetting.STATUS_TIER_SEPARATOR)
                    .append(TreeViewerSetting.TIER_NAMES[tier.getDetail().ordinal()])
                    .append(' ')
                    .append(tier.getVertexCount());
        }

        return text.append(TreeViewerSetting.STATUS_VERTICES).toString();
    }
}
