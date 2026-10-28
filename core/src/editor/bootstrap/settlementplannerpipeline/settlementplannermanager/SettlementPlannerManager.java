package editor.bootstrap.settlementplannerpipeline.settlementplannermanager;

import java.util.concurrent.ThreadLocalRandom;

import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.world.WorldPlacementKind;
import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.settlementplannerpipeline.settlementplanner.SettlementPlannerArchitectureStruct;
import editor.bootstrap.settlementplannerpipeline.settlementplanner.SettlementPlannerEntryStruct;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SettlementPlannerManager extends ManagerPackage {

    /*
     * Owns the settlement planner's shared design, chosen the way a brush is:
     * which settlement type or structure it stands up, the architecture a
     * settlement is built in, the numbered layout it is planned from and the
     * quarter turns a structure takes. The World Editor stands the design
     * wherever its Place tool clicks or the design is dragged from the
     * planner and dropped, previewed on every map through
     * SettlementPreviewBranch while it is carried over the canvas. A placed,
     * selected or picked placement is edited from then on: every change to
     * its architecture, layout or turns, and every move to exact coordinates,
     * is applied to it live through the World Editor, so the world, its maps
     * and its preview terrain follow at once. Every placement in the world
     * image pixel last selected, or in the edited placement's own, is listed
     * to pick from. Choosing another settlement or structure, or Deselect,
     * lets the placement go. Settlement type edits from the Info Panel go live
     * through SettlementLiveBranch, and the catalogue of what can be designed
     * is kept by SettlementCatalogueBranch.
     */

    // Internal
    private WorldManager worldManager;
    private WorldEditorManager worldEditorManager;
    private SettlementCatalogueBranch settlementCatalogueBranch;
    private SettlementPreviewBranch settlementPreviewBranch;
    private SettlementLiveBranch settlementLiveBranch;

    // Design
    private int entryIndex;
    private int architectureIndex;
    private int layout;
    private int quarterTurns;

    // World
    private WorldHandle worldHandle;
    private WorldPlacementStruct editedPlacement;
    private int shownPlacementRevision;

    // Here
    private ObjectArrayList<WorldPlacementStruct> placementsHere;
    private boolean hasHere;
    private int herePixelX;
    private int herePixelY;

    // Drag
    private boolean dragging;
    private WindowInstance dragWindow;
    private boolean dropShown;
    private int dropPixelX;
    private int dropPixelY;

    // Status
    private String statusMessage;
    private int revision;
    private int rowRevision;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.settlementCatalogueBranch = create(SettlementCatalogueBranch.class);
        this.settlementPreviewBranch = create(SettlementPreviewBranch.class);
        this.settlementLiveBranch = create(SettlementLiveBranch.class);

        // Design
        this.layout = EditorSetting.SETTLEMENT_PLANNER_LAYOUT_DEFAULT;

        // World
        this.shownPlacementRevision = EngineSetting.INDEX_NOT_FOUND;

        // Here
        this.placementsHere = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.worldManager = get(WorldManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        syncWorld();
        syncPlacements();
    }

    // The planner designs for the active world, nothing of another world kept
    private void syncWorld() {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        if (activeWorld == worldHandle)
            return;

        this.worldHandle = activeWorld;
        this.editedPlacement = null;
        this.hasHere = false;
        placementsHere.clear();
        settlementCatalogueBranch.gatherArchitectures(getSelectedEntry());
        rowRevision++;
        requestSummary();
    }

    // A placement reloaded or cleared away from under the planner is let go, and the pixel's list gathered again
    private void syncPlacements() {

        int placementRevision = worldManager.getPlacementRevision();

        if (placementRevision == shownPlacementRevision)
            return;

        this.shownPlacementRevision = placementRevision;

        if (editedPlacement != null && !stands(editedPlacement))
            detach();

        refreshHere();
    }

    private boolean stands(WorldPlacementStruct placement) {

        WorldPlacementStruct[] placements = worldManager.getPlacements(worldHandle);

        for (int i = 0; i < placements.length; i++)
            if (placements[i] == placement)
                return true;

        return false;
    }

    // Catalogue \\

    public void selectEntry(int index) {

        ObjectArrayList<SettlementPlannerEntryStruct> entries = settlementCatalogueBranch.getEntries();

        if (index < 0 || index >= entries.size())
            return;

        letGo();
        worldEditorManager.showPlacements();

        this.entryIndex = index;
        this.architectureIndex = 0;
        settlementCatalogueBranch.gatherArchitectures(entries.get(index));
        rowRevision++;

        if (!settlementCatalogueBranch.isBuildable(entries.get(index)))
            setStatusMessage(
                    EditorSetting.SETTLEMENT_PLANNER_MESSAGE_UNBUILDABLE + entries.get(index).getDisplayName());

        requestSummary();
    }

    // The catalogue entry a settlement type or structure selected in the Hierarchy names, if it is a new one
    void selectEntryByName(WorldPlacementKind kind, String name) {

        int index = settlementCatalogueBranch.indexOfEntry(kind, name);

        if (index != EngineSetting.INDEX_NOT_FOUND && index != entryIndex)
            selectEntry(index);
    }

    public void cycleEntry(int step) {

        int size = settlementCatalogueBranch.getEntries().size();

        if (size > 0)
            selectEntry(Math.floorMod(entryIndex + step, size));
    }

    // The catalogue gathered again after a live edit, the design kept and planned anew
    void refreshCatalogue() {

        settlementCatalogueBranch.refreshEntries();
        settlementCatalogueBranch.gatherArchitectures(getSelectedEntry());
        rowRevision++;
        requestSummary();
    }

    // Architecture \\

    public void selectArchitecture(int index) {

        if (index < 0 || index >= settlementCatalogueBranch.getArchitectures().size())
            return;

        this.architectureIndex = index;
        rowRevision++;
        applyDesign();
    }

    public void cycleArchitecture(int step) {

        int size = settlementCatalogueBranch.getArchitectures().size();

        if (size == 0) {
            setStatusMessage(EditorSetting.SETTLEMENT_PLANNER_MESSAGE_NO_ARCHITECTURE);
            return;
        }

        selectArchitecture(Math.floorMod(architectureIndex + step, size));
    }

    // Layout \\

    public void stepLayout(int step) {

        if (!isDesigningSettlement()) {
            setStatusMessage(EditorSetting.SETTLEMENT_PLANNER_MESSAGE_NO_LAYOUT);
            return;
        }

        this.layout = Math.max(
                EngineSetting.WORLD_PLACEMENT_LAYOUT_CELL,
                Math.min(EngineSetting.WORLD_PLACEMENT_LAYOUT_MAX, layout + step));
        applyDesign();
    }

    public void randomLayout() {

        if (!isDesigningSettlement()) {
            setStatusMessage(EditorSetting.SETTLEMENT_PLANNER_MESSAGE_NO_LAYOUT);
            return;
        }

        this.layout = ThreadLocalRandom.current().nextInt(
                EngineSetting.WORLD_PLACEMENT_LAYOUT_CELL + 1, EngineSetting.WORLD_PLACEMENT_LAYOUT_MAX + 1);
        applyDesign();
    }

    public void turn() {

        SettlementPlannerEntryStruct entry = getSelectedEntry();

        if (entry == null || entry.isSettlement()) {
            setStatusMessage(EditorSetting.SETTLEMENT_PLANNER_MESSAGE_NO_TURN);
            return;
        }

        this.quarterTurns = (quarterTurns + 1) % EngineSetting.STRUCTURE_QUARTER_TURN_COUNT;
        applyDesign();
    }

    public void editType() {

        SettlementPlannerEntryStruct entry = getSelectedEntry();

        if (entry == null)
            return;

        settlementLiveBranch.selectInHierarchy(
                entry.isSettlement() ? EditorSetting.INFO_SCHEMA_SETTLEMENTS : EditorSetting.INFO_SCHEMA_STRUCTURES,
                entry.getName());
    }

    // Selection \\

    // The placement being edited let go; the design stays as it was
    public void detach() {
        if (letGo())
            requestSummary();
    }

    private boolean letGo() {

        if (editedPlacement == null)
            return false;

        this.editedPlacement = null;
        rowRevision++;
        setStatusMessage(EditorSetting.SETTLEMENT_PLANNER_MESSAGE_RELEASED);

        return true;
    }

    // A standing placement brought into the planner, every later change to the design applied to it live
    public void editPlacement(WorldPlacementStruct placement) {

        if (!loadDesign(placement)) {
            setStatusMessage(EditorSetting.SETTLEMENT_PLANNER_MESSAGE_UNKNOWN + placement.getName());
            return;
        }

        this.editedPlacement = placement;
        this.hasHere = true;
        refreshHere();
        setStatusMessage(EditorSetting.SETTLEMENT_PLANNER_MESSAGE_EDITING + getSelectedEntry().getDisplayName());
        requestSummary();
    }

    // The design taken from a placement, false when the catalogue has nothing by its name
    private boolean loadDesign(WorldPlacementStruct placement) {

        int index = settlementCatalogueBranch.indexOfEntry(placement.getKind(), placement.getName());

        if (index == EngineSetting.INDEX_NOT_FOUND)
            return false;

        this.entryIndex = index;
        settlementCatalogueBranch.gatherArchitectures(getSelectedEntry());
        this.architectureIndex = Math.max(0, settlementCatalogueBranch.indexOfArchitecture(
                placement.getArchitectureName()));
        this.layout = placement.getLayout();
        this.quarterTurns = placement.getQuarterTurns();
        rowRevision++;

        return true;
    }

    public void selectPlacedHere(int index) {
        if (index >= 0 && index < placementsHere.size())
            editPlacement(placementsHere.get(index));
    }

    // The list follows the edited placement's own pixel, or else stays on the pixel last selected
    private void refreshHere() {

        if (!hasHere)
            return;

        if (editedPlacement != null) {
            this.herePixelX = worldEditorManager.toPixelX(editedPlacement);
            this.herePixelY = worldEditorManager.toPixelY(editedPlacement);
        }

        worldEditorManager.collectPlacementsInPixel(herePixelX, herePixelY, placementsHere);
        rowRevision++;
    }

    // Placement \\

    // The edited placement stood on another block column, returning why it was refused or null once it stands
    public String moveEditedPlacement(long worldX, long worldZ) {

        if (editedPlacement == null)
            return EditorSetting.SETTLEMENT_PLANNER_MESSAGE_NOTHING_EDITED;

        return applyDesignAt(worldX, worldZ);
    }

    public boolean isCoordinateValid(String text) {

        if (editedPlacement == null)
            return false;

        try {
            Long.parseLong(text.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void setEditedX(String text) {
        if (isCoordinateValid(text))
            reportMove(moveEditedPlacement(Long.parseLong(text.trim()), editedPlacement.getWorldZ()));
    }

    public void setEditedZ(String text) {
        if (isCoordinateValid(text))
            reportMove(moveEditedPlacement(editedPlacement.getWorldX(), Long.parseLong(text.trim())));
    }

    private void reportMove(String refusal) {
        setStatusMessage(refusal != null ? refusal : EditorSetting.SETTLEMENT_PLANNER_MESSAGE_MOVED);
    }

    // The edited placement's coordinates written so the coordinate dialog can start from them
    public String formatEditedX() {
        return editedPlacement != null ? Long.toString(editedPlacement.getWorldX()) : "";
    }

    public String formatEditedZ() {
        return editedPlacement != null ? Long.toString(editedPlacement.getWorldZ()) : "";
    }

    public void removeEditedPlacement() {

        if (!requireEditedPlacement())
            return;

        WorldPlacementStruct removed = editedPlacement;
        String refusal = worldEditorManager.removePlacement(removed);

        if (refusal != null) {
            setStatusMessage(refusal);
            return;
        }

        this.editedPlacement = null;
        setStatusMessage(EditorSetting.SETTLEMENT_PLANNER_MESSAGE_REMOVED + removed.getName());
        requestSummary();
    }

    // True while a placement is edited; otherwise says so in the status line
    public boolean requireEditedPlacement() {

        if (editedPlacement != null)
            return true;

        setStatusMessage(EditorSetting.SETTLEMENT_PLANNER_MESSAGE_NOTHING_EDITED);

        return false;
    }

    // Drag \\

    // An entry taken hold of in the catalogue becomes the design and is carried until let go
    public void beginDrag(int index, WindowInstance window) {

        if (dragging)
            return;

        selectEntry(index);

        this.dragging = true;
        this.dragWindow = window;
        this.dropShown = false;
    }

    // The carried design previewed on every map at the column under the pointer, planned again per pixel crossed
    public void hoverDrop(int pixelX, int pixelY, long worldX, long worldZ) {

        if (!dragging || worldHandle == null || dropShown && pixelX == dropPixelX && pixelY == dropPixelY)
            return;

        WorldPlacementStruct design = createPlacement(worldX, worldZ);

        if (design == null)
            return;

        this.dropShown = true;
        this.dropPixelX = pixelX;
        this.dropPixelY = pixelY;
        settlementPreviewBranch.request(worldHandle, design, true);
    }

    // The carry let go, its preview taken off the maps
    public void endDrag() {

        if (!dragging)
            return;

        this.dragging = false;
        this.dragWindow = null;

        if (!dropShown)
            return;

        this.dropShown = false;
        requestSummary();
    }

    public boolean isDragging() {
        return dragging;
    }

    public WindowInstance getDragWindow() {
        return dragWindow;
    }

    // Design \\

    // The design standing at a world column, null when nothing buildable is chosen
    public WorldPlacementStruct createPlacement(long worldX, long worldZ) {

        SettlementPlannerEntryStruct entry = getSelectedEntry();

        if (entry == null || !settlementCatalogueBranch.isBuildable(entry))
            return null;

        boolean settlement = entry.isSettlement();

        return new WorldPlacementStruct(
                entry.getKind(),
                entry.getName(),
                settlement ? settlementCatalogueBranch.getArchitectures().get(architectureIndex).getArchitectureName()
                        : "",
                settlement ? layout : EngineSetting.WORLD_PLACEMENT_LAYOUT_CELL,
                worldX,
                worldZ,
                settlement ? 0 : quarterTurns);
    }

    // A changed design applied to the placement being edited; refused, the design goes back to what stands
    private void applyDesign() {

        if (editedPlacement == null) {
            notifyChanged();
            return;
        }

        String refusal = applyDesignAt(editedPlacement.getWorldX(), editedPlacement.getWorldZ());

        if (refusal == null)
            return;

        loadDesign(editedPlacement);
        setStatusMessage(refusal);
    }

    // The design swapped in for the edited placement at a block column, returning why it was refused or null
    private String applyDesignAt(long worldX, long worldZ) {

        WorldPlacementStruct design = createPlacement(worldX, worldZ);
        String refusal = design != null
                ? worldEditorManager.replacePlacement(editedPlacement, design)
                : EditorSetting.SETTLEMENT_PLANNER_MESSAGE_NOT_APPLIED;

        if (refusal == null)
            this.editedPlacement = design;

        requestSummary();

        return refusal;
    }

    // The edited placement planned for its summary, nothing previewed when none is edited
    private void requestSummary() {

        notifyChanged();

        if (worldHandle == null)
            return;

        if (editedPlacement == null) {
            settlementPreviewBranch.clear();
            return;
        }

        settlementPreviewBranch.request(worldHandle, editedPlacement, false);
    }

    // Status \\

    void notifyChanged() {
        revision++;
    }

    void setStatusMessage(String statusMessage) {

        this.statusMessage = statusMessage;
        notifyChanged();
    }

    public String getStatusText() {

        StringBuilder status = new StringBuilder(describeDesign());

        if (statusMessage != null)
            status.append(EditorSetting.SETTLEMENT_PLANNER_STATUS_SEPARATOR).append(statusMessage);

        return status.toString();
    }

    // What the design stands up, in what and how, for the planner's and the World Editor's status lines
    public String describeDesign() {

        SettlementPlannerEntryStruct entry = getSelectedEntry();

        if (entry == null)
            return EditorSetting.SETTLEMENT_PLANNER_STATUS_NOTHING;

        return entry.getDisplayName() + EditorSetting.SETTLEMENT_PLANNER_STATUS_SEPARATOR + describeValues();
    }

    // The design's own values: a settlement's architecture and layout, a structure's turns
    public String describeValues() {

        SettlementPlannerEntryStruct entry = getSelectedEntry();

        if (entry == null)
            return EditorSetting.SETTLEMENT_PLANNER_STATUS_NOTHING;

        if (!entry.isSettlement())
            return EditorSetting.SETTLEMENT_PLANNER_STATUS_TURNS + quarterTurns;

        ObjectArrayList<SettlementPlannerArchitectureStruct> architectures = settlementCatalogueBranch
                .getArchitectures();
        StringBuilder values = new StringBuilder(architectureIndex < architectures.size()
                ? architectures.get(architectureIndex).getDisplayName()
                : EditorSetting.SETTLEMENT_PLANNER_STATUS_NO_ARCHITECTURE)
                .append(EditorSetting.SETTLEMENT_PLANNER_STATUS_SEPARATOR);

        return layout == EngineSetting.WORLD_PLACEMENT_LAYOUT_CELL
                ? values.append(EditorSetting.SETTLEMENT_PLANNER_STATUS_CELL_LAYOUT).toString()
                : values.append(EditorSetting.SETTLEMENT_PLANNER_STATUS_LAYOUT).append(layout).toString();
    }

    // The edited placement, where it stands and what it plans to, or how to have one
    public String describeEdited() {

        if (editedPlacement == null)
            return EditorSetting.SETTLEMENT_PLANNER_STATUS_NOTHING_EDITED;

        StringBuilder description = new StringBuilder(describePlacement(editedPlacement))
                .append(EditorSetting.SETTLEMENT_PLANNER_STATUS_SEPARATOR);

        appendSummary(description);

        return description.toString();
    }

    // A placement's shown name and exact block column
    public String describePlacement(WorldPlacementStruct placement) {

        int index = settlementCatalogueBranch.indexOfEntry(placement.getKind(), placement.getName());
        String name = index != EngineSetting.INDEX_NOT_FOUND
                ? settlementCatalogueBranch.getEntries().get(index).getDisplayName()
                : placement.getName();

        return name + EditorSetting.SETTLEMENT_PLANNER_STATUS_COLUMN_X + placement.getWorldX()
                + EditorSetting.SETTLEMENT_PLANNER_STATUS_Z + placement.getWorldZ();
    }

    private void appendSummary(StringBuilder status) {

        if (settlementPreviewBranch.isPlanning() || !settlementPreviewBranch.isSummarized()) {
            status.append(EditorSetting.SETTLEMENT_PLANNER_STATUS_PLANNING);
            return;
        }

        status.append(EditorSetting.SETTLEMENT_PLANNER_STATUS_BUILDINGS).append(settlementPreviewBranch.getLotCount())
                .append(EditorSetting.SETTLEMENT_PLANNER_STATUS_ROADS).append(settlementPreviewBranch.getPathCount());

        if (settlementPreviewBranch.isWalled())
            status.append(EditorSetting.SETTLEMENT_PLANNER_STATUS_WALLED);
    }

    // Accessible \\

    public ObjectArrayList<SettlementPlannerEntryStruct> getEntries() {
        return settlementCatalogueBranch.getEntries();
    }

    public SettlementPlannerEntryStruct getSelectedEntry() {

        ObjectArrayList<SettlementPlannerEntryStruct> entries = settlementCatalogueBranch.getEntries();

        return entries.isEmpty() ? null : entries.get(entryIndex);
    }

    public int getEntryIndex() {
        return entryIndex;
    }

    public ObjectArrayList<SettlementPlannerArchitectureStruct> getArchitectures() {
        return settlementCatalogueBranch.getArchitectures();
    }

    public int getArchitectureIndex() {
        return architectureIndex;
    }

    public ObjectArrayList<WorldPlacementStruct> getPlacementsHere() {
        return placementsHere;
    }

    public boolean isDesigningSettlement() {

        SettlementPlannerEntryStruct entry = getSelectedEntry();

        return entry != null && entry.isSettlement();
    }

    // How far the design stands out from its column, 0 when nothing is chosen
    public int getFootprintRadiusBlocks() {

        SettlementPlannerEntryStruct entry = getSelectedEntry();

        return entry != null ? settlementCatalogueBranch.resolveRadiusBlocks(entry) : 0;
    }

    public boolean isEdited(WorldPlacementStruct placement) {
        return placement == editedPlacement;
    }

    public boolean hasEditedPlacement() {
        return editedPlacement != null;
    }

    public WorldHandle getWorldHandle() {
        return worldHandle;
    }

    public int getRevision() {
        return revision;
    }

    public int getRowRevision() {
        return rowRevision;
    }
}
