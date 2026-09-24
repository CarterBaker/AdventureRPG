package application.bootstrap.geometrypipeline.subvoxelmanager;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

class SubVoxelMeshUtility extends EngineUtility {

    /*
     * Greedy mesher for sub-voxel models. Emits only faces exposed to empty
     * cells, merges coplanar faces of the same part, and maps UVs from cell
     * position so merged faces keep their texels. Null outputs count quads only.
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

        return quadCount;
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

                mask[a + b * resolution] = exposed ? part + 1 : EngineSetting.SUB_VOXEL_EMPTY_CELL;
            }
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

        int baseVertex = vertices.size() / EngineSetting.SUB_VOXEL_VERTEX_STRIDE;
        int uvBase = partIndex * 4;
        float u0 = partUVBounds[uvBase];
        float v0 = partUVBounds[uvBase + 1];
        float u1 = partUVBounds[uvBase + 2];
        float v1 = partUVBounds[uvBase + 3];

        float x0 = min[0], y0 = min[1], z0 = min[2];
        float x1 = max[0], y1 = max[1], z1 = max[2];

        switch (face) {
            case 0 -> {
                putVertex(vertices, x0, y0, z1, face, x0, y0, u0, v0, u1, v1);
                putVertex(vertices, x1, y0, z1, face, x1, y0, u0, v0, u1, v1);
                putVertex(vertices, x1, y1, z1, face, x1, y1, u0, v0, u1, v1);
                putVertex(vertices, x0, y1, z1, face, x0, y1, u0, v0, u1, v1);
            }
            case 1 -> {
                putVertex(vertices, x1, y0, z1, face, 1f - z1, y0, u0, v0, u1, v1);
                putVertex(vertices, x1, y0, z0, face, 1f - z0, y0, u0, v0, u1, v1);
                putVertex(vertices, x1, y1, z0, face, 1f - z0, y1, u0, v0, u1, v1);
                putVertex(vertices, x1, y1, z1, face, 1f - z1, y1, u0, v0, u1, v1);
            }
            case 2 -> {
                putVertex(vertices, x1, y0, z0, face, 1f - x1, y0, u0, v0, u1, v1);
                putVertex(vertices, x0, y0, z0, face, 1f - x0, y0, u0, v0, u1, v1);
                putVertex(vertices, x0, y1, z0, face, 1f - x0, y1, u0, v0, u1, v1);
                putVertex(vertices, x1, y1, z0, face, 1f - x1, y1, u0, v0, u1, v1);
            }
            case 3 -> {
                putVertex(vertices, x0, y0, z0, face, z0, y0, u0, v0, u1, v1);
                putVertex(vertices, x0, y0, z1, face, z1, y0, u0, v0, u1, v1);
                putVertex(vertices, x0, y1, z1, face, z1, y1, u0, v0, u1, v1);
                putVertex(vertices, x0, y1, z0, face, z0, y1, u0, v0, u1, v1);
            }
            case 4 -> {
                putVertex(vertices, x0, y1, z0, face, x0, z0, u0, v0, u1, v1);
                putVertex(vertices, x1, y1, z0, face, x1, z0, u0, v0, u1, v1);
                putVertex(vertices, x1, y1, z1, face, x1, z1, u0, v0, u1, v1);
                putVertex(vertices, x0, y1, z1, face, x0, z1, u0, v0, u1, v1);
            }
            default -> {
                putVertex(vertices, x0, y0, z1, face, x0, 1f - z1, u0, v0, u1, v1);
                putVertex(vertices, x1, y0, z1, face, x1, 1f - z1, u0, v0, u1, v1);
                putVertex(vertices, x1, y0, z0, face, x1, 1f - z0, u0, v0, u1, v1);
                putVertex(vertices, x0, y0, z0, face, x0, 1f - z0, u0, v0, u1, v1);
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

    private static int resolveAxis(int face) {

        int[] normal = FACE_NORMALS[face];

        if (normal[0] != 0)
            return 0;

        if (normal[1] != 0)
            return 1;

        return 2;
    }
}
