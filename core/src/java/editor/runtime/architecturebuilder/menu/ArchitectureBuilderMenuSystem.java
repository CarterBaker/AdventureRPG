package editor.runtime.architecturebuilder.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.architecturebuilderpipeline.architecturebuildermanager.ArchitectureBuilderManager;
import editor.runtime.architecturebuilder.ArchitectureBuilderSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class ArchitectureBuilderMenuSystem extends SystemPackage {

    /*
     * Opens the Architecture Builder's menu over the viewport: the status
     * line names the architecture worked on and the slot picked with what
     * fills it, then the last file action's message, or why nothing is
     * shown, rewritten only when the builder's revision moves. The axis gizmo
     * is every viewport's own (ViewGizmoSystem).
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private ArchitectureBuilderManager architectureBuilderManager;

    // Menus
    private MenuInstance viewerMenu;

    // Status
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // Status
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.architectureBuilderManager = get(ArchitectureBuilderManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.viewerMenu = menuManager.openMenu(ArchitectureBuilderSetting.MENU_VIEWER, window);
    }

    // Update \\

    @Override
    protected void update() {

        int revision = architectureBuilderManager.getRevision();

        if (revision == shownRevision)
            return;

        this.shownRevision = revision;

        ElementInstance label = viewerMenu.getEntryPoint(ArchitectureBuilderSetting.ENTRY_STATUS);

        if (label != null)
            label.setFontText(ArchitectureBuilderSetting.STATUS_PREFIX + describe());
    }

    // The architecture and the layer picked, then the last message, or why no architecture is shown
    private String describe() {

        String architectureName = architectureBuilderManager.getArchitectureName();

        if (architectureName == null)
            return ArchitectureBuilderSetting.STATUS_NONE;

        String message = architectureBuilderManager.getStatusMessage();

        return architectureName + ArchitectureBuilderSetting.STATUS_SEPARATOR
                + architectureBuilderManager.describeLayer()
                + (message.isEmpty() ? "" : ArchitectureBuilderSetting.STATUS_SEPARATOR + message);
    }
}
