package editor.bootstrap.roadplannerpipeline.roadplannermanager;

import java.util.Objects;

import application.bootstrap.worldpipeline.layout.LayoutDraftStruct;
import application.bootstrap.worldpipeline.layout.LayoutPlanStruct;
import application.bootstrap.worldpipeline.layoutmanager.LayoutManager;
import application.bootstrap.worldpipeline.road.RoadPathStruct;
import application.bootstrap.worldpipeline.road.RoadSpanType;
import application.bootstrap.worldpipeline.roadmanager.RoadManager;
import application.bootstrap.worldpipeline.settlementmanager.SettlementManager;
import application.bootstrap.worldpipeline.util.RoadPathUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import editor.bootstrap.biomeplannerpipeline.biomeplannermanager.BiomePlannerManager;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.propertiespipeline.util.PaletteRowUtility;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.root.UtilityPackage.InternalException;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RoadPlannerManager extends ManagerPackage {

    /*
     * Owns the Road Planner's shared state: the road type drawn, the one the
     * Hierarchy and the Palette tab pick, and the cells of the biome
     * planner's grid it runs through in order. The grid is the Biome
     * Planner's own, painted through BiomePlannerManager, so the mountains,
     * rivers and lakes its biomes raise are the ones the road meets. Place
     * adds a cell to the road or takes it off, Erase takes it off and Brush
     * paints the biome picked into it. The road runs cell centre to cell
     * centre, each leg bent as the roads between settlements bend, and is
     * settled against the land through RoadManager.planPath(), the planner
     * generation takes, its heights, bridges and tunnels; it is handed to
     * the planner's world through SettlementManager.setPlannedLayouts(), so
     * the world lays it, draws it and keeps off it as it does any road, and
     * the world restreams, the Biome Viewer showing it as the game builds it.
     * It is planned again whenever its cells, its type or the grid change. A
     * road type edited in the Info Panel is rebuilt live, saved or not.
     */

    // Internal
    private InfoManager infoManager;
    private ToolManager toolManager;
    private RoadManager roadManager;
    private SettlementManager settlementManager;
    private LayoutManager layoutManager;
    private WorldManager worldManager;
    private WorldEditorManager worldEditorManager;
    private BiomePlannerManager biomePlannerManager;

    // Road — its type, the cells it runs through in order, and what its last plan holds
    private String roadName;
    private IntArrayList waypoints;
    private int bridgeCount;
    private int tunnelCount;
    private float lengthBlocks;

    // Planning — the points of one leg and of the whole road
    private DoubleArrayList legX;
    private DoubleArrayList legZ;
    private DoubleArrayList pointX;
    private DoubleArrayList pointZ;
    private int shownGridRevision;

    // Status
    private String statusMessage;
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Road
        this.waypoints = new IntArrayList();

        // Planning
        this.legX = new DoubleArrayList();
        this.legZ = new DoubleArrayList();
        this.pointX = new DoubleArrayList();
        this.pointZ = new DoubleArrayList();
        this.shownGridRevision = EngineSetting.INDEX_NOT_FOUND;

        // Status
        this.statusMessage = "";
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.toolManager = get(ToolManager.class);
        this.roadManager = get(RoadManager.class);
        this.settlementManager = get(SettlementManager.class);
        this.layoutManager = get(LayoutManager.class);
        this.worldManager = get(WorldManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
        this.biomePlannerManager = get(BiomePlannerManager.class);
    }

    @Override
    protected void awake() {

        infoManager.addSelectionListener(
                EditorSetting.INFO_SCHEMA_ROADS, entry -> applySelection(entry.getEntryName()));
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_ROADS, this::rebuildRoad);
    }

    // Update \\

    // The road planned again once the grid's biomes change the land under it
    @Override
    protected void update() {

        int gridRevision = biomePlannerManager.getRevision();

        if (gridRevision == shownGridRevision)
            return;

        this.shownGridRevision = gridRevision;

        if (waypoints.size() > 1)
            replan();
    }

    // Selection \\

    // A road type picked in the Palette tab, opened in the Hierarchy and the Info Panel alike
    public void selectRoad(String selectedName) {

        infoManager.selectEntry(EditorSetting.INFO_SCHEMA_ROADS, selectedName, selectedName);
        applySelection(selectedName);
    }

    private void applySelection(String selectedName) {

        if (Objects.equals(selectedName, roadName))
            return;

        this.roadName = selectedName;
        replan();
    }

    // The road type opened in the Hierarchy so the Info Panel's own New and Duplicate act on it — false with none
    public boolean focusRoad() {

        if (roadName == null) {
            setStatusMessage(EditorSetting.ROAD_PLANNER_MESSAGE_NONE);
            return false;
        }

        infoManager.selectEntry(EditorSetting.INFO_SCHEMA_ROADS, roadName, roadName);
        return true;
    }

    // A road type edited in the Info Panel rebuilt live, or left as it was with the reason why
    private void rebuildRoad(InfoEntryStruct entry) {

        try {
            roadManager.rebuildRoad(entry.getEntryName(), entry.getArpg());
        } catch (InternalException e) {
            errorLog(e.getMessage());
            setStatusMessage(EditorSetting.ROAD_PLANNER_MESSAGE_REFUSED + e.getMessage());
            return;
        }

        restreamActiveWorld();

        if (entry.getEntryName().equals(roadName))
            replan();
    }

    // Every road of the active world planned again as the type now builds it
    private void restreamActiveWorld() {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        if (activeWorld != null)
            worldEditorManager.restreamWorld(activeWorld);
    }

    // Palette \\

    // Every road type, then every biome the grid may be painted with
    public void buildPaletteRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.ROAD_PLANNER_PALETTE_ROADS));
        PaletteRowUtility.addEntryRows(
                infoManager, rows, EditorSetting.INFO_SCHEMA_ROADS, EditorSetting.ROAD_PLANNER_ROW_ROAD, roadName);

        rows.add(CharacterViewRowStruct.header(EditorSetting.ROAD_PLANNER_PALETTE_BIOMES));
        PaletteRowUtility.addEntryRows(
                infoManager, rows, EditorSetting.INFO_SCHEMA_BIOMES, EditorSetting.ROAD_PLANNER_ROW_BIOME,
                worldEditorManager.getSelectedBiomeName());
    }

    // A Palette row chosen: a road type to draw, or the biome the Brush paints, the grid's centre with it
    public void choosePaletteRow(String rowKey) {

        if (rowKey.startsWith(EditorSetting.ROAD_PLANNER_ROW_ROAD)) {
            selectRoad(rowKey.substring(EditorSetting.ROAD_PLANNER_ROW_ROAD.length()));
            return;
        }

        worldEditorManager.selectBiome(rowKey.substring(EditorSetting.ROAD_PLANNER_ROW_BIOME.length()));
        notifyChanged();
    }

    // Cells \\

    // A cell pressed with the tool picked: Place adds it to the road or takes it off, Erase takes it off, Brush
    // paints the biome picked into it
    public void pressCell(int cell) {

        if (cell < 0 || cell >= biomePlannerManager.getCellCount())
            return;

        EditorTool tool = toolManager.getTool(ToolCanvas.ROAD);

        if (tool == EditorTool.BRUSH) {
            biomePlannerManager.paintCell(cell);
            return;
        }

        int index = waypoints.indexOf(cell);

        if (index != EngineSetting.INDEX_NOT_FOUND)
            waypoints.removeInt(index);
        else if (tool == EditorTool.PLACE)
            waypoints.add(cell);
        else
            return;

        replan();
    }

    // A Tools tab Road action run by its key
    public void act(String actionKey) {

        if (!actionKey.equals(EditorSetting.ROAD_PLANNER_KEY_CLEAR))
            return;

        waypoints.clear();
        replan();
        setStatusMessage(EditorSetting.ROAD_PLANNER_MESSAGE_CLEARED);
    }

    // Plan \\

    // The road settled against the planner's world and handed to it, or taken off it while there is no road to lay
    private void replan() {

        WorldHandle world = biomePlannerManager.getWorldHandle();

        this.bridgeCount = 0;
        this.tunnelCount = 0;
        this.lengthBlocks = 0f;

        if (roadName == null || !roadManager.hasRoad(roadName) || waypoints.size() < 2) {
            settlementManager.setPlannedLayouts(world, new LayoutPlanStruct[0]);
            worldEditorManager.restreamWorld(world);
            notifyChanged();
            return;
        }

        gatherPoints(world);

        RoadPathStruct path = roadManager.planPath(
                world, roadManager.getRoadHandleFromRoadName(roadName), pointX, pointZ,
                EngineSetting.ROAD_HEIGHT_FREE, EngineSetting.ROAD_HEIGHT_FREE);
        LayoutDraftStruct draft = new LayoutDraftStruct(world, pointX.getDouble(0), pointZ.getDouble(0), null);

        draft.addPath(path);
        settlementManager.setPlannedLayouts(world, new LayoutPlanStruct[] { layoutManager.seal(draft) });
        worldEditorManager.restreamWorld(world);
        summarize(path);
        notifyChanged();
    }

    // Every leg from one cell's centre to the next, bent as the roads between settlements bend, joined end to end
    private void gatherPoints(WorldHandle world) {

        pointX.clear();
        pointZ.clear();

        long seed = RegistryUtility.toNameSeed(roadName);

        for (int i = 1; i < waypoints.size(); i++) {

            RoadPathUtility.buildCurve(
                    resolveCellCentre(world, waypoints.getInt(i - 1), true),
                    resolveCellCentre(world, waypoints.getInt(i - 1), false),
                    resolveCellCentre(world, waypoints.getInt(i), true),
                    resolveCellCentre(world, waypoints.getInt(i), false),
                    EngineSetting.SETTLEMENT_LINK_BEND, seed + i, legX, legZ);

            for (int point = pointX.isEmpty() ? 0 : 1; point < legX.size(); point++) {
                pointX.add(legX.getDouble(point));
                pointZ.add(legZ.getDouble(point));
            }
        }
    }

    // A cell's centre on an axis in world blocks, cells laid row by row from the world's corner
    private double resolveCellCentre(WorldHandle world, int cell, boolean alongX) {

        int side = EditorSetting.BIOME_PLANNER_GRID_SIDE;
        int index = alongX ? cell % side : cell / side;
        double cellBlocks = (double) (alongX ? world.getWorldScale().x : world.getWorldScale().y) / side;

        return (index + 0.5) * cellBlocks;
    }

    // How long the road runs and how many bridges and tunnels it was settled with
    private void summarize(RoadPathStruct path) {

        this.lengthBlocks = path.getLength();

        for (int i = 1; i < path.getPointCount(); i++) {

            RoadSpanType span = path.getPointSpan(i);

            if (span == path.getPointSpan(i - 1))
                continue;

            if (span == RoadSpanType.BRIDGE)
                this.bridgeCount++;
            else if (span == RoadSpanType.TUNNEL)
                this.tunnelCount++;
        }
    }

    // File \\

    public void save() {

        if (!focusRoad())
            return;

        String message = infoManager.saveDocument(EditorSetting.INFO_SCHEMA_ROADS, roadName)
                ? EditorSetting.ROAD_PLANNER_MESSAGE_SAVED
                : EditorSetting.ROAD_PLANNER_MESSAGE_SAVE_FAILED;

        setStatusMessage(message + roadName);
    }

    public void revert() {

        if (!focusRoad())
            return;

        infoManager.revertDocument(EditorSetting.INFO_SCHEMA_ROADS, roadName);
        setStatusMessage(EditorSetting.ROAD_PLANNER_MESSAGE_REVERTED + roadName);
    }

    // Status \\

    private void setStatusMessage(String message) {

        this.statusMessage = message;
        notifyChanged();
    }

    private void notifyChanged() {
        this.revision++;
    }

    // Accessible \\

    // The road type drawn, null before one is chosen
    public String getRoadName() {
        return roadName;
    }

    // Where a cell stands along the road, counted from one, INDEX_NOT_FOUND for a cell it does not run through
    public int getWaypointNumber(int cell) {

        int index = waypoints.indexOf(cell);

        return index == EngineSetting.INDEX_NOT_FOUND ? EngineSetting.INDEX_NOT_FOUND : index + 1;
    }

    public int getWaypointCount() {
        return waypoints.size();
    }

    public int getBridgeCount() {
        return bridgeCount;
    }

    public int getTunnelCount() {
        return tunnelCount;
    }

    public float getLengthBlocks() {
        return lengthBlocks;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public int getRevision() {
        return revision;
    }
}
