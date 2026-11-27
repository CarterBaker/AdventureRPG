package editor.runtime.roadplanner.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.biomeplannerpipeline.biomeplannermanager.BiomePlannerManager;
import editor.bootstrap.roadplannerpipeline.roadplannermanager.RoadPlannerManager;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.biomegrid.BiomeGridUtility;
import editor.runtime.roadplanner.RoadPlannerSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class RoadPlannerMenuSystem extends SystemPackage {

    /*
     * Opens the Road Planner menu. Every grid cell shows the biome it stands
     * for in its color and by name, as the Biome Planner shows it, marked
     * with where the road passes through it, and the status line names the
     * road type, how long it runs and how many bridges and tunnels it was
     * settled with, then the last message. Cells and status are rewritten
     * only when the road or the grid changes.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private WorldEditorManager worldEditorManager;
    private BiomePlannerManager biomePlannerManager;
    private RoadPlannerManager roadPlannerManager;

    // Menus
    private MenuInstance plannerMenu;

    // Status
    private int shownRevision;
    private int shownGridRevision;

    // Base \\

    @Override
    protected void create() {
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
        this.shownGridRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
        this.biomePlannerManager = get(BiomePlannerManager.class);
        this.roadPlannerManager = get(RoadPlannerManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.plannerMenu = menuManager.openMenu(RoadPlannerSetting.MENU_PLANNER, window);
    }

    // Update \\

    @Override
    protected void update() {

        int revision = roadPlannerManager.getRevision();
        int gridRevision = biomePlannerManager.getRevision();

        if (revision == shownRevision && gridRevision == shownGridRevision)
            return;

        this.shownRevision = revision;
        this.shownGridRevision = gridRevision;

        for (int cell = 0; cell < biomePlannerManager.getCellCount(); cell++)
            BiomeGridUtility.refreshCell(
                    plannerMenu.getEntryPoint(RoadPlannerSetting.ENTRY_FIRST_CELL + cell), cell, describeCell(cell),
                    biomePlannerManager, worldEditorManager);

        ElementInstance status = plannerMenu.getEntryPoint(RoadPlannerSetting.ENTRY_STATUS);

        if (status != null)
            status.setFontText(RoadPlannerSetting.STATUS_PREFIX + describeRoad());
    }

    // Where the road passes through a cell, nothing for a cell it misses
    private String describeCell(int cell) {

        int number = roadPlannerManager.getWaypointNumber(cell);

        return number == EngineSetting.INDEX_NOT_FOUND
                ? ""
                : RoadPlannerSetting.CELL_WAYPOINT_PREFIX + number + RoadPlannerSetting.CELL_WAYPOINT_SUFFIX;
    }

    // The road type, its length, bridges and tunnels, then the last message, or why no road is laid
    private String describeRoad() {

        String roadName = roadPlannerManager.getRoadName();
        String message = roadPlannerManager.getStatusMessage();
        String tail = message.isEmpty() ? "" : RoadPlannerSetting.STATUS_SEPARATOR + message;

        if (roadName == null)
            return RoadPlannerSetting.STATUS_NONE + tail;

        if (roadPlannerManager.getWaypointCount() < 2)
            return roadName + RoadPlannerSetting.STATUS_SEPARATOR + RoadPlannerSetting.STATUS_FEW + tail;

        return roadName + RoadPlannerSetting.STATUS_SEPARATOR
                + Math.round(roadPlannerManager.getLengthBlocks()) + RoadPlannerSetting.STATUS_LENGTH
                + RoadPlannerSetting.STATUS_SEPARATOR + roadPlannerManager.getBridgeCount()
                + RoadPlannerSetting.STATUS_BRIDGES
                + RoadPlannerSetting.STATUS_SEPARATOR + roadPlannerManager.getTunnelCount()
                + RoadPlannerSetting.STATUS_TUNNELS + tail;
    }
}
