package application.bootstrap.geometrypipeline.subvoxelmanager;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

class SubVoxelMeshUtility extends EngineUtility {

    /*
     * Greedy mesher for sub-voxel models. Emits only cube faces exposed to
     * empty cells, merges coplanar faces of the same part, and maps UVs from
     * cell position so merged faces keep their texels. A wall is one quad seen
     * from both sides, since items draw without culling; it faces its open
     * side, hides where cubes bury it on both sides, and takes the place of a
     * cube face it covers. Null outputs count quads only. A pocket's open
     * box is built here too, one quad per block of each wall so every quad's
     * texels stay inside one block, and emitFace() is the one face writer
     * every sub-voxel mesh goes through, a vehicle's included.
     */

    // Faces — index order matches the item shader's normal table
    private static final int[][] FACE_NORMALS = {
            { 0, 0, 1 },
            { 1, 0, 0 },
            { 0, 0, -1 },
            { -1, 0, 0 },
            { 0, 1, 0 },
            { 0, -1, 0 }
    };

    // Build \\

    static int build(
            SubVoxelModelStruct model,
            float[] partUVBounds,
            FloatArrayList vertices,
            ShortArrayList indices) {

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        int[] mask = new int[resolution * resolution];
        int quadCount = 0;

        for (int face = 0; face < EngineSetting.SUB_VOXEL_FACE_COUNT; face++)
            for (int slice = 0; slice < resolution; slice++) {
                fillMask(model, face, slice, mask);
                quadCount += mergeMask(face, slice, mask, partUVBounds, vertices, indices);
            }

        for (int face = 0; face < EngineSetting.SUB_VOXEL_FACE_COUNT; face++)
            for (int plane = 0; plane <= resolution; plane++) {

                // A positive face sits on the far side of the slice before the plane
                int slice = FACE_NORMALS[face][resolveAxis(face)] > 0 ? plane - 1 : plane;

                fillWallMask(model, face, plane, mask);
                quadCount += mergeMask(face, slice, mask, partUVBounds, vertices, indices);
            }

        return quadCount;
    }

    // Pocket \\

    // An open box of walls around a pocket's space — a floor and four sides facing in, split per block
    static void buildPocket(
            int sizeX,
            int sizeY,
            int sizeZ,
            float[] uvBounds,
            FloatArrayList vertices,
            ShortArrayList indices) {

        int[] size = { sizeX, sizeY, sizeZ };

        for (int face = 0; face < EngineSetting.SUB_VOXEL_FACE_COUNT; face++) {

            int axis = resolveAxis(face);

            // The open top has no wall, and every other wall faces into the space
            if (axis == 1 && FACE_NORMALS[face][axis] < 0)
                continue;

            int plane = FACE_NORMALS[face][axis] > 0 ? 0 : size[axis];

            emitPocketWall(face, axis, plane, size, uvBounds, vertices, indices);
        }
    }

    private static void emitPocketWall(
            int face,
            int axis,
            int plane,
            int[] size,
            float[] uvBounds,
            FloatArrayList vertices,
            ShortArrayList indices) {

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        int uAxis = (axis + 1) % 3;
        int vAxis = (axis + 2) % 3;
        float[] min = new float[3];
        float[] max = new float[3];
        float[] origin = new float[3];

        min[axis] = (float) plane / resolution;
        max[axis] = min[axis];
        origin[axis] = (float) Math.floor(min[axis]);

        for (int b = 0; b < size[vAxis]; b += resolution)
            for (int a = 0; a < size[uAxis]; a += resolution) {

                min[uAxis] = (float) a / resolution;
                max[uAxis] = (float) Math.min(a + resolution, size[uAxis]) / resolution;
                min[vAxis] = (float) b / resolution;
                max[vAxis] = (float) Math.min(b + resolution, size[vAxis]) / resolution;
                origin[uAxis] = min[uAxis];
                origin[vAxis] = min[vAxis];

                emitFace(
                        face, min, max, origin[0], origin[1], origin[2],
                        uvBounds[0], uvBounds[1], uvBounds[2], uvBounds[3],
                        vertices, indices);
            }
    }

    // Mask \\

