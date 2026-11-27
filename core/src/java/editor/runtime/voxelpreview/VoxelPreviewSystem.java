package editor.runtime.voxelpreview;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelGridStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinition.ItemShapeStruct;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import editor.runtime.EditorSetting;
import editor.runtime.viewcamera.ViewCameraSubject;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class VoxelPreviewSystem extends SystemPackage implements ViewCameraSubject {

    /*
     * The one preview every voxel editor tab builds what it shows into, the
     * Architecture Builder, the Structure Editor and the Layout Planner
     * alike, and the subject their shared view camera frames. Between
     * begin() and finish() a tab lays whole or partial blocks, each by its
     * tile and the octants it fills, and boxes of sub-voxels into one
     * sub-voxel grid, one part to each tile, and stands items at the corner
     * of their model grid; finish() greedily meshes the grid through
     * SubVoxelManager, the last meshes freed. Positions are model blocks from
     * the preview's corner and never lie below zero. VoxelPreviewRenderSystem
     * draws what is laid.
     */

    // Internal
    private SubVoxelManager subVoxelManager;
    private MeshManager meshManager;

    // Meshing
    private SubVoxelGridStruct grid;
    private Int2IntOpenHashMap tile2Part;
    private IntArrayList partTiles;

    // Shown — the meshes, every item with its corner in model blocks and its orientation, and the bounds
    private ObjectArrayList<MeshInstance> meshes;
    private ObjectArrayList<ItemDefinitionHandle> items;
    private FloatArrayList itemCorners;
    private IntArrayList itemOrientations;
    private float maxX;
    private float maxY;
    private float maxZ;

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
        this.items = new ObjectArrayList<>();
        this.itemCorners = new FloatArrayList();
        this.itemOrientations = new IntArrayList();
    }

    @Override
    protected void get() {
        this.subVoxelManager = get(SubVoxelManager.class);
        this.meshManager = get(MeshManager.class);
    }

    @Override
    protected void dispose() {
        releaseMeshes();
    }

    // Build \\

    // Everything laid before let go, the last meshes freed
    public void begin() {

        releaseMeshes();
        grid.clear();
        tile2Part.clear();
        partTiles.clear();
        items.clear();
        itemCorners.clear();
        itemOrientations.clear();
        this.maxX = 0f;
        this.maxY = 0f;
        this.maxZ = 0f;
    }

    // A block laid by its tile, a partial one only the octants it fills; nothing below zero or past the parts a grid
    // holds
    public void layBlock(int x, int y, int z, int tile, int mask) {

        int part = resolvePart(tile);

        if (x < 0 || y < 0 || z < 0 || part == EngineSetting.INDEX_NOT_FOUND)
            return;

        extend(x + 1, y + 1, z + 1);

        if (mask == SubBlockUtility.MASK_FULL) {
            grid.fillBlock(x, y, z, part, false);
            return;
        }

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        int half = resolution / SubBlockUtility.DIVISIONS;

        for (int octant = 0; octant < SubBlockUtility.OCTANT_COUNT; octant++) {

            if (!SubBlockUtility.hasOctant(mask, octant))
                continue;

            int minX = x * resolution + SubBlockUtility.getOctantX(octant) * half;
            int minY = y * resolution + SubBlockUtility.getOctantY(octant) * half;
            int minZ = z * resolution + SubBlockUtility.getOctantZ(octant) * half;

            grid.fillBox(minX, minY, minZ, minX + half, minY + half, minZ + half, part, false);
        }
    }

    // A box of sub-voxels laid by its tile, minimum inclusive and maximum exclusive
    public void layBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int tile) {

        int part = resolvePart(tile);

        if (minX < 0 || minY < 0 || minZ < 0 || part == EngineSetting.INDEX_NOT_FOUND)
            return;

        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;

        extend(maxX / resolution, maxY / resolution, maxZ / resolution);
        grid.fillBox(minX, minY, minZ, maxX, maxY, maxZ, part, false);
    }

    // An item stood at the corner of its model grid, in model blocks, turned to an orientation
    public void addItem(ItemDefinitionHandle item, float cornerX, float cornerY, float cornerZ, int orientation) {

        ItemShapeStruct shape = item.getShape();
        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;

        items.add(item);
        itemCorners.add(cornerX);
        itemCorners.add(cornerY);
        itemCorners.add(cornerZ);
        itemOrientations.add(orientation);
        extend(
                cornerX + (shape.getOffsetX() + shape.getSizeX()) / resolution,
                cornerY + (shape.getOffsetY() + shape.getSizeY()) / resolution,
                cornerZ + (shape.getOffsetZ() + shape.getSizeZ()) / resolution);
    }

    // The grid meshed for drawing
    public void finish() {

        if (!grid.isEmpty())
            meshes.addAll(subVoxelManager.createGridMeshes(grid, partTiles.toIntArray()));
    }

    // The grid part a tile is drawn as, given one the first time it is met; INDEX_NOT_FOUND for no tile or past the
    // parts a grid holds
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

    private void extend(float x, float y, float z) {

        this.maxX = Math.max(maxX, x);
        this.maxY = Math.max(maxY, y);
        this.maxZ = Math.max(maxZ, z);
    }

    private void releaseMeshes() {

        for (int i = 0; i < meshes.size(); i++)
            meshManager.removeMesh(meshes.get(i));

        meshes.clear();
    }

    // Accessible \\

    // True while anything is laid
    @Override
    public boolean isShown() {
        return !meshes.isEmpty() || !items.isEmpty();
    }

    // The middle of what is laid on an axis, in model blocks from the corner
    @Override
    public float getCenter(int axis) {
        return switch (axis) {
            case EngineSetting.AXIS_X -> maxX * 0.5f;
            case EngineSetting.AXIS_Y -> maxY * 0.5f;
            default -> maxZ * 0.5f;
        };
    }

    // Half the diagonal of what is laid, in model blocks, never smaller than a block
    @Override
    public float getBoundingRadius() {
        return Math.max(
                EditorSetting.VOXEL_PREVIEW_MIN_RADIUS_BLOCKS,
                0.5f * (float) Math.sqrt(maxX * maxX + maxY * maxY + maxZ * maxZ));
    }

    public ObjectArrayList<MeshInstance> getMeshes() {
        return meshes;
    }

    public ObjectArrayList<ItemDefinitionHandle> getItems() {
        return items;
    }

    // An item's corner on an axis, in model blocks from the corner
    public float getItemCorner(int index, int axis) {
        return itemCorners.getFloat(index * EngineSetting.AXIS_COUNT + axis);
    }

    public int getItemOrientation(int index) {
        return itemOrientations.getInt(index);
    }
}
