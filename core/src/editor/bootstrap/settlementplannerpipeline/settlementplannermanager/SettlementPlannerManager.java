package editor.bootstrap.settlementplannerpipeline.settlementplannermanager;

import java.util.concurrent.ThreadLocalRandom;

import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import editor.bootstrap.settlementplannerpipeline.settlementplanner.SettlementPlannerArchitectureStruct;
import editor.bootstrap.settlementplannerpipeline.settlementplanner.SettlementPlannerEntryStruct;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.EditorSetting;
import engine.assets.image.Pixmap;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SettlementPlannerManager extends ManagerPackage {

    /*
     * Owns the settlement planner's shared design: which settlement type or
     * structure it stands up, the architecture a settlement is built in, the
     * numbered layout it is planned from, the quarter turns a structure takes,
     * and the site in the active world where it is shown, first the world's
     * first hand placement or else the dry land nearest its middle. While
     * nothing is being edited the design is previewed at the site on every
     * map, through SettlementPreviewBranch, without standing in the world.
     * The World Editor places the design wherever it is clicked, and its Edit
     * tool brings a standing placement back here: from then on every change
     * to its architecture, layout or turns is applied to that placement live,
     * through the World Editor, so the world, its maps and its preview
     * terrain follow at once. Choosing another settlement or structure, or
     * New, lets the placement go and designs afresh at the site, and Preview
     * Here moves the site. The planner's maps reframe whenever the site or
     * the chosen entry changes. Settlement type edits from the Info Panel go live
     * through SettlementLiveBranch, and the catalogue of what can be designed
     * is kept by SettlementCatalogueBranch.
     */

    // Internal
    private WorldManager worldManager;
    private BiomeManager biomeManager;
    private WorldEditorManager worldEditorManager;
    private SettlementCatalogueBranch settlementCatalogueBranch;
    private SettlementPreviewBranch settlementPreviewBranch;
    private SettlementLiveBranch settlementLiveBranch;

    // Design
    private int entryIndex;
    private int architectureIndex;
    private int layout;
    private int quarterTurns;

    // Site
    private WorldHandle siteWorldHandle;
    private long siteX;
    private long siteZ;
    private WorldPlacementStruct editedPlacement;
    private int shownPlacementRevision;

    // Status
    private String statusMessage;
    private int revision;
    private int rowRevision;
    private int frameRevision;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.settlementCatalogueBranch = create(SettlementCatalogueBranch.class);
        this.settlementPreviewBranch = create(SettlementPreviewBranch.class);
        this.settlementLiveBranch = create(SettlementLiveBranch.class);

        // Design
        this.layout = EditorSetting.SETTLEMENT_PLANNER_LAYOUT_DEFAULT;

        // Site
        this.shownPlacementRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.worldManager = get(WorldManager.class);
        this.biomeManager = get(BiomeManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        syncWorld();
        syncEditedPlacement();
    }

    // The planner designs for the active world, sited on its first placement or else the dry land nearest its centre
    private void syncWorld() {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        if (activeWorld == siteWorldHandle)
            return;

        WorldPlacementStruct[] placements = worldManager.getPlacements(activeWorld);

        this.siteWorldHandle = activeWorld;
        this.editedPlacement = null;
        settlementCatalogueBranch.gatherArchitectures(getSelectedEntry());
        rowRevision++;

        if (placements.length > 0)
            moveSite(placements[0].getWorldX(), placements[0].getWorldZ());
        else
            siteOnLand(activeWorld);

        requestPreview();
    }

    // The centre of the world image pixel nearest its middle that is not painted as sea, searched ring by ring
    private void siteOnLand(WorldHandle worldHandle) {

        Pixmap world = worldHandle.getWorld();
        int centerX = world.getWidth() / 2;
        int centerZ = world.getHeight() / 2;
        int maxRing = Math.max(centerX, centerZ);

        for (int ring = 0; ring <= maxRing; ring++)
            for (int offsetZ = -ring; offsetZ <= ring; offsetZ++)
                for (int offsetX = -ring; offsetX <= ring; offsetX += Math.abs(offsetZ) == ring ? 1 : ring * 2)
                    if (isLand(worldHandle, centerX + offsetX, centerZ + offsetZ)) {
                        moveSite(toBlock(centerX + offsetX), toBlock(centerZ + offsetZ));
                        return;
                    }

        moveSite(toBlock(centerX), toBlock(centerZ));
    }

    private boolean isLand(WorldHandle worldHandle, int pixelX, int pixelZ) {

        BiomeHandle biome = biomeManager.getPaintedBiome(worldHandle, pixelX, pixelZ);

        return biome != null && !biome.hasOceanWater();
    }

    private long toBlock(int pixel) {

        long pixelBlocks = (long) EngineSetting.CHUNKS_PER_PIXEL * EngineSetting.CHUNK_SIZE;

        return pixel * pixelBlocks + pixelBlocks / 2;
    }

    // A placement reloaded or cleared away from under the planner is let go, its design kept at the site
    private void syncEditedPlacement() {

        int placementRevision = worldManager.getPlacementRevision();

        if (placementRevision == shownPlacementRevision)
            return;

        this.shownPlacementRevision = placementRevision;

        if (editedPlacement != null && !stands(editedPlacement))
            detach();
    }

    private boolean stands(WorldPlacementStruct placement) {

        WorldPlacementStruct[] placements = worldManager.getPlacements(siteWorldHandle);

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

        this.entryIndex = index;
        this.architectureIndex = 0;
        settlementCatalogueBranch.gatherArchitectures(entries.get(index));
        rowRevision++;
        frameRevision++;

        if (!settlementCatalogueBranch.isBuildable(entries.get(index)))
            setStatusMessage(
                    EditorSetting.SETTLEMENT_PLANNER_MESSAGE_UNBUILDABLE + entries.get(index).getDisplayName());

        requestPreview();
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
        requestPreview();
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

    // Site \\

    // The design shown at another column of the active world, any placement being edited let go
    public void previewAt(double worldX, double worldZ) {

        letGo();
        moveSite((long) Math.floor(worldX), (long) Math.floor(worldZ));
        setStatusMessage(EditorSetting.SETTLEMENT_PLANNER_MESSAGE_PREVIEWING);
        requestPreview();
    }

    // The placement being edited let go; the design stays where it stood, previewed
    public void detach() {
        if (letGo())
            requestPreview();
    }

    private boolean letGo() {

        if (editedPlacement == null)
            return false;

        this.editedPlacement = null;
        setStatusMessage(EditorSetting.SETTLEMENT_PLANNER_MESSAGE_RELEASED);

        return true;
    }

    // A standing placement brought into the planner, every later change to the design applied to it live
    public void editPlacement(WorldPlacementStruct placement) {

        int index = settlementCatalogueBranch.indexOfEntry(placement.getKind(), placement.getName());

        if (index == EngineSetting.INDEX_NOT_FOUND) {
            setStatusMessage(EditorSetting.SETTLEMENT_PLANNER_MESSAGE_UNKNOWN + placement.getName());
            return;
        }

        this.entryIndex = index;
        settlementCatalogueBranch.gatherArchitectures(getSelectedEntry());
        this.architectureIndex = Math.max(0, settlementCatalogueBranch.indexOfArchitecture(
                placement.getArchitectureName()));
        this.layout = placement.getLayout();
        this.quarterTurns = placement.getQuarterTurns();
        this.editedPlacement = placement;
        rowRevision++;

        moveSite(placement.getWorldX(), placement.getWorldZ());
        setStatusMessage(EditorSetting.SETTLEMENT_PLANNER_MESSAGE_EDITING + getSelectedEntry().getDisplayName());
        requestPreview();
    }

    private void moveSite(long worldX, long worldZ) {

        this.siteX = worldX;
        this.siteZ = worldZ;
        frameRevision++;
        notifyChanged();
    }

    public void editType() {

        SettlementPlannerEntryStruct entry = getSelectedEntry();

        if (entry == null)
            return;

        settlementLiveBranch.selectInHierarchy(
                entry.isSettlement() ? EditorSetting.INFO_SCHEMA_SETTLEMENTS : EditorSetting.INFO_SCHEMA_STRUCTURES,
                entry.getName());
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

    // A changed design applied to the placement being edited, or else previewed at the site
    private void applyDesign() {

        if (editedPlacement == null || siteWorldHandle == null) {
            requestPreview();
            return;
        }

        WorldPlacementStruct design = createPlacement(editedPlacement.getWorldX(), editedPlacement.getWorldZ());
        String refusal = design != null
                ? worldEditorManager.replacePlacement(editedPlacement, design)
                : EditorSetting.SETTLEMENT_PLANNER_MESSAGE_NOT_APPLIED;

        if (refusal != null) {
            this.editedPlacement = null;
            setStatusMessage(refusal);
        } else
            this.editedPlacement = design;

        requestPreview();
    }

    // The design planned where it stands or would stand, shown on the maps unless it already stands
    private void requestPreview() {

        notifyChanged();

        if (siteWorldHandle == null)
            return;

        WorldPlacementStruct design = editedPlacement != null ? editedPlacement : createPlacement(siteX, siteZ);

        if (design == null) {
            settlementPreviewBranch.clear();
            return;
        }

        settlementPreviewBranch.request(siteWorldHandle, design, editedPlacement == null);
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

        status.append(EditorSetting.SETTLEMENT_PLANNER_STATUS_SEPARATOR)
                .append(editedPlacement != null
                        ? EditorSetting.SETTLEMENT_PLANNER_STATUS_EDITING
                        : EditorSetting.SETTLEMENT_PLANNER_STATUS_PREVIEW)
                .append(EditorSetting.SETTLEMENT_PLANNER_STATUS_X).append(siteX)
                .append(EditorSetting.SETTLEMENT_PLANNER_STATUS_Z).append(siteZ)
                .append(EditorSetting.SETTLEMENT_PLANNER_STATUS_SEPARATOR);

        appendSummary(status);

        if (statusMessage != null)
            status.append(EditorSetting.SETTLEMENT_PLANNER_STATUS_SEPARATOR).append(statusMessage);

        return status.toString();
    }

    // What the design stands up, in what and how, for the planner's and the World Editor's status lines
    public String describeDesign() {

        SettlementPlannerEntryStruct entry = getSelectedEntry();

        if (entry == null)
            return EditorSetting.SETTLEMENT_PLANNER_STATUS_NOTHING;

        StringBuilder description = new StringBuilder(entry.getDisplayName())
                .append(EditorSetting.SETTLEMENT_PLANNER_STATUS_SEPARATOR);

        if (!entry.isSettlement())
            return description.append(EditorSetting.SETTLEMENT_PLANNER_STATUS_TURNS).append(quarterTurns).toString();

        ObjectArrayList<SettlementPlannerArchitectureStruct> architectures = settlementCatalogueBranch
                .getArchitectures();

        description.append(architectureIndex < architectures.size()
                ? architectures.get(architectureIndex).getDisplayName()
                : EditorSetting.SETTLEMENT_PLANNER_STATUS_NO_ARCHITECTURE)
                .append(EditorSetting.SETTLEMENT_PLANNER_STATUS_SEPARATOR);

        return layout == EngineSetting.WORLD_PLACEMENT_LAYOUT_CELL
                ? description.append(EditorSetting.SETTLEMENT_PLANNER_STATUS_CELL_LAYOUT).toString()
                : description.append(EditorSetting.SETTLEMENT_PLANNER_STATUS_LAYOUT).append(layout).toString();
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

    public WorldHandle getSiteWorldHandle() {
        return siteWorldHandle;
    }

    public long getSiteX() {
        return siteX;
    }

    public long getSiteZ() {
        return siteZ;
    }

    public int getRevision() {
        return revision;
    }

    public int getRowRevision() {
        return rowRevision;
    }

    // Raised whenever the site moves or another entry is chosen, so the planner's maps frame the design again
    public int getFrameRevision() {
        return frameRevision;
    }
}
