package application.bootstrap.vehiclepipeline.vehiclemanager;

import java.util.Arrays;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.bootstrap.vehiclepipeline.vehicle.VehicleGridStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Coordinate3Long;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

class VehicleGeometryBuilder extends BuilderPackage {

    /*
     * Turns a vehicle's parts into sub-voxels and meshes. Parts fill their
     * boxes in data order, a later part taking a cell from an earlier one.
     * Every solid part fills the solid grid riders and cargo collide with;
     * every part that never moves is meshed together into the hull meshes,
     * and every moving part is meshed alone. The mesher sweeps one plane of
     * sub-voxel faces at a time, keeps only faces open to an empty cell, and
     * merges each part's faces greedily across the whole plane, block
     * boundaries included, so a long plank is one quad end to end. A face
     * carries only its part's texture corner and the vehicle shader repeats
     * the texture once per block. Faces are written through SubVoxelManager,
     * and a mesh is cut before it passes the mesh vertex limit.
     */

    // Internal
    private SubVoxelManager subVoxelManager;

    // Settings
    private int resolution;

    // Scratch
    private int[] blockScratch;
    private int[] cellScratch;
    private float[] minScratch;
    private float[] maxScratch;

    // Plane — the masks of the plane being swept, one per block it crosses, and spare masks to fill
    private Long2ObjectOpenHashMap<int[]> planeMasks;
    private LongArrayList planeOrder;
    private ObjectArrayList<int[]> spareMasks;
    private long lastMaskKey;
    private int[] lastMask;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.resolution = EngineSetting.SUB_VOXEL_RESOLUTION;

        // Scratch
        this.blockScratch = new int[EngineSetting.AXIS_COUNT];
        this.cellScratch = new int[EngineSetting.AXIS_COUNT];
        this.minScratch = new float[EngineSetting.AXIS_COUNT];
        this.maxScratch = new float[EngineSetting.AXIS_COUNT];

