package editor.bootstrap.worldeditorpipeline.worldeditormanager;

import application.bootstrap.worldpipeline.settlementmanager.SettlementManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.world.WorldPlacementKind;
import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import application.bootstrap.worldpipeline.util.WorldPlacementArpgUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.settlementplannerpipeline.settlementplannermanager.SettlementPlannerManager;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class WorldPlacementBranch extends BranchPackage {

    /*
     * Hand-picks what stands where in the world, and is the one place a hand
     * placement is added, swapped or taken away. Place stands the Settlement
     * Planner's design on the exact block column under the pointer, or where
     * it is dropped from the planner, and hands it back to the planner to
     * keep editing, and Erase takes away the placement nearest the pointer.
     * The planner's live changes to the placement it edits, and every move
     * of the Select tool's selection, are swapped in here, and the
     * placements they remove taken away.
     * A settlement cell holds one hand-placed settlement, so a second is
     * refused. Every change goes to WorldManager at once and restreams the
     * map and the preview terrain it can reach, since a settlement's roads
     * run out to its neighbours. Placements are saved into and reloaded from
     * the world's companion file alongside the world image, through the
     * world's content in InfoManager, so they are checked as every other save
     * is and the Info Panel shows them.
     */

    // Internal
    private InfoManager infoManager;
    private WorldManager worldManager;
    private SettlementManager settlementManager;
    private WorldEditorManager worldEditorManager;
    private SettlementPlannerManager settlementPlannerManager;

    // Base \\

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.worldManager = get(WorldManager.class);
        this.settlementManager = get(SettlementManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
        this.settlementPlannerManager = get(SettlementPlannerManager.class);
    }


    // Placement \\

    // The planner's design stood on the block column under an image position, and edited from then on
    void place(WorldHandle worldHandle, double imageX, double imageY) {

        WorldPlacementStruct placement = settlementPlannerManager.createPlacement(toBlock(imageX), toBlock(imageY));

        if (placement == null) {
            worldEditorManager.setStatusMessage(EditorSetting.WORLD_EDITOR_STATUS_NO_PLACEABLE);
            return;
        }

        String refusal = findRefusal(worldHandle, placement, null);

        if (refusal != null) {
            worldEditorManager.setStatusMessage(refusal);
            return;
        }

        worldManager.addPlacement(worldHandle, placement);
        restream(worldHandle, placement);
        settlementPlannerManager.editPlacement(placement);
        worldEditorManager.setStatusMessage(EditorSetting.WORLD_EDITOR_MESSAGE_PLACED + placement.getName());
    }

    // The hand placement nearest an image position taken away
    void clear(WorldHandle worldHandle, double imageX, double imageY) {

        WorldPlacementStruct removed = worldManager.removeNearestPlacement(
                worldHandle, toBlock(imageX), toBlock(imageY), EditorSetting.WORLD_EDITOR_CLEAR_RADIUS_BLOCKS);

        if (removed == null) {
            worldEditorManager.setStatusMessage(EditorSetting.WORLD_EDITOR_MESSAGE_NOTHING_NEAR);
            return;
        }

        restream(worldHandle, removed);
        worldEditorManager.setStatusMessage(EditorSetting.WORLD_EDITOR_MESSAGE_CLEARED + removed.getName());
    }

    // One exact placement taken away, returning why it was not, or null once it is gone
    String remove(WorldHandle worldHandle, WorldPlacementStruct placement) {

        if (!worldManager.removePlacement(worldHandle, placement))
            return EditorSetting.WORLD_EDITOR_MESSAGE_PLACEMENT_GONE;

        restream(worldHandle, placement);
        worldEditorManager.setStatusMessage(EditorSetting.WORLD_EDITOR_MESSAGE_CLEARED + placement.getName());

        return null;
    }

    // One placement swapped for another, returning why it was refused, or null once it stands
    String replace(WorldHandle worldHandle, WorldPlacementStruct previous, WorldPlacementStruct replacement) {

        String refusal = findRefusal(worldHandle, replacement, previous);

        if (refusal != null)
            return refusal;

        if (!worldManager.replacePlacement(worldHandle, previous, replacement))
            return EditorSetting.WORLD_EDITOR_MESSAGE_PLACEMENT_GONE;

        restream(worldHandle, previous);

        if (previous.getWorldX() != replacement.getWorldX() || previous.getWorldZ() != replacement.getWorldZ())
            restream(worldHandle, replacement);

        return null;
    }

    // A settlement refused where another hand-placed settlement already holds its cell
    private String findRefusal(WorldHandle worldHandle, WorldPlacementStruct placement, WorldPlacementStruct ignored) {

        if (placement.getKind() != WorldPlacementKind.SETTLEMENT)
            return null;

        WorldPlacementStruct holder = settlementManager.findPlacedSettlement(
                worldHandle, placement.getWorldX(), placement.getWorldZ());

        return holder == null || holder == ignored
                ? null
                : EditorSetting.WORLD_EDITOR_MESSAGE_CELL_HELD + holder.getName();
    }

    // Every map tile and preview chunk a placement's settlement and roads can reach regenerated
    private void restream(WorldHandle worldHandle, WorldPlacementStruct placement) {

        int reach = EditorSetting.WORLD_EDITOR_PLACEMENT_REACH_PIXELS;
        int pixelX = toPixelX(worldHandle, placement);
        int pixelY = toPixelY(worldHandle, placement);

        worldEditorManager.restreamPixels(
                worldHandle, pixelX - reach, pixelY - reach, pixelX + reach, pixelY + reach);
    }

    // Pixel \\

    void collectInPixel(
            WorldHandle worldHandle,
            int pixelX,
            int pixelY,
            ObjectArrayList<WorldPlacementStruct> output) {

        WorldPlacementStruct[] placements = worldManager.getPlacements(worldHandle);

        output.clear();

        for (int i = 0; i < placements.length; i++)
            if (toPixelX(worldHandle, placements[i]) == pixelX && toPixelY(worldHandle, placements[i]) == pixelY)
                output.add(placements[i]);
    }

    int toPixelX(WorldHandle worldHandle, WorldPlacementStruct placement) {
        return (int) Math.floor(
                WorldWrapUtility.wrapBlockX(worldHandle, placement.getWorldX()) / resolvePixelBlocks());
    }

    int toPixelY(WorldHandle worldHandle, WorldPlacementStruct placement) {
        return (int) Math.floor(
                WorldWrapUtility.wrapBlockZ(worldHandle, placement.getWorldZ()) / resolvePixelBlocks());
    }

    // Disk \\

    // The placements written into the world's content, every other field of it kept, and saved with it
    boolean save(WorldHandle worldHandle) {

        WorldPlacementStruct[] placements = worldManager.getPlacements(worldHandle);

        infoManager.editEntry(
                EditorSetting.INFO_SCHEMA_WORLDS,
                worldHandle.getWorldName(),
                worldHandle.getWorldName(),
                worldArpg -> WorldPlacementArpgUtility.writePlacements(worldArpg, placements));

        return infoManager.saveDocument(EditorSetting.INFO_SCHEMA_WORLDS, worldHandle.getWorldName());
    }

    String getSaveRefusal() {
        return infoManager.getRefusal();
    }

    // The placements read back from disk, every map tile and preview chunk of the world regenerated
    boolean reload(WorldHandle worldHandle) {

        infoManager.revertDocument(EditorSetting.INFO_SCHEMA_WORLDS, worldHandle.getWorldName());

        if (!worldManager.reloadPlacements(worldHandle))
            return false;

        worldEditorManager.restreamWorld(worldHandle);

        return true;
    }

    // Utility \\

    long toBlock(double imagePosition) {
        return (long) Math.floor(imagePosition * resolvePixelBlocks());
    }

    private double resolvePixelBlocks() {
        return EngineSetting.CHUNKS_PER_PIXEL * (double) EngineSetting.CHUNK_SIZE;
    }
}
