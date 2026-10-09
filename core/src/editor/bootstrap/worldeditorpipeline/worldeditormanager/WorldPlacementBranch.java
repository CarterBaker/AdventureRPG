package editor.bootstrap.worldeditorpipeline.worldeditormanager;

import application.bootstrap.mappipeline.mapmanager.MapManager;
import application.bootstrap.worldpipeline.settlement.SettlementHandle;
import application.bootstrap.worldpipeline.settlementmanager.SettlementManager;
import application.bootstrap.worldpipeline.structure.StructureHandle;
import application.bootstrap.worldpipeline.structuremanager.StructureManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.world.WorldPlacementKind;
import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import editor.bootstrap.worldeditorpipeline.worldplacement.WorldPlacementEntryStruct;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class WorldPlacementBranch extends BranchPackage {

    /*
     * Hand-picks what stands where in the world. The palette lists every
     * settlement type and every structure, settlements first, each by name;
     * the selected entry is placed on the exact block column under the
     * pointer, a structure turned by the chosen quarter turns, and Clear
     * takes away the hand placement nearest the pointer. Every change goes
     * to WorldManager at once and restreams the map and the preview terrain
     * it can reach, since a settlement's roads run out to its neighbours.
     * Placements are saved into and reloaded from the world's companion file
     * alongside the world image.
     */

    // Internal
    private WorldManager worldManager;
    private SettlementManager settlementManager;
    private StructureManager structureManager;
    private MapManager mapManager;
    private WorldStreamManager worldStreamManager;
    private WorldEditorManager worldEditorManager;

    // Palette
    private ObjectArrayList<WorldPlacementEntryStruct> placeables;
    private int selectedIndex;
    private int quarterTurns;

    // Base \\

    @Override
    protected void create() {
        this.placeables = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.worldManager = get(WorldManager.class);
        this.settlementManager = get(SettlementManager.class);
        this.structureManager = get(StructureManager.class);
        this.mapManager = get(MapManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
    }

    // Palette \\

    // Every settlement type, then every structure, gathered the first time the palette is asked for
    private ObjectArrayList<WorldPlacementEntryStruct> resolvePlaceables() {

        if (!placeables.isEmpty())
            return placeables;

        ObjectArrayList<SettlementHandle> settlements = settlementManager.getSettlementHandles();

        for (int i = 0; i < settlements.size(); i++)
            placeables.add(new WorldPlacementEntryStruct(
                    WorldPlacementKind.SETTLEMENT,
                    settlements.get(i).getSettlementName(),
                    EditorSetting.WORLD_EDITOR_PLACEABLE_SETTLEMENT + settlements.get(i).getDisplayName()));

        StructureHandle[] structures = structureManager.getStructureHandles();

        for (int i = 0; i < structures.length; i++)
            placeables.add(new WorldPlacementEntryStruct(
                    WorldPlacementKind.STRUCTURE,
                    structures[i].getStructureName(),
                    EditorSetting.WORLD_EDITOR_PLACEABLE_STRUCTURE + structures[i].getStructureName()));

        return placeables;
    }

    void cycle(int step) {

        ObjectArrayList<WorldPlacementEntryStruct> entries = resolvePlaceables();

        if (!entries.isEmpty())
            selectedIndex = Math.floorMod(selectedIndex + step, entries.size());
    }

    void turn() {
        quarterTurns = (quarterTurns + 1) % EngineSetting.STRUCTURE_QUARTER_TURN_COUNT;
    }

    WorldPlacementEntryStruct getSelected() {

        ObjectArrayList<WorldPlacementEntryStruct> entries = resolvePlaceables();

        return entries.isEmpty() ? null : entries.get(selectedIndex);
    }

    int getQuarterTurns() {
        return quarterTurns;
    }

    // Placement \\

    // The selected entry stood on the block column under an image position
    void place(WorldHandle worldHandle, double imageX, double imageY) {

        WorldPlacementEntryStruct entry = getSelected();

        if (entry == null) {
            worldEditorManager.setStatusMessage(EditorSetting.WORLD_EDITOR_STATUS_NO_PLACEABLE);
            return;
        }

        long blockX = toBlock(imageX);
        long blockZ = toBlock(imageY);

        worldManager.addPlacement(worldHandle, new WorldPlacementStruct(
                entry.getKind(), entry.getName(), "", blockX, blockZ,
                entry.getKind() == WorldPlacementKind.STRUCTURE ? quarterTurns : 0));

        restream(worldHandle, imageX, imageY);
        worldEditorManager.setStatusMessage(EditorSetting.WORLD_EDITOR_MESSAGE_PLACED + entry.getDisplayName());
    }

    // The hand placement nearest an image position taken away
    void clear(WorldHandle worldHandle, double imageX, double imageY) {

        WorldPlacementStruct removed = worldManager.removeNearestPlacement(
                worldHandle, toBlock(imageX), toBlock(imageY), EditorSetting.WORLD_EDITOR_CLEAR_RADIUS_BLOCKS);

        if (removed == null) {
            worldEditorManager.setStatusMessage(EditorSetting.WORLD_EDITOR_MESSAGE_NOTHING_TO_CLEAR);
            return;
        }

        double pixelBlocks = resolvePixelBlocks();

        restream(worldHandle, removed.getWorldX() / pixelBlocks, removed.getWorldZ() / pixelBlocks);
        worldEditorManager.setStatusMessage(EditorSetting.WORLD_EDITOR_MESSAGE_CLEARED + removed.getName());
    }

    // Every map tile and preview chunk a placement's settlement and roads can reach regenerated
    private void restream(WorldHandle worldHandle, double imageX, double imageY) {

        int reach = EditorSetting.WORLD_EDITOR_PLACEMENT_REACH_PIXELS;
        int pixelX = (int) Math.floor(imageX);
        int pixelY = (int) Math.floor(imageY);

        mapManager.invalidateWorldPixels(worldHandle, pixelX - reach, pixelY - reach, pixelX + reach, pixelY + reach);
        worldStreamManager.requestLiveRebuild(
                worldHandle, pixelX - reach, pixelY - reach, pixelX + reach, pixelY + reach);
    }

    // Disk \\

    boolean save(WorldHandle worldHandle) {
        return worldManager.savePlacements(worldHandle);
    }

    // The placements read back from disk, every map tile and preview chunk of the world regenerated
    boolean reload(WorldHandle worldHandle, int imageWidth, int imageHeight) {

        if (!worldManager.reloadPlacements(worldHandle))
            return false;

        mapManager.invalidateWorldPixels(worldHandle, 0, 0, imageWidth - 1, imageHeight - 1);
        worldStreamManager.requestLiveRebuild(worldHandle, 0, 0, imageWidth - 1, imageHeight - 1);

        return true;
    }

    // Utility \\

    private long toBlock(double imagePosition) {
        return (long) Math.floor(imagePosition * resolvePixelBlocks());
    }

    private double resolvePixelBlocks() {
        return EngineSetting.CHUNKS_PER_PIXEL * (double) EngineSetting.CHUNK_SIZE;
    }
}