        // Plane
        this.planeMasks = new Long2ObjectOpenHashMap<>();
        this.planeOrder = new LongArrayList();
        this.spareMasks = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.subVoxelManager = get(SubVoxelManager.class);
    }

    // Grid \\

    VehicleGridStruct buildSolidGrid(ObjectArrayList<VehiclePartStruct> parts) {

        VehicleGridStruct grid = new VehicleGridStruct();

        for (int partIndex = 0; partIndex < parts.size(); partIndex++)
            if (parts.get(partIndex).getRole().isSolid())
                rasterize(grid, parts.get(partIndex), partIndex);

        return grid;
    }

    private void rasterize(VehicleGridStruct grid, VehiclePartStruct part, int partIndex) {

        for (int box = 0; box < part.getBoxCount(); box++)
            grid.fillBox(
                    part.getBoxBound(box, EngineSetting.BOX_MIN_X),
                    part.getBoxBound(box, EngineSetting.BOX_MIN_Y),
                    part.getBoxBound(box, EngineSetting.BOX_MIN_Z),
                    part.getBoxBound(box, EngineSetting.BOX_MAX_X),
                    part.getBoxBound(box, EngineSetting.BOX_MAX_Y),
                    part.getBoxBound(box, EngineSetting.BOX_MAX_Z),
                    partIndex);
    }

    // Meshes \\

    // Every part that never moves merged into as few meshes as the limit allows
    ObjectArrayList<MeshInstance> buildHullMeshes(ObjectArrayList<VehiclePartStruct> parts) {

        VehicleGridStruct grid = new VehicleGridStruct();

        for (int partIndex = 0; partIndex < parts.size(); partIndex++)
            if (parts.get(partIndex).getRole().isStatic())
                rasterize(grid, parts.get(partIndex), partIndex);

        return mesh(grid, parts);
    }

    // Every moving part meshed alone, so it can be drawn through its own transform
    void buildPartMeshes(ObjectArrayList<VehiclePartStruct> parts) {

        for (int partIndex = 0; partIndex < parts.size(); partIndex++) {

            VehiclePartStruct part = parts.get(partIndex);

            if (part.getRole().isStatic())
                continue;

            VehicleGridStruct grid = new VehicleGridStruct();
            rasterize(grid, part, partIndex);

            ObjectArrayList<MeshInstance> meshes = mesh(grid, parts);

            for (int i = 0; i < meshes.size(); i++)
                part.addMesh(meshes.get(i));
        }
    }

    // Mesh \\

    private ObjectArrayList<MeshInstance> mesh(VehicleGridStruct grid, ObjectArrayList<VehiclePartStruct> parts) {

        float[] partUVBounds = resolveUVBounds(parts);
        ObjectArrayList<MeshInstance> meshes = new ObjectArrayList<>();
        FloatArrayList vertices = new FloatArrayList();
        ShortArrayList indices = new ShortArrayList();

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++) {

            Int2ObjectOpenHashMap<LongArrayList> layer2Blocks = gatherLayers(grid, axis);
            int[] layers = layer2Blocks.keySet().toIntArray();
            Arrays.sort(layers);

            for (int face = 0; face < EngineSetting.SUB_VOXEL_FACE_COUNT; face++) {

                if (subVoxelManager.getFaceNormal(face, axis) == 0)
                    continue;

                for (int i = 0; i < layers.length; i++)
                    for (int slice = 0; slice < resolution; slice++)
                        meshPlane(grid, layer2Blocks.get(layers[i]), face, axis, layers[i], slice,
                                partUVBounds, meshes, vertices, indices);
            }
        }

        if (!indices.isEmpty())
            meshes.add(subVoxelManager.createMesh(vertices, indices));

        return meshes;
    }

    // The texture corner of every part, four floats each
    private float[] resolveUVBounds(ObjectArrayList<VehiclePartStruct> parts) {

        float[] uvBounds = new float[parts.size() * EngineSetting.SUB_VOXEL_UV_BOUNDS_FLOATS];

        for (int partIndex = 0; partIndex < parts.size(); partIndex++)
            writeTextureCorner(parts.get(partIndex).getTextureName(), uvBounds, partIndex);

        return uvBounds;
    }

    // A texture's corner written as both its corners, so every face of it carries the corner alone
    private void writeTextureCorner(String textureName, float[] uvBounds, int texture) {

        int base = texture * EngineSetting.SUB_VOXEL_UV_BOUNDS_FLOATS;

        subVoxelManager.writeUVBounds(textureName, uvBounds, base);
        uvBounds[base + 2] = uvBounds[base];
        uvBounds[base + 3] = uvBounds[base + 1];
    }

    // Every block of the grid by its block coordinate along the axis
    private Int2ObjectOpenHashMap<LongArrayList> gatherLayers(VehicleGridStruct grid, int axis) {

        Int2ObjectOpenHashMap<LongArrayList> layer2Blocks = new Int2ObjectOpenHashMap<>();
        LongIterator blocks = grid.getBlocks().iterator();

        while (blocks.hasNext()) {

            long block = blocks.nextLong();
            int layer = axis == EngineSetting.AXIS_X ? Coordinate3Long.unpackX(block)
                    : axis == EngineSetting.AXIS_Y ? Coordinate3Long.unpackY(block) : Coordinate3Long.unpackZ(block);
            LongArrayList layerBlocks = layer2Blocks.get(layer);

            if (layerBlocks == null) {
                layerBlocks = new LongArrayList();
                layer2Blocks.put(layer, layerBlocks);
            }

            layerBlocks.add(block);
        }

        return layer2Blocks;
    }

    // Plane \\

    // One slice of one layer of blocks facing one way: every exposed face gathered, then merged across the plane
    private void meshPlane(
            VehicleGridStruct grid,
            LongArrayList blocks,
            int face,
            int axis,
            int layer,
            int slice,
            float[] partUVBounds,
            ObjectArrayList<MeshInstance> meshes,
            FloatArrayList vertices,
            ShortArrayList indices) {

        int normal = subVoxelManager.getFaceNormal(face, axis);
        int uAxis = (axis + 1) % EngineSetting.AXIS_COUNT;
        int vAxis = (axis + 2) % EngineSetting.AXIS_COUNT;

        for (int i = 0; i < blocks.size(); i++) {

            long block = blocks.getLong(i);

            blockScratch[EngineSetting.AXIS_X] = Coordinate3Long.unpackX(block);
            blockScratch[EngineSetting.AXIS_Y] = Coordinate3Long.unpackY(block);
            blockScratch[EngineSetting.AXIS_Z] = Coordinate3Long.unpackZ(block);

            int[] mask = acquireMask();

            if (fillMask(grid, axis, normal, slice, mask)) {

                long key = Coordinate2Long.pack(blockScratch[uAxis], blockScratch[vAxis]);

                planeMasks.put(key, mask);
                planeOrder.add(key);
            } else
                spareMasks.add(mask);
        }

        LongArrays.quickSort(planeOrder.elements(), 0, planeOrder.size(), this::comparePlaneKeys);

        int plane = layer * resolution + (normal > 0 ? slice + 1 : slice);

        for (int i = 0; i < planeOrder.size(); i++)
            mergePlaneMask(planeOrder.getLong(i), face, axis, plane, partUVBounds, meshes, vertices, indices);

        releasePlane();
    }

    // Blocks of a plane row by row, so faces grow along a row before they grow across rows
    private int comparePlaneKeys(long first, long second) {

        int byRow = Integer.compare(Coordinate2Long.unpackY(first), Coordinate2Long.unpackY(second));

        return byRow != 0 ? byRow : Integer.compare(Coordinate2Long.unpackX(first), Coordinate2Long.unpackX(second));
    }

    // Each cell of the block's slice holds its one-based part where its face opens onto an empty cell, else empty —
    // true when any face is exposed
    private boolean fillMask(VehicleGridStruct grid, int axis, int normal, int slice, int[] mask) {

        int uAxis = (axis + 1) % EngineSetting.AXIS_COUNT;
        int vAxis = (axis + 2) % EngineSetting.AXIS_COUNT;
        byte[] cells = grid.getBlockCells(
                blockScratch[EngineSetting.AXIS_X], blockScratch[EngineSetting.AXIS_Y],
                blockScratch[EngineSetting.AXIS_Z]);
        byte[] neighbourCells = grid.getBlockCells(
                blockScratch[EngineSetting.AXIS_X] + (axis == EngineSetting.AXIS_X ? normal : 0),
                blockScratch[EngineSetting.AXIS_Y] + (axis == EngineSetting.AXIS_Y ? normal : 0),
                blockScratch[EngineSetting.AXIS_Z] + (axis == EngineSetting.AXIS_Z ? normal : 0));
        int neighbourSlice = slice + normal;
        byte[] facing = neighbourSlice >= 0 && neighbourSlice < resolution ? cells : neighbourCells;
        int facingSlice = Math.floorMod(neighbourSlice, resolution);
        boolean exposed = false;

        for (int b = 0; b < resolution; b++)
            for (int a = 0; a < resolution; a++) {

                cellScratch[uAxis] = a;
                cellScratch[vAxis] = b;
                cellScratch[axis] = slice;

                int part = cells[VehicleGridStruct.toCellIndex(cellScratch[0], cellScratch[1], cellScratch[2])] & 0xFF;
                mask[a + b * resolution] = EngineSetting.SUB_VOXEL_EMPTY_CELL;

                if (part == EngineSetting.SUB_VOXEL_EMPTY_CELL)
                    continue;

                cellScratch[axis] = facingSlice;

                if (facing != null && facing[VehicleGridStruct.toCellIndex(cellScratch[0], cellScratch[1],
                        cellScratch[2])] != EngineSetting.SUB_VOXEL_EMPTY_CELL)
                    continue;

                mask[a + b * resolution] = part;
                exposed = true;
            }

        return exposed;
    }

    // Every face left in one block's mask grown as far across the plane as the same part reaches, then written
    private void mergePlaneMask(
            long key,
            int face,
            int axis,
            int plane,
            float[] partUVBounds,
            ObjectArrayList<MeshInstance> meshes,
            FloatArrayList vertices,
            ShortArrayList indices) {

        int[] mask = planeMasks.get(key);
        int originU = Coordinate2Long.unpackX(key) * resolution;
        int originV = Coordinate2Long.unpackY(key) * resolution;

        for (int b = 0; b < resolution; b++)
            for (int a = 0; a < resolution; a++) {

                int value = mask[a + b * resolution];

                if (value == EngineSetting.SUB_VOXEL_EMPTY_CELL)
                    continue;

                int u = originU + a;
                int v = originV + b;
                int width = measureWidth(value, u, v);
                int height = measureHeight(value, u, v, width);

                for (int h = 0; h < height; h++)
                    for (int w = 0; w < width; w++)
                        setPlaneCell(u + w, v + h, EngineSetting.SUB_VOXEL_EMPTY_CELL);

                if (vertices.size() / EngineSetting.SUB_VOXEL_VERTEX_STRIDE + EngineSetting.QUAD_VERTEX_COUNT
                        > EngineSetting.MESH_VERT_LIMIT)
                    flush(meshes, vertices, indices);

                emitQuad(face, axis, plane, u, v, width, height, value - 1, partUVBounds, vertices, indices);
            }
    }

    private int measureWidth(int value, int u, int v) {

        int width = 1;

        while (getPlaneCell(u + width, v) == value)
            width++;

        return width;
    }

    private int measureHeight(int value, int u, int v, int width) {

        int height = 1;

        while (true) {

            for (int w = 0; w < width; w++)
                if (getPlaneCell(u + w, v + height) != value)
                    return height;

            height++;
        }
    }

    // A cell of the plane in plane sub-voxels, empty where no block's mask reaches
    private int getPlaneCell(int u, int v) {

        int[] mask = findPlaneMask(u, v);

        return mask == null ? EngineSetting.SUB_VOXEL_EMPTY_CELL
                : mask[Math.floorMod(u, resolution) + Math.floorMod(v, resolution) * resolution];
    }

    private void setPlaneCell(int u, int v, int value) {
        findPlaneMask(u, v)[Math.floorMod(u, resolution) + Math.floorMod(v, resolution) * resolution] = value;
    }

    private int[] findPlaneMask(int u, int v) {

        long key = Coordinate2Long.pack(Math.floorDiv(u, resolution), Math.floorDiv(v, resolution));

        if (key == lastMaskKey && lastMask != null)
            return lastMask;

        lastMaskKey = key;
        lastMask = planeMasks.get(key);

        return lastMask;
    }

    private int[] acquireMask() {
        return spareMasks.isEmpty() ? new int[resolution * resolution] : spareMasks.pop();
    }

    // Every mask of the plane goes back to the spares for the next
    private void releasePlane() {

        for (int i = 0; i < planeOrder.size(); i++)
            spareMasks.add(planeMasks.get(planeOrder.getLong(i)));

        planeMasks.clear();
        planeOrder.clear();
        lastMask = null;
    }

    // Emit \\

    private void emitQuad(
            int face,
            int axis,
            int plane,
            int u,
            int v,
            int width,
            int height,
            int partIndex,
            float[] partUVBounds,
            FloatArrayList vertices,
            ShortArrayList indices) {

        int uAxis = (axis + 1) % EngineSetting.AXIS_COUNT;
        int vAxis = (axis + 2) % EngineSetting.AXIS_COUNT;
        float scale = 1f / resolution;

        minScratch[axis] = plane * scale;
        maxScratch[axis] = minScratch[axis];
        minScratch[uAxis] = u * scale;
        maxScratch[uAxis] = (u + width) * scale;
        minScratch[vAxis] = v * scale;
        maxScratch[vAxis] = (v + height) * scale;

        subVoxelManager.emitFace(
                face,
                minScratch,
                maxScratch,
                0f,
                0f,
                0f,
                partUVBounds,
                partIndex * EngineSetting.SUB_VOXEL_UV_BOUNDS_FLOATS,
                vertices,
                indices);
    }

    // The geometry gathered so far becomes one mesh, and the next starts empty
    private void flush(ObjectArrayList<MeshInstance> meshes, FloatArrayList vertices, ShortArrayList indices) {

        meshes.add(subVoxelManager.createMesh(vertices, indices));

        vertices.clear();
        indices.clear();
    }
}
