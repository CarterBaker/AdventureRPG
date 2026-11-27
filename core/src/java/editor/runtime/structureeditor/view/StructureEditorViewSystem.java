package editor.runtime.structureeditor.view;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelGridStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import editor.bootstrap.structureeditorpipeline.structureeditor.StructureEditorPartStruct;
import editor.bootstrap.structureeditorpipeline.structureeditormanager.StructureEditorManager;
import editor.runtime.structureeditor.StructureEditorSetting;
import editor.runtime.structureeditor.tool.StructureEditorHitStruct;
import editor.runtime.viewcamera.ViewCameraSubject;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.extras.Coordinate3Long;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StructureEditorViewSystem extends SystemPackage implements ViewCameraSubject {

    /*
     * Lays out what the Structure Editor shows of every shown layer: each
     * cell as the job picked draws it, a whole block filling its cell and a
     * partial block only the octants it fills, and each NPC as a marker post
     * standing in its cell, rasterized into a sub-voxel grid with one part to
     * each tile and greedily meshed by SubVoxelManager, one block a model
     * block from the structure's corner; and each piece of furniture by the
     * corner of its model grid and its turn, for the render system to draw.
     * The layout is made again only when the editor's revision moves, the old
     * meshes freed. It also walks the pointer's ray through the cells, cell
     * by cell, to the first one it stops at, or to the ground plane under the
     * structure where it stops at none.
     */

    // Faces — the face a ray enters a cell through, by the axis it crossed and whether it was stepping up that axis
    private static final Direction3Vector[] ENTERED_FACES = {
            Direction3Vector.WEST, Direction3Vector.EAST,
            Direction3Vector.DOWN, Direction3Vector.UP,
            Direction3Vector.SOUTH, Direction3Vector.NORTH };

    // Internal
    private StructureEditorManager structureEditorManager;
    private SubVoxelManager subVoxelManager;
    private MeshManager meshManager;

    // Meshing
    private SubVoxelGridStruct grid;
    private Int2IntOpenHashMap tile2Part;
    private IntArrayList partTiles;

    // Shown — the meshes, and every piece of furniture with its corner in model blocks and its orientation
    private ObjectArrayList<MeshInstance> meshes;
    private ObjectArrayList<ItemDefinitionHandle> furnitureItems;
    private FloatArrayList furnitureCorners;
    private IntArrayList furnitureOrientations;
    private int maxX;
    private int maxY;
    private int maxZ;
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // Meshing
        this.grid = new SubVoxelGridStruct();
        this.tile2Part = new Int2IntOpenHashMap();
        this.tile2Part.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
        this.partTiles = new IntArrayList();

        // Shown
        this.meshes = new ObjectArrayList<>();
        this.furnitureItems = new ObjectArrayList<>();
        this.furnitureCorners = new FloatArrayList();
        this.furnitureOrientations = new IntArrayList();
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.structureEditorManager = get(StructureEditorManager.class);
        this.subVoxelManager = get(SubVoxelManager.class);
        this.meshManager = get(MeshManager.class);
    }

    @Override
    protected void dispose() {
        releaseMeshes();
    }

    // Update \\

    @Override
    protected void update() {

        int revision = structureEditorManager.getRevision();

        if (revision == shownRevision)
            return;

        this.shownRevision = revision;
        rebuild();
    }

    // Every shown cell and NPC rasterized as the job draws them and meshed afresh, and every shown furniture laid out
    private void rebuild() {

        releaseMeshes();
        grid.clear();
        tile2Part.clear();
        partTiles.clear();
        furnitureItems.clear();
        furnitureCorners.clear();
        furnitureOrientations.clear();
        this.maxX = 0;
        this.maxY = 0;
        this.maxZ = 0;

        for (Long2ObjectMap.Entry<ArpgObjectStruct> cell : structureEditorManager.getShownCells()
                .long2ObjectEntrySet())
            rasterize(cell.getLongKey(), cell.getValue());

        ObjectArrayList<StructureEditorPartStruct> parts = structureEditorManager.getParts();

        for (int i = 0; i < parts.size(); i++)
            if (parts.get(i).isShown())
                layPlaced(parts.get(i));

        if (!grid.isEmpty())
            meshes.addAll(subVoxelManager.createGridMeshes(grid, partTiles.toIntArray()));
    }

    private void rasterize(long cell, ArpgObjectStruct entry) {

        int x = Coordinate3Long.unpackX(cell);
        int y = Coordinate3Long.unpackY(cell);
        int z = Coordinate3Long.unpackZ(cell);

        extend(cell);

        int part = resolvePart(structureEditorManager.resolveCellTile(entry));

        if (part == EngineSetting.INDEX_NOT_FOUND)
            return;

        int mask = structureEditorManager.resolveCellMask(entry);

        if (mask == SubBlockUtility.MASK_FULL) {
            grid.fillBlock(x, y, z, part, false);
            return;
        }

        int half = EngineSetting.SUB_VOXEL_RESOLUTION / SubBlockUtility.DIVISIONS;

        for (int octant = 0; octant < SubBlockUtility.OCTANT_COUNT; octant++) {

            if (!SubBlockUtility.hasOctant(mask, octant))
                continue;

            int minX = x * EngineSetting.SUB_VOXEL_RESOLUTION + SubBlockUtility.getOctantX(octant) * half;
            int minY = y * EngineSetting.SUB_VOXEL_RESOLUTION + SubBlockUtility.getOctantY(octant) * half;
            int minZ = z * EngineSetting.SUB_VOXEL_RESOLUTION + SubBlockUtility.getOctantZ(octant) * half;

            grid.fillBox(minX, minY, minZ, minX + half, minY + half, minZ + half, part, false);
        }
    }

    // Every NPC of a layer as a marker post in its cell, every piece of its furniture by its corner and turn
    private void layPlaced(StructureEditorPartStruct part) {

        int markerPart = resolvePart(structureEditorManager.resolveSpawnTile());
        ObjectArrayList<ArpgObjectStruct> spawns = part.getSpawns();

        for (int i = 0; i < spawns.size(); i++) {

            long cell = structureEditorManager.resolvePlacedCell(spawns.get(i));

            if (cell != Long.MIN_VALUE && markerPart != EngineSetting.INDEX_NOT_FOUND)
                layMarker(cell, markerPart);
        }

        ObjectArrayList<ArpgObjectStruct> furnishings = part.getFurnishings();

        for (int i = 0; i < furnishings.size(); i++)
            layFurniture(furnishings.get(i));
    }

    // A thin post through the middle of an NPC's cell, as tall as the cell
    private void layMarker(long cell, int part) {

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        int inset = (resolution - StructureEditorSetting.SPAWN_MARKER_WIDTH_SUB_VOXELS) / 2;
        int minX = Coordinate3Long.unpackX(cell) * resolution + inset;
        int minY = Coordinate3Long.unpackY(cell) * resolution;
        int minZ = Coordinate3Long.unpackZ(cell) * resolution + inset;

        grid.fillBox(
                minX, minY, minZ,
                minX + StructureEditorSetting.SPAWN_MARKER_WIDTH_SUB_VOXELS, minY + resolution,
                minZ + StructureEditorSetting.SPAWN_MARKER_WIDTH_SUB_VOXELS,
                part, false);
        extend(cell);
    }

    private void layFurniture(ArpgObjectStruct entry) {

        long cell = structureEditorManager.resolvePlacedCell(entry);
        ItemDefinitionHandle item = structureEditorManager.resolveFurnitureItem(entry);

        if (cell == Long.MIN_VALUE || item == null)
            return;

        furnitureItems.add(item);

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++)
            furnitureCorners.add(structureEditorManager.getOrigin(axis)
                    + entry.getAsArray("corner").get(axis).getAsInt() / (float) EngineSetting.SUB_VOXEL_RESOLUTION);

        furnitureOrientations.add(EngineSetting.DEFAULT_BLOCK_ORIENTATION
                + Math.floorMod(ArpgUtility.getInt(entry, "spin", 0), EngineSetting.ENCODED_FACE_SPIN_COUNT));
        extend(cell);
    }

    private void extend(long cell) {

        this.maxX = Math.max(maxX, Coordinate3Long.unpackX(cell) + 1);
        this.maxY = Math.max(maxY, Coordinate3Long.unpackY(cell) + 1);
        this.maxZ = Math.max(maxZ, Coordinate3Long.unpackZ(cell) + 1);
    }

    // The grid part a tile is drawn as, given one the first time it is met; INDEX_NOT_FOUND past the parts a grid
    // holds or for a cell that draws nothing
    private int resolvePart(int tile) {

        if (tile == EngineSetting.INDEX_NOT_FOUND)
            return EngineSetting.INDEX_NOT_FOUND;

        int part = tile2Part.get(tile);

        if (part != EngineSetting.INDEX_NOT_FOUND || partTiles.size() >= EngineSetting.SUB_VOXEL_MAX_PARTS)
            return part;

        part = partTiles.size();
        partTiles.add(tile);
        tile2Part.put(tile, part);

        return part;
    }

    private void releaseMeshes() {

        for (int i = 0; i < meshes.size(); i++)
            meshManager.removeMesh(meshes.get(i));

        meshes.clear();
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

    // Accessible \\

    // True while anything is shown
    @Override
    public boolean isShown() {
        return !meshes.isEmpty() || !furnitureItems.isEmpty();
    }

    // The middle of the structure's bounds on an axis, in blocks from its corner
    @Override
    public float getCenter(int axis) {
        return switch (axis) {
            case EngineSetting.AXIS_X -> maxX * 0.5f;
            case EngineSetting.AXIS_Y -> maxY * 0.5f;
            default -> maxZ * 0.5f;
        };
    }

    // Half the diagonal of the structure's bounds, in blocks, never smaller than a block
    @Override
    public float getBoundingRadius() {
        return Math.max(
                StructureEditorSetting.MIN_RADIUS_BLOCKS,
                0.5f * (float) Math.sqrt(maxX * maxX + maxY * maxY + maxZ * maxZ));
    }

    // Every mesh drawn, in model blocks from the structure's corner
    public ObjectArrayList<MeshInstance> getMeshes() {
        return meshes;
    }

    // Every piece of furniture drawn, its corner and orientation by the same index
    public ObjectArrayList<ItemDefinitionHandle> getFurnitureItems() {
        return furnitureItems;
    }

    // A piece of furniture's corner on an axis, in model blocks from the structure's corner
    public float getFurnitureCorner(int index, int axis) {
        return furnitureCorners.getFloat(index * EngineSetting.AXIS_COUNT + axis);
    }

    public int getFurnitureOrientation(int index) {
        return furnitureOrientations.getInt(index);
    }
}
