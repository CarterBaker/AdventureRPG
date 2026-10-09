package application.bootstrap.geometrypipeline.subvoxelmanager;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelGridStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelQuadListStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class SubVoxelMeshUtility extends EngineUtility {

    /*
     * The item vertex format for sub-voxel surfaces. A model's cubes and
     * walls are laid onto a sub-voxel grid and meshed by
     * SubVoxelGridMeshUtility like every other sub-voxel surface, so its faces
     * merge across block boundaries. A face carries only its part's texture
     * corner, and the item shaders repeat the texture once per block of the
     * model grid, so a merged face keeps every texel. A pocket's open box is
     * built here too, one quad per wall, and emitFace() is the one face writer
     * every item-format sub-voxel mesh goes through, a vehicle's included.
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

    // Model \\

    // Every cube and wall of the model laid onto the grid, its corner at the grid origin
    static void writeGrid(SubVoxelModelStruct model, SubVoxelGridStruct grid) {

        grid.clear();

        for (int z = 0; z < model.getSizeZ(); z++)
            for (int y = 0; y < model.getSizeY(); y++)
                for (int x = 0; x < model.getSizeX(); x++) {

                    int part = model.getCellPart(x, y, z);

                    if (part != EngineSetting.INDEX_NOT_FOUND)
                        grid.setPart(x, y, z, part);
                }

        for (int axis = 0; axis < EngineSetting.SUB_VOXEL_AXIS_COUNT; axis++)
            writeWalls(model, grid, axis);
    }

    private static void writeWalls(SubVoxelModelStruct model, SubVoxelGridStruct grid, int axis) {

        int[] position = new int[EngineSetting.AXIS_COUNT];
        int uAxis = (axis + 1) % EngineSetting.AXIS_COUNT;
        int vAxis = (axis + 2) % EngineSetting.AXIS_COUNT;

        for (int v = 0; v < model.getSize(vAxis); v++)
            for (int u = 0; u < model.getSize(uAxis); u++)
                for (int plane = 0; plane <= model.getSize(axis); plane++) {

                    position[axis] = plane;
                    position[uAxis] = u;
                    position[vAxis] = v;

                    int part = model.getWallPart(axis, position[0], position[1], position[2]);

                    if (part != EngineSetting.INDEX_NOT_FOUND)
                        grid.setWall(axis, position[0], position[1], position[2], part);
                }
    }

    // Quads \\

    // Every quad from first up to last written as item-format faces, each carrying its part's texture corner
    static void emitQuads(
            SubVoxelQuadListStruct quads,
            int first,
            int last,
            float[] partUVBounds,
            float[] minScratch,
            float[] maxScratch,
            FloatArrayList vertices,
            ShortArrayList indices) {

        for (int quad = first; quad < last; quad++) {

            int uvBase = quads.getPart(quad) * EngineSetting.SUB_VOXEL_UV_BOUNDS_FLOATS;

            resolveQuadBounds(quads, quad, minScratch, maxScratch);
            emitFace(
                    quads.getFace(quad), minScratch, maxScratch,
                    partUVBounds[uvBase], partUVBounds[uvBase + 1], vertices, indices);
        }
    }

    // Pocket \\

    // An open box of walls around a pocket's space — a floor and four sides facing in, one quad each
    static void buildPocket(
            int sizeX,
            int sizeY,
            int sizeZ,
            float[] uvBounds,
            FloatArrayList vertices,
            ShortArrayList indices) {

        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        int[] size = { sizeX, sizeY, sizeZ };
        float[] min = new float[EngineSetting.AXIS_COUNT];
        float[] max = new float[EngineSetting.AXIS_COUNT];

        for (int face = 0; face < EngineSetting.SUB_VOXEL_FACE_COUNT; face++) {

            int axis = resolveAxis(face);

            // The open top has no wall, and every other wall faces into the space
            if (axis == EngineSetting.AXIS_Y && FACE_NORMALS[face][axis] < 0)
                continue;

            for (int component = 0; component < EngineSetting.AXIS_COUNT; component++) {
                min[component] = 0f;
                max[component] = size[component] / resolution;
            }

            float plane = FACE_NORMALS[face][axis] > 0 ? 0f : size[axis] / resolution;

            min[axis] = plane;
            max[axis] = plane;

            emitFace(face, min, max, uvBounds[0], uvBounds[1], vertices, indices);
        }
    }

    // Emit \\

    // One face spanning min to max in block units, carrying its texture's corner
    static void emitFace(
            int face,
            float[] min,
            float[] max,
            float u0,
            float v0,
            FloatArrayList vertices,
            ShortArrayList indices) {

        int baseVertex = vertices.size() / EngineSetting.SUB_VOXEL_VERTEX_STRIDE;
        float[] corners = new float[EngineSetting.QUAD_VERTEX_COUNT * EngineSetting.AXIS_COUNT];

        resolveCorners(face, min, max, corners);

        for (int corner = 0; corner < EngineSetting.QUAD_VERTEX_COUNT; corner++)
            putVertex(
                    vertices,
                    corners[corner * EngineSetting.AXIS_COUNT],
                    corners[corner * EngineSetting.AXIS_COUNT + 1],
                    corners[corner * EngineSetting.AXIS_COUNT + 2],
                    face, u0, v0);

        indices.add((short) baseVertex);
        indices.add((short) (baseVertex + 1));
        indices.add((short) (baseVertex + 2));
        indices.add((short) (baseVertex + 2));
        indices.add((short) (baseVertex + 3));
        indices.add((short) baseVertex);
    }

    // The four corners of a face spanning min to max, three floats each, in the order every sub-voxel face is wound
    static void resolveCorners(int face, float[] min, float[] max, float[] corners) {

        float x0 = min[0], y0 = min[1], z0 = min[2];
        float x1 = max[0], y1 = max[1], z1 = max[2];

        switch (face) {
            case 0 -> writeCorners(corners, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
            case 1 -> writeCorners(corners, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
            case 2 -> writeCorners(corners, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
            case 3 -> writeCorners(corners, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
            case 4 -> writeCorners(corners, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
            default -> writeCorners(corners, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0);
        }
    }

    private static void writeCorners(
            float[] corners,
            float ax, float ay, float az,
            float bx, float by, float bz,
            float cx, float cy, float cz,
            float dx, float dy, float dz) {

        corners[0] = ax;
        corners[1] = ay;
        corners[2] = az;
        corners[3] = bx;
        corners[4] = by;
        corners[5] = bz;
        corners[6] = cx;
        corners[7] = cy;
        corners[8] = cz;
        corners[9] = dx;
        corners[10] = dy;
        corners[11] = dz;
    }

    // A quad's extent in blocks, its plane at both ends of its own axis
    static void resolveQuadBounds(SubVoxelQuadListStruct quads, int quad, float[] min, float[] max) {

        float scale = 1f / EngineSetting.SUB_VOXEL_RESOLUTION;
        int axis = resolveAxis(quads.getFace(quad));
        int uAxis = (axis + 1) % EngineSetting.AXIS_COUNT;
        int vAxis = (axis + 2) % EngineSetting.AXIS_COUNT;

        min[axis] = quads.getPlane(quad) * scale;
        max[axis] = min[axis];
        min[uAxis] = quads.getU(quad) * scale;
        max[uAxis] = (quads.getU(quad) + quads.getWidth(quad)) * scale;
        min[vAxis] = quads.getV(quad) * scale;
        max[vAxis] = (quads.getV(quad) + quads.getHeight(quad)) * scale;
    }

    private static void putVertex(
            FloatArrayList vertices,
            float x,
            float y,
            float z,
            int face,
            float u0,
            float v0) {

        vertices.add(x);
        vertices.add(y);
        vertices.add(z);
        vertices.add(face);
        vertices.add(u0);
        vertices.add(v0);
    }

    // Utility \\

    // One component of a face's outward normal, in the item shader's face order
    public static int getFaceNormal(int face, int axis) {
        return FACE_NORMALS[face][axis];
    }

    // The face whose normal runs along an axis, toward its positive or negative side
    static int resolveFace(int axis, boolean positive) {

        for (int face = 0; face < EngineSetting.SUB_VOXEL_FACE_COUNT; face++)
            if (FACE_NORMALS[face][axis] == (positive ? 1 : -1))
                return face;

        return throwException("No face runs along axis " + axis + ".");
    }

    public static int resolveAxis(int face) {

        int[] normal = FACE_NORMALS[face];

        if (normal[0] != 0)
            return 0;

        if (normal[1] != 0)
            return 1;

        return 2;
    }
}
