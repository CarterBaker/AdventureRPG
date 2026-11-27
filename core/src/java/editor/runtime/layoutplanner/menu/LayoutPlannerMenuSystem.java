package editor.runtime.layoutplanner.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.layoutplannerpipeline.layoutplannermanager.LayoutPlannerManager;
import editor.runtime.layoutplanner.LayoutPlannerSetting;
import editor.runtime.layoutplanner.view.LayoutPlannerViewSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class LayoutPlannerMenuSystem extends SystemPackage {

    /*
     * Opens the Layout Planner's menu over the viewport: the status line
     * names the settlement designed, the layout planned, how many buildings
     * and roads the plan lays, then the last message, or why nothing is
     * shown, rewritten only when the design or its plan moves. The axis gizmo
     * is every viewport's own (ViewGizmoSystem).
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private WorldManager worldManager;
    private LayoutPlannerManager layoutPlannerManager;
    private LayoutPlannerViewSystem layoutPlannerViewSystem;

    // Menus
    private MenuInstance viewerMenu;

    // Status
    private int shownRevision;
    private int shownPlanRevision;

    // Base \\

    @Override
    protected void create() {

        // Status
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
        this.shownPlanRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.worldManager = get(WorldManager.class);
        this.layoutPlannerManager = get(LayoutPlannerManager.class);
        this.layoutPlannerViewSystem = get(LayoutPlannerViewSystem.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.viewerMenu = menuManager.openMenu(LayoutPlannerSetting.MENU_VIEWER, window);
    }

    // Update \\

    @Override
    protected void update() {

        int revision = layoutPlannerManager.getRevision();
        int planRevision = layoutPlannerViewSystem.getRevision();

        if (revision == shownRevision && planRevision == shownPlanRevision)
            return;

        this.shownRevision = revision;
        this.shownPlanRevision = planRevision;

        ElementInstance label = viewerMenu.getEntryPoint(LayoutPlannerSetting.ENTRY_STATUS);

        if (label != null)
            label.setFontText(LayoutPlannerSetting.STATUS_PREFIX + describe());
    }

    // The settlement, its layout and what the plan lays, then the last message, or why nothing is shown
    private String describe() {

        String settlementName = layoutPlannerManager.getSettlementName();

        if (settlementName == null)
            return LayoutPlannerSetting.STATUS_NONE;

        if (worldManager.getActiveWorld() == null)
            return LayoutPlannerSetting.STATUS_NO_WORLD;

        String message = layoutPlannerManager.getStatusMessage();

        return settlementName + LayoutPlannerSetting.STATUS_SEPARATOR
                + LayoutPlannerSetting.STATUS_LAYOUT + layoutPlannerManager.getLayout()
                + LayoutPlannerSetting.STATUS_SEPARATOR
                + layoutPlannerViewSystem.getLotCount() + LayoutPlannerSetting.STATUS_BUILDINGS
                + LayoutPlannerSetting.STATUS_SEPARATOR
                + layoutPlannerViewSystem.getRoadCount() + LayoutPlannerSetting.STATUS_ROADS
                + (message.isEmpty() ? "" : LayoutPlannerSetting.STATUS_SEPARATOR + message);
    }
}