    private static void fillMask(SubVoxelModelStruct model, int face, int slice, int[] mask) {

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        int[] normal = FACE_NORMALS[face];
        int axis = resolveAxis(face);
        int uAxis = (axis + 1) % 3;
        int vAxis = (axis + 2) % 3;
        int[] cell = new int[3];

        for (int b = 0; b < resolution; b++)
            for (int a = 0; a < resolution; a++) {

                cell[axis] = slice;
                cell[uAxis] = a;
                cell[vAxis] = b;

                int part = model.getCellPart(cell[0], cell[1], cell[2]);
                boolean exposed = part != EngineSetting.INDEX_NOT_FOUND
                        && !model.isFilled(cell[0] + normal[0], cell[1] + normal[1], cell[2] + normal[2]);

                if (exposed) {
                    cell[axis] = normal[axis] > 0 ? slice + 1 : slice;
                    exposed = !model.hasWall(axis, cell[0], cell[1], cell[2]);
                }

                mask[a + b * resolution] = exposed ? part + 1 : EngineSetting.SUB_VOXEL_EMPTY_CELL;
            }
    }

    // Walls on one plane that face this face's way: toward their open side, or positive when both sides are open
    private static void fillWallMask(SubVoxelModelStruct model, int face, int plane, int[] mask) {

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        int axis = resolveAxis(face);
        boolean positive = FACE_NORMALS[face][axis] > 0;
        int uAxis = (axis + 1) % 3;
        int vAxis = (axis + 2) % 3;
        int[] cell = new int[3];

        for (int b = 0; b < resolution; b++)
            for (int a = 0; a < resolution; a++) {

                cell[axis] = plane;
                cell[uAxis] = a;
                cell[vAxis] = b;

                int part = model.getWallPart(axis, cell[0], cell[1], cell[2]);
                boolean frontOpen = !isFilledOnSide(model, cell, axis, true);
                boolean backOpen = !isFilledOnSide(model, cell, axis, false);
                boolean shown = part != EngineSetting.INDEX_NOT_FOUND
                        && (positive ? frontOpen : backOpen && !frontOpen);

                mask[a + b * resolution] = shown ? part + 1 : EngineSetting.SUB_VOXEL_EMPTY_CELL;
            }
    }

    private static boolean isFilledOnSide(SubVoxelModelStruct model, int[] wall, int axis, boolean positive) {

        int offset = positive ? 0 : -1;

        return model.isFilled(
                wall[0] + (axis == 0 ? offset : 0),
                wall[1] + (axis == 1 ? offset : 0),
                wall[2] + (axis == 2 ? offset : 0));
    }

    private static int mergeMask(
            int face,
            int slice,
            int[] mask,
            float[] partUVBounds,
            FloatArrayList vertices,
            ShortArrayList indices) {

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        int quadCount = 0;

        for (int b = 0; b < resolution; b++)
            for (int a = 0; a < resolution;) {

                int value = mask[a + b * resolution];

                if (value == EngineSetting.SUB_VOXEL_EMPTY_CELL) {
                    a++;
                    continue;
                }

                int width = measureWidth(mask, value, a, b);
                int height = measureHeight(mask, value, a, b, width);

                for (int h = 0; h < height; h++)
                    for (int w = 0; w < width; w++)
                        mask[a + w + (b + h) * resolution] = EngineSetting.SUB_VOXEL_EMPTY_CELL;

                if (vertices != null)
                    emitQuad(face, slice, a, b, width, height, value - 1, partUVBounds, vertices, indices);

                quadCount++;
                a += width;
            }

        return quadCount;
    }

    private static int measureWidth(int[] mask, int value, int a, int b) {

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        int width = 1;

        while (a + width < resolution && mask[a + width + b * resolution] == value)
            width++;

        return width;
    }

