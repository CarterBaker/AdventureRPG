package application.bootstrap.vehiclepipeline.vehiclemanager;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.bootstrap.vehiclepipeline.vehicle.VehicleGridStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate3Long;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

class VehicleGeometryBuilder extends BuilderPackage {

    /*
     * Turns a vehicle's parts into sub-voxels and meshes. Parts fill their
     * boxes in data order, a later part taking a cell from an earlier one.
     * Every solid part fills the solid grid riders and cargo collide with;
     * every part that never moves is meshed together into the hull meshes,
     * and every moving part is meshed alone. The mesher walks each touched
     * block a face at a time, keeps only faces open to an empty cell, across
     * block boundaries too, and merges each block's faces of one part
     * greedily, so a face's texels stay inside one block exactly as an item's
     * do. Faces are written through SubVoxelManager, and a mesh is cut before
     * it passes the mesh vertex limit.
     */

    // Internal
    private SubVoxelManager subVoxelManager;

    // Settings
    private int resolution;

    // Scratch
    private int[] mask;
    private int[] blockScratch;
    private float[] minScratch;
    private float[] maxScratch;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.resolution = EngineSetting.SUB_VOXEL_RESOLUTION;

        // Scratch
        this.mask = new int[resolution * resolution];
        this.blockScratch = new int[EngineSetting.AXIS_COUNT];
        this.minScratch = new float[EngineSetting.AXIS_COUNT];
        this.maxScratch = new float[EngineSetting.AXIS_COUNT];
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

    // Every part that never moves, merged into as few meshes as the vertex limit allows
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

        float[] partUVBounds = resolvePartUVBounds(parts);
        ObjectArrayList<MeshInstance> meshes = new ObjectArrayList<>();
        FloatArrayList vertices = new FloatArrayList();
        ShortArrayList indices = new ShortArrayList();
        LongIterator blocks = grid.getBlocks().iterator();

        while (blocks.hasNext()) {

            long block = blocks.nextLong();

            blockScratch[EngineSetting.AXIS_X] = Coordinate3Long.unpackX(block);
            blockScratch[EngineSetting.AXIS_Y] = Coordinate3Long.unpackY(block);
            blockScratch[EngineSetting.AXIS_Z] = Coordinate3Long.unpackZ(block);

            for (int face = 0; face < EngineSetting.SUB_VOXEL_FACE_COUNT; face++)
                meshBlockFace(grid, face, partUVBounds, meshes, vertices, indices);
        }

        if (!indices.isEmpty())
            meshes.add(subVoxelManager.createMesh(vertices, indices));

