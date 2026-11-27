package editor.runtime.structureeditor.view;

import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import editor.bootstrap.structureeditorpipeline.structureeditor.StructureEditorPartStruct;
import editor.bootstrap.structureeditorpipeline.structureeditormanager.StructureEditorManager;
import editor.runtime.structureeditor.StructureEditorSetting;
import editor.runtime.structureeditor.tool.StructureEditorHitStruct;
import editor.runtime.voxelpreview.VoxelPreviewSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.extras.Coordinate3Long;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StructureEditorViewSystem extends SystemPackage {

    /*
     * Lays out what the Structure Editor shows of every shown layer into the
     * window's VoxelPreviewSystem, one block a model block from the
     * structure's corner: each cell as the job picked draws it, each NPC as a
     * marker post standing in its cell, and each piece of furniture by the
     * corner of its model grid and its turn. The layout is made again only
     * when the editor's revision moves. It also walks the pointer's ray
     * through the cells, cell by cell, to the first one it stops at, or to
     * the ground plane under the structure where it stops at none.
     */

    // Faces — the face a ray enters a cell through, by the axis it crossed and whether it was stepping up that axis
    private static final Direction3Vector[] ENTERED_FACES = {
            Direction3Vector.WEST, Direction3Vector.EAST,
            Direction3Vector.DOWN, Direction3Vector.UP,
            Direction3Vector.SOUTH, Direction3Vector.NORTH };

    // Internal
    private StructureEditorManager structureEditorManager;
    private VoxelPreviewSystem voxelPreviewSystem;

    // Shown
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.structureEditorManager = get(StructureEditorManager.class);
        this.voxelPreviewSystem = get(VoxelPreviewSystem.class);
    }

    // Update \\

    @Override
    protected void update() {

        int revision = structureEditorManager.getRevision();

        if (revision == shownRevision)
            return;

        this.shownRevision = revision;
        layOut();
    }

    // Every shown cell, NPC and piece of furniture laid afresh
    private void layOut() {

        voxelPreviewSystem.begin();

        for (Long2ObjectMap.Entry<ArpgObjectStruct> cell : structureEditorManager.getShownCells()
                .long2ObjectEntrySet())
            voxelPreviewSystem.layBlock(
                    Coordinate3Long.unpackX(cell.getLongKey()),
                    Coordinate3Long.unpackY(cell.getLongKey()),
                    Coordinate3Long.unpackZ(cell.getLongKey()),
                    structureEditorManager.resolveCellTile(cell.getValue()),
                    structureEditorManager.resolveCellMask(cell.getValue()));

        ObjectArrayList<StructureEditorPartStruct> parts = structureEditorManager.getParts();

        for (int i = 0; i < parts.size(); i++)
            if (parts.get(i).isShown())
                layPlaced(parts.get(i));

        voxelPreviewSystem.finish();
    }

    // Every NPC of a layer as a marker post in its cell, every piece of its furniture by its corner and turn
    private void layPlaced(StructureEditorPartStruct part) {

        int markerTile = structureEditorManager.resolveSpawnTile();
        ObjectArrayList<ArpgObjectStruct> spawns = part.getSpawns();

        for (int i = 0; i < spawns.size(); i++) {

            long cell = structureEditorManager.resolvePlacedCell(spawns.get(i));

            if (cell != Long.MIN_VALUE)
                layMarker(cell, markerTile);
        }

        ObjectArrayList<ArpgObjectStruct> furnishings = part.getFurnishings();

        for (int i = 0; i < furnishings.size(); i++)
            layFurniture(furnishings.get(i));
    }

    // A thin post through the middle of an NPC's cell, as tall as the cell
    private void layMarker(long cell, int tile) {

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        int width = StructureEditorSetting.SPAWN_MARKER_WIDTH_SUB_VOXELS;
        int minX = Coordinate3Long.unpackX(cell) * resolution + (resolution - width) / 2;
        int minY = Coordinate3Long.unpackY(cell) * resolution;
        int minZ = Coordinate3Long.unpackZ(cell) * resolution + (resolution - width) / 2;

        voxelPreviewSystem.layBox(minX, minY, minZ, minX + width, minY + resolution, minZ + width, tile);
    }

    // A piece of furniture stood at its exact corner from the origin, turned as it stands
    private void layFurniture(ArpgObjectStruct entry) {

        ItemDefinitionHandle item = structureEditorManager.resolveFurnitureItem(entry);

        if (item == null || structureEditorManager.resolvePlacedCell(entry) == Long.MIN_VALUE)
            return;

        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;

        voxelPreviewSystem.addItem(
                item,
                structureEditorManager.getOrigin(EngineSetting.AXIS_X)
                        + entry.getAsArray("corner").get(EngineSetting.AXIS_X).getAsInt() / resolution,
                structureEditorManager.getOrigin(EngineSetting.AXIS_Y)
                        + entry.getAsArray("corner").get(EngineSetting.AXIS_Y).getAsInt() / resolution,
                structureEditorManager.getOrigin(EngineSetting.AXIS_Z)
                        + entry.getAsArray("corner").get(EngineSetting.AXIS_Z).getAsInt() / resolution,
                EngineSetting.DEFAULT_BLOCK_ORIENTATION
                        + Math.floorMod(ArpgUtility.getInt(entry, "spin", 0), EngineSetting.ENCODED_FACE_SPIN_COUNT));
    }

    // Cast \\

    // The first cell the ray strikes, walked cell by cell, else the ground plane under the structure it crosses
    public void cast(Vector3 origin, Vector3 direction, StructureEditorHitStruct hit) {

        hit.clear();

        int cellX = (int) Math.floor(origin.x);
        int cellY = (int) Math.floor(origin.y);
        int cellZ = (int) Math.floor(origin.z);
        int stepX = direction.x >= 0f ? 1 : -1;
        int stepY = direction.y >= 0f ? 1 : -1;
        int stepZ = direction.z >= 0f ? 1 : -1;
        float deltaX = direction.x == 0f ? Float.MAX_VALUE : Math.abs(1f / direction.x);
        float deltaY = direction.y == 0f ? Float.MAX_VALUE : Math.abs(1f / direction.y);
        float deltaZ = direction.z == 0f ? Float.MAX_VALUE : Math.abs(1f / direction.z);
        float nextX = deltaX * (stepX > 0 ? cellX + 1 - origin.x : origin.x - cellX);
        float nextY = deltaY * (stepY > 0 ? cellY + 1 - origin.y : origin.y - cellY);
        float nextZ = deltaZ * (stepZ > 0 ? cellZ + 1 - origin.z : origin.z - cellZ);

        for (int step = 0; step < StructureEditorSetting.CAST_MAX_STEPS; step++) {

            int axis;

            if (nextX <= nextY && nextX <= nextZ) {
                cellX += stepX;
                nextX += deltaX;
                axis = EngineSetting.AXIS_X;
            } else if (nextY <= nextZ) {
                cellY += stepY;
                nextY += deltaY;
                axis = EngineSetting.AXIS_Y;
            } else {
                cellZ += stepZ;
                nextZ += deltaZ;
                axis = EngineSetting.AXIS_Z;
            }

            int stepSign = axis == EngineSetting.AXIS_X ? stepX : axis == EngineSetting.AXIS_Y ? stepY : stepZ;

            if (cellX >= 0 && cellY >= 0 && cellZ >= 0
                    && structureEditorManager.strikes(Coordinate3Long.pack(cellX, cellY, cellZ))) {
                hit.set(cellX, cellY, cellZ, ENTERED_FACES[axis * 2 + (stepSign > 0 ? 0 : 1)]);
                return;
            }
        }

        castGround(origin, direction, hit);
    }

    // The ground plane the structure stands on struck from above, as the top face of the cell under it
    private void castGround(Vector3 origin, Vector3 direction, StructureEditorHitStruct hit) {

        if (direction.y >= 0f || origin.y <= 0f)
            return;

        float distance = -origin.y / direction.y;

        hit.set(
                (int) Math.floor(origin.x + direction.x * distance),
                -1,
                (int) Math.floor(origin.z + direction.z * distance),
                Direction3Vector.UP);
    }
}