    private static int measureHeight(int[] mask, int value, int a, int b, int width) {

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
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

    private static void emitQuad(
            int face,
            int slice,
            int a,
            int b,
            int width,
            int height,
            int partIndex,
            float[] partUVBounds,
            FloatArrayList vertices,
            ShortArrayList indices) {

        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        int axis = resolveAxis(face);
        int uAxis = (axis + 1) % 3;
        int vAxis = (axis + 2) % 3;
        float plane = (FACE_NORMALS[face][axis] > 0 ? slice + 1 : slice) / resolution;

        float[] min = new float[3];
        float[] max = new float[3];

        min[axis] = plane;
        max[axis] = plane;
        min[uAxis] = a / resolution;
        max[uAxis] = (a + width) / resolution;
        min[vAxis] = b / resolution;
        max[vAxis] = (b + height) / resolution;

        int uvBase = partIndex * 4;

        emitFace(
                face, min, max, 0f, 0f, 0f,
                partUVBounds[uvBase], partUVBounds[uvBase + 1], partUVBounds[uvBase + 2], partUVBounds[uvBase + 3],
                vertices, indices);
    }

    // One face spanning min to max in block units; UVs run from the block corner at the origin
    static void emitFace(
            int face,
            float[] min,
            float[] max,
            float originX,
            float originY,
            float originZ,
            float u0,
            float v0,
            float u1,
            float v1,
            FloatArrayList vertices,
            ShortArrayList indices) {

        int baseVertex = vertices.size() / EngineSetting.SUB_VOXEL_VERTEX_STRIDE;

        float x0 = min[0], y0 = min[1], z0 = min[2];
        float x1 = max[0], y1 = max[1], z1 = max[2];
        float lx0 = x0 - originX, ly0 = y0 - originY, lz0 = z0 - originZ;
        float lx1 = x1 - originX, ly1 = y1 - originY, lz1 = z1 - originZ;

        switch (face) {
            case 0 -> {
                putVertex(vertices, x0, y0, z1, face, lx0, ly0, u0, v0, u1, v1);
                putVertex(vertices, x1, y0, z1, face, lx1, ly0, u0, v0, u1, v1);
                putVertex(vertices, x1, y1, z1, face, lx1, ly1, u0, v0, u1, v1);
                putVertex(vertices, x0, y1, z1, face, lx0, ly1, u0, v0, u1, v1);
            }
            case 1 -> {
                putVertex(vertices, x1, y0, z1, face, 1f - lz1, ly0, u0, v0, u1, v1);
                putVertex(vertices, x1, y0, z0, face, 1f - lz0, ly0, u0, v0, u1, v1);
                putVertex(vertices, x1, y1, z0, face, 1f - lz0, ly1, u0, v0, u1, v1);
                putVertex(vertices, x1, y1, z1, face, 1f - lz1, ly1, u0, v0, u1, v1);
            }
            case 2 -> {
                putVertex(vertices, x1, y0, z0, face, 1f - lx1, ly0, u0, v0, u1, v1);
                putVertex(vertices, x0, y0, z0, face, 1f - lx0, ly0, u0, v0, u1, v1);
                putVertex(vertices, x0, y1, z0, face, 1f - lx0, ly1, u0, v0, u1, v1);
                putVertex(vertices, x1, y1, z0, face, 1f - lx1, ly1, u0, v0, u1, v1);
            }
            case 3 -> {
                putVertex(vertices, x0, y0, z0, face, lz0, ly0, u0, v0, u1, v1);
                putVertex(vertices, x0, y0, z1, face, lz1, ly0, u0, v0, u1, v1);
                putVertex(vertices, x0, y1, z1, face, lz1, ly1, u0, v0, u1, v1);
                putVertex(vertices, x0, y1, z0, face, lz0, ly1, u0, v0, u1, v1);
            }
            case 4 -> {
                putVertex(vertices, x0, y1, z0, face, lx0, lz0, u0, v0, u1, v1);
                putVertex(vertices, x1, y1, z0, face, lx1, lz0, u0, v0, u1, v1);
                putVertex(vertices, x1, y1, z1, face, lx1, lz1, u0, v0, u1, v1);
                putVertex(vertices, x0, y1, z1, face, lx0, lz1, u0, v0, u1, v1);
            }
            default -> {
                putVertex(vertices, x0, y0, z1, face, lx0, 1f - lz1, u0, v0, u1, v1);
                putVertex(vertices, x1, y0, z1, face, lx1, 1f - lz1, u0, v0, u1, v1);
                putVertex(vertices, x1, y0, z0, face, lx1, 1f - lz0, u0, v0, u1, v1);
                putVertex(vertices, x0, y0, z0, face, lx0, 1f - lz0, u0, v0, u1, v1);
            }
        }

        indices.add((short) baseVertex);
        indices.add((short) (baseVertex + 1));
        indices.add((short) (baseVertex + 2));
        indices.add((short) (baseVertex + 2));
        indices.add((short) (baseVertex + 3));
        indices.add((short) baseVertex);
    }

    private static void putVertex(
            FloatArrayList vertices,
            float x,
            float y,
            float z,
            int face,
            float localU,
            float localV,
            float u0,
            float v0,
            float u1,
            float v1) {

        vertices.add(x);
        vertices.add(y);
        vertices.add(z);
        vertices.add(face);
        vertices.add(u0 + localU * (u1 - u0));
        vertices.add(v0 + localV * (v1 - v0));
    }

    // Utility \\

    // One component of a face's outward normal, in the item shader's face order
    static int getFaceNormal(int face, int axis) {
        return FACE_NORMALS[face][axis];
    }

    private static int resolveAxis(int face) {

        int[] normal = FACE_NORMALS[face];

        if (normal[0] != 0)
            return 0;

        if (normal[1] != 0)
            return 1;

        return 2;
    }
}
