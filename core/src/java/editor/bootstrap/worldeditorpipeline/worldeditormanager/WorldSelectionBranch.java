package editor.bootstrap.worldeditorpipeline.worldeditormanager;

import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import editor.bootstrap.worldeditorpipeline.util.WorldEditorMode;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class WorldSelectionBranch extends BranchPackage {

    /*
     * Keeps the hand placements the Select tool has picked, following the
     * editor's selection conventions: a click picks the placement nearest the
     * pointer alone, or clears the selection on nothing; Shift toggles one in
     * or out and a box adds what it holds; Ctrl takes one or a box's
     * contents out. Only the kinds of placement the mode places are picked,
     * so placing settlements never takes up a camera nor placing cameras a
     * settlement. The placement picked last is handed to the planner that
     * designs its kind to edit, and letting the selection go lets it go
     * there too. Everything selected moves together by the same number of
     * blocks, every move going through WorldPlacementBranch as the planners'
     * do, and is taken away together. Placements that no longer stand drop
     * out.
     */

    // Internal
    private WorldManager worldManager;
    private WorldEditorManager worldEditorManager;
    private WorldPlacementBranch worldPlacementBranch;

    // Selection
    private ObjectArrayList<WorldPlacementStruct> selected;
    private int shownPlacementRevision;

    // Base \\

    @Override
    protected void create() {

        // Selection
        this.selected = new ObjectArrayList<>();
        this.shownPlacementRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.worldManager = get(WorldManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
        this.worldPlacementBranch = get(WorldPlacementBranch.class);
    }

    // Sync \\

    // Placements cleared or reloaded away from under the selection leave it
    void sync(WorldHandle worldHandle) {

        int placementRevision = worldManager.getPlacementRevision();

        if (placementRevision == shownPlacementRevision)
            return;

        this.shownPlacementRevision = placementRevision;

        WorldPlacementStruct[] placements = worldManager.getPlacements(worldHandle);

        for (int i = selected.size() - 1; i >= 0; i--)
            if (!stands(placements, selected.get(i)))
                selected.remove(i);
    }

    private boolean stands(WorldPlacementStruct[] placements, WorldPlacementStruct placement) {

        for (int i = 0; i < placements.length; i++)
            if (placements[i] == placement)
                return true;

        return false;
    }

    // Pick \\

    // The placement the mode holds nearest an image position within a reach in image pixels, null when none is near
    WorldPlacementStruct findNear(WorldHandle worldHandle, double imageX, double imageY, double reachPixels) {

        WorldPlacementStruct[] placements = worldManager.getPlacements(worldHandle);
        WorldEditorMode mode = worldEditorManager.getMode();
        double pixelBlocks = resolvePixelBlocks();
        double nearestSq = reachPixels * reachPixels;
        WorldPlacementStruct nearest = null;

        for (int i = 0; i < placements.length; i++) {

            if (!mode.holds(placements[i].getKind()))
                continue;

            double deltaX = WorldWrapUtility.wrappedBlockDeltaX(
                    worldHandle, placements[i].getWorldX(), imageX * pixelBlocks) / pixelBlocks;
            double deltaY = WorldWrapUtility.wrappedBlockDeltaZ(
                    worldHandle, placements[i].getWorldZ(), imageY * pixelBlocks) / pixelBlocks;
            double distanceSq = deltaX * deltaX + deltaY * deltaY;

            if (distanceSq > nearestSq)
                continue;

            nearestSq = distanceSq;
            nearest = placements[i];
        }

        return nearest;
    }

    // Selection \\

    // A click picks alone, Shift toggles, Ctrl takes out; a click on nothing clears unless a modifier is held
    void click(WorldPlacementStruct placement, boolean add, boolean subtract) {

        if (placement == null) {

            if (!add && !subtract)
                clear();

            return;
        }

        if (subtract) {
            deselect(placement);
            return;
        }

        if (add && selected.contains(placement)) {
            deselect(placement);
            return;
        }

        if (!add)
            selected.clear();

        selected.add(placement);
        worldEditorManager.editPlacement(placement);
        describe();
    }

    // Every placement in an image rectangle joins, leaves, or alone becomes the selection
    void box(
            WorldHandle worldHandle,
            double minImageX,
            double minImageY,
            double maxImageX,
            double maxImageY,
            boolean add,
            boolean subtract) {

        WorldPlacementStruct[] placements = worldManager.getPlacements(worldHandle);
        WorldEditorMode mode = worldEditorManager.getMode();
        double pixelBlocks = resolvePixelBlocks();

        if (!add && !subtract)
            selected.clear();

        for (int i = 0; i < placements.length; i++) {

            if (!mode.holds(placements[i].getKind()))
                continue;

            double pixelX = WorldWrapUtility.wrapBlockX(worldHandle, placements[i].getWorldX()) / pixelBlocks;
            double pixelY = WorldWrapUtility.wrapBlockZ(worldHandle, placements[i].getWorldZ()) / pixelBlocks;

            if (pixelX < minImageX || pixelX > maxImageX || pixelY < minImageY || pixelY > maxImageY)
                continue;

            if (subtract)
                selected.remove(placements[i]);
            else if (!selected.contains(placements[i]))
                selected.add(placements[i]);
        }

        if (selected.isEmpty())
            worldEditorManager.detachPlacement();

        describe();
    }

    private void deselect(WorldPlacementStruct placement) {

        selected.remove(placement);

        if (worldEditorManager.isPlacementEdited(placement))
            worldEditorManager.detachPlacement();

        describe();
    }

    void clear() {

        if (selected.isEmpty())
            return;

        selected.clear();
        worldEditorManager.detachPlacement();
        describe();
    }

    // Edit \\

    // Everything selected stood the same number of blocks away, each refused move leaving its placement as it was
    void move(WorldHandle worldHandle, long deltaBlocksX, long deltaBlocksZ) {

        String refusal = null;

        for (int i = 0; i < selected.size(); i++) {

            WorldPlacementStruct previous = selected.get(i);
            WorldPlacementStruct moved = previous.moveTo(
                    previous.getWorldX() + deltaBlocksX,
                    previous.getWorldZ() + deltaBlocksZ);
            String moveRefusal = worldPlacementBranch.replace(worldHandle, previous, moved);

            if (moveRefusal == null)
                selected.set(i, moved);
            else
                refusal = moveRefusal;
        }

        this.shownPlacementRevision = worldManager.getPlacementRevision();

        if (!selected.isEmpty())
            worldEditorManager.editPlacement(selected.get(selected.size() - 1));

        worldEditorManager.setStatusMessage(refusal != null ? refusal : EditorSetting.WORLD_EDITOR_MESSAGE_MOVED);
    }

    // Everything selected taken away together
    void delete(WorldHandle worldHandle) {

        if (selected.isEmpty()) {
            worldEditorManager.setStatusMessage(EditorSetting.WORLD_EDITOR_MESSAGE_NOTHING_SELECTED);
            return;
        }

        int count = selected.size();

        worldEditorManager.detachPlacement();

        for (int i = 0; i < selected.size(); i++)
            worldPlacementBranch.remove(worldHandle, selected.get(i));

        selected.clear();
        worldEditorManager.setStatusMessage(EditorSetting.WORLD_EDITOR_MESSAGE_DELETED + count);
    }

    private void describe() {
        worldEditorManager.setStatusMessage(selected.size() + EditorSetting.WORLD_EDITOR_MESSAGE_SELECTED);
    }

    // Accessible \\

    boolean isSelected(WorldPlacementStruct placement) {
        return selected.contains(placement);
    }

    boolean hasSelection() {
        return !selected.isEmpty();
    }

    // Utility \\

    private double resolvePixelBlocks() {
        return EngineSetting.CHUNKS_PER_PIXEL * (double) EngineSetting.CHUNK_SIZE;
    }
}