        return meshes;
    }

    private float[] resolvePartUVBounds(ObjectArrayList<VehiclePartStruct> parts) {

        float[] partUVBounds = new float[parts.size() * EngineSetting.SUB_VOXEL_UV_BOUNDS_FLOATS];

        for (int partIndex = 0; partIndex < parts.size(); partIndex++)
            subVoxelManager.writeUVBounds(
                    parts.get(partIndex).getTextureName(),
                    partUVBounds,
                    partIndex * EngineSetting.SUB_VOXEL_UV_BOUNDS_FLOATS);

        return partUVBounds;
    }

    // Every slice of one block facing one way, each exposed face merged and written
    private void meshBlockFace(
            VehicleGridStruct grid,
            int face,
            float[] partUVBounds,
            ObjectArrayList<MeshInstance> meshes,
            FloatArrayList vertices,
            ShortArrayList indices) {

        int axis = resolveAxis(face);
        int normal = subVoxelManager.getFaceNormal(face, axis);
        int blockX = blockScratch[EngineSetting.AXIS_X];
        int blockY = blockScratch[EngineSetting.AXIS_Y];
        int blockZ = blockScratch[EngineSetting.AXIS_Z];

        byte[] cells = grid.getBlockCells(blockX, blockY, blockZ);
        byte[] neighbourCells = grid.getBlockCells(
                blockX + (axis == EngineSetting.AXIS_X ? normal : 0),
                blockY + (axis == EngineSetting.AXIS_Y ? normal : 0),
                blockZ + (axis == EngineSetting.AXIS_Z ? normal : 0));

        for (int slice = 0; slice < resolution; slice++) {

            fillMask(cells, neighbourCells, axis, normal, slice);
            mergeMask(face, axis, normal, slice, partUVBounds, meshes, vertices, indices);
        }
    }

    // Each cell of the slice holds its one-based part where its face opens onto an empty cell, else empty
    private void fillMask(byte[] cells, byte[] neighbourCells, int axis, int normal, int slice) {

        int uAxis = (axis + 1) % EngineSetting.AXIS_COUNT;
        int vAxis = (axis + 2) % EngineSetting.AXIS_COUNT;
        int neighbourSlice = slice + normal;
        boolean inside = neighbourSlice >= 0 && neighbourSlice < resolution;
        byte[] facing = inside ? cells : neighbourCells;
        int facingSlice = Math.floorMod(neighbourSlice, resolution);
        int[] cell = new int[EngineSetting.AXIS_COUNT];

        for (int b = 0; b < resolution; b++)
            for (int a = 0; a < resolution; a++) {

                cell[uAxis] = a;
                cell[vAxis] = b;
                cell[axis] = slice;

                int part = cells[VehicleGridStruct.toCellIndex(cell[0], cell[1], cell[2])] & 0xFF;

                if (part == EngineSetting.SUB_VOXEL_EMPTY_CELL) {
                    mask[a + b * resolution] = EngineSetting.SUB_VOXEL_EMPTY_CELL;
                    continue;
                }

                cell[axis] = facingSlice;

                boolean covered = facing != null
                        && facing[VehicleGridStruct.toCellIndex(cell[0], cell[1], cell[2])]
                                != EngineSetting.SUB_VOXEL_EMPTY_CELL;

                mask[a + b * resolution] = covered ? EngineSetting.SUB_VOXEL_EMPTY_CELL : part;
            }
    }

    private void mergeMask(
            int face,
            int axis,
            int normal,
            int slice,
            float[] partUVBounds,
            ObjectArrayList<MeshInstance> meshes,
            FloatArrayList vertices,
            ShortArrayList indices) {

        for (int b = 0; b < resolution; b++)
            for (int a = 0; a < resolution;) {

                int value = mask[a + b * resolution];

                if (value == EngineSetting.SUB_VOXEL_EMPTY_CELL) {
                    a++;
                    continue;
                }

                int width = measureWidth(value, a, b);
                int height = measureHeight(value, a, b, width);

                for (int h = 0; h < height; h++)
                    for (int w = 0; w < width; w++)
                        mask[a + w + (b + h) * resolution] = EngineSetting.SUB_VOXEL_EMPTY_CELL;

                if (vertices.size() / EngineSetting.SUB_VOXEL_VERTEX_STRIDE + EngineSetting.QUAD_VERTEX_COUNT
                        > EngineSetting.MESH_VERT_LIMIT)
                    flush(meshes, vertices, indices);

                emitQuad(face, axis, normal, slice, a, b, width, height, value - 1, partUVBounds, vertices, indices);
                a += width;
            }
    }

    private int measureWidth(int value, int a, int b) {

        int width = 1;

        while (a + width < resolution && mask[a + width + b * resolution] == value)
            width++;

        return width;
    }

    private int measureHeight(int value, int a, int b, int width) {

        int height = 1;

        while (b + height < resolution) {

            for (int w = 0; w < width; w++)
                if (mask[a + w + (b + height) * resolution] != value)
                    return height;

            height++;
        }

        return height;
    }

    // Emit \\

    private void emitQuad(
            int face,
            int axis,
            int normal,
            int slice,
            int a,
            int b,
            int width,
            int height,
            int partIndex,
            float[] partUVBounds,
            FloatArrayList vertices,
            ShortArrayList indices) {

        int uAxis = (axis + 1) % EngineSetting.AXIS_COUNT;
        int vAxis = (axis + 2) % EngineSetting.AXIS_COUNT;
        float scale = 1f / resolution;

        minScratch[axis] = blockScratch[axis] + (normal > 0 ? slice + 1 : slice) * scale;
        maxScratch[axis] = minScratch[axis];
        minScratch[uAxis] = blockScratch[uAxis] + a * scale;
        maxScratch[uAxis] = blockScratch[uAxis] + (a + width) * scale;
        minScratch[vAxis] = blockScratch[vAxis] + b * scale;
        maxScratch[vAxis] = blockScratch[vAxis] + (b + height) * scale;

        subVoxelManager.emitFace(
                face,
                minScratch,
                maxScratch,
                blockScratch[EngineSetting.AXIS_X],
                blockScratch[EngineSetting.AXIS_Y],
                blockScratch[EngineSetting.AXIS_Z],
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

    // Utility \\

    private int resolveAxis(int face) {

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++)
            if (subVoxelManager.getFaceNormal(face, axis) != 0)
                return axis;

        return throwException("Sub-voxel face " + face + " has no normal axis.");
    }
}
