package editor.runtime.treeviewer.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.treeviewerpipeline.treeviewermanager.TreeViewerManager;
import editor.runtime.treeviewer.TreeViewerSetting;
import editor.runtime.treeviewer.layer.TreeViewerLayerSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class TreeViewerMenuSystem extends SystemPackage {

    /*
     * Opens the Tree Viewer's status line over the viewport, which names the
     * species, the layer shown and how many vertices it draws, the age and
     * seed the preview grows from and the viewer's last message, rewritten
     * only when the viewer, its preview or its layers change.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private TreeViewerManager treeViewerManager;
    private TreeViewerLayerSystem treeViewerLayerSystem;

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
        this.treeViewerManager = get(TreeViewerManager.class);
        this.treeViewerLayerSystem = get(TreeViewerLayerSystem.class);
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

        int revision = treeViewerLayerSystem.getRevision();

        if (revision == shownRevision)
            return;

        shownRevision = revision;

        ElementInstance label = viewerMenu.getEntryPoint(TreeViewerSetting.ENTRY_STATUS);

        if (label != null)
            label.setFontText(TreeViewerSetting.STATUS_PREFIX + describe());
    }

    // The species, the layer shown with its vertex count, the preview's age and seed and the last message
    private String describe() {

        String treeName = treeViewerManager.getTreeName();

        if (treeName == null)
            return TreeViewerSetting.STATUS_NONE;

        StringBuilder text = new StringBuilder(treeName).append(TreeViewerSetting.STATUS_SEPARATOR);
        int shown = treeViewerLayerSystem.getShownLayer();

        if (shown == EngineSetting.INDEX_NOT_FOUND)
            text.append(TreeViewerSetting.STATUS_HIDDEN);
        else
            text.append(treeViewerLayerSystem.getName(shown))
                    .append(' ')
                    .append(treeViewerManager.getPreviewLayer(shown).getVertexCount())
                    .append(TreeViewerSetting.STATUS_VERTICES);

        text.append(TreeViewerSetting.STATUS_SEPARATOR)
                .append(TreeViewerSetting.STATUS_AGE)
                .append(Math.round(treeViewerManager.getAge() * TreeViewerSetting.STATUS_PERCENT))
                .append(TreeViewerSetting.STATUS_PERCENT_SIGN)
                .append(TreeViewerSetting.STATUS_SEPARATOR)
                .append(TreeViewerSetting.STATUS_SEED)
                .append(treeViewerManager.getSeed());

        if (treeViewerManager.getStatusMessage() != null)
            text.append(TreeViewerSetting.STATUS_SEPARATOR).append(treeViewerManager.getStatusMessage());

        return text.toString();
    }
}
