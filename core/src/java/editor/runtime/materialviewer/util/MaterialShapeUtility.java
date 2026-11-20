package editor.runtime.materialviewer.util;

import editor.runtime.materialviewer.MaterialViewerSetting;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class MaterialShapeUtility extends EngineUtility {

    /*
     * The shapes a material is shown on, as a material editor's preview
     * offers them: a sphere, a cube, a cylinder and a plane, each centred on
     * the origin within the shape radius, unwrapped so the material reads the
     * right way round from outside and its texels stay square, and wound
     * counter-clockwise seen from outside. Every vertex is laid out as a
     * painted item's — position, normal, UV and tile — the tile left at zero,
     * since the viewer names each map's texture itself.
     */

    // Build \\

    // The shape's vertices and triangles, in TOOL_SHAPE_NAMES order
    public static void build(int shape, FloatArrayList vertices, ShortArrayList indices) {

        vertices.clear();
        indices.clear();

        switch (shape) {
            case MaterialViewerSetting.SHAPE_CUBE -> buildCube(vertices, indices);
            case MaterialViewerSetting.SHAPE_CYLINDER -> buildCylinder(vertices, indices);
            case MaterialViewerSetting.SHAPE_PLANE -> buildPlane(vertices, indices);
            default -> buildSphere(vertices, indices);
        }
    }

    // Rings from the top pole down, each its segments round, the seam's column doubled so the UVs close
    private static void buildSphere(FloatArrayList vertices, ShortArrayList indices) {

        int segments = MaterialViewerSetting.SPHERE_SEGMENTS;
        int rings = MaterialViewerSetting.SPHERE_RINGS;
        float radius = MaterialViewerSetting.SHAPE_RADIUS;

        for (int r = 0; r <= rings; r++) {

            double theta = Math.PI * r / rings;

            for (int s = 0; s <= segments; s++) {

                double phi = 2.0 * Math.PI * s / segments;
                float nx = (float) (Math.sin(theta) * Math.cos(phi));
                float ny = (float) Math.cos(theta);
                float nz = (float) (-Math.sin(theta) * Math.sin(phi));

                addVertex(
                        vertices,
                        nx * radius,
                        ny * radius,
                        nz * radius,
                        nx,
                        ny,
                        nz,
                        MaterialViewerSetting.SPHERE_U_REPEAT * s / segments,
                        1f - (float) r / rings);
            }
        }

        addGrid(indices, 0, rings, segments);
    }

    // Six faces, each its own four corners laid across the face's u and v
    private static void buildCube(FloatArrayList vertices, ShortArrayList indices) {

        float half = MaterialViewerSetting.CUBE_HALF_EXTENT;

        for (int f = 0; f < MaterialViewerSetting.CUBE_FACES.length; f++) {

            float[] face = MaterialViewerSetting.CUBE_FACES[f];
            int base = toVertexCount(vertices);

            for (int c = 0; c < MaterialViewerSetting.QUAD_CORNERS.length; c++) {

                float cu = MaterialViewerSetting.QUAD_CORNERS[c][0];
                float cv = MaterialViewerSetting.QUAD_CORNERS[c][1];

                addVertex(
                        vertices,
                        (face[0] + face[3] * cu + face[6] * cv) * half,
                        (face[1] + face[4] * cu + face[7] * cv) * half,
                        (face[2] + face[5] * cu + face[8] * cv) * half,
                        face[0],
                        face[1],
                        face[2],
                        (cu + 1f) * MaterialViewerSetting.CAP_UV_MIDDLE,
                        (cv + 1f) * MaterialViewerSetting.CAP_UV_MIDDLE);
            }

            addQuad(indices, base, base + 1, base + 2, base + 3);
        }
    }

    // A side of two rings, top then bottom, and a fan for each cap
    private static void buildCylinder(FloatArrayList vertices, ShortArrayList indices) {

        int segments = MaterialViewerSetting.CYLINDER_SEGMENTS;
        float radius = MaterialViewerSetting.CYLINDER_RADIUS;
        float half = MaterialViewerSetting.CYLINDER_HALF_HEIGHT;

        for (int r = 0; r <= 1; r++)
            for (int s = 0; s <= segments; s++) {

                double phi = 2.0 * Math.PI * s / segments;
                float nx = (float) Math.cos(phi);
                float nz = (float) -Math.sin(phi);

                addVertex(
                        vertices,
                        nx * radius,
                        r == 0 ? half : -half,
                        nz * radius,
                        nx,
                        0f,
                        nz,
                        MaterialViewerSetting.CYLINDER_U_REPEAT * s / segments,
                        1f - r);
            }

        addGrid(indices, 0, 1, segments);
        addCap(vertices, indices, half, 1f);
        addCap(vertices, indices, -half, -1f);
    }

    // A cap's middle and its rim, the rim laid flat across the UVs, facing up or down
    private static void addCap(FloatArrayList vertices, ShortArrayList indices, float y, float facing) {

        int segments = MaterialViewerSetting.CYLINDER_SEGMENTS;
        float radius = MaterialViewerSetting.CYLINDER_RADIUS;
        float middle = MaterialViewerSetting.CAP_UV_MIDDLE;
        int center = toVertexCount(vertices);

        addVertex(vertices, 0f, y, 0f, 0f, facing, 0f, middle, middle);

        for (int s = 0; s <= segments; s++) {

            double phi = 2.0 * Math.PI * s / segments;
            float x = (float) Math.cos(phi);
            float z = (float) -Math.sin(phi);

            addVertex(
                    vertices,
                    x * radius,
                    y,
                    z * radius,
                    0f,
                    facing,
                    0f,
                    middle + middle * x,
                    middle - middle * z * facing);
        }

        for (int s = 0; s < segments; s++) {

            int rim = center + 1 + s;

            indices.add((short) center);
            indices.add((short) (facing > 0f ? rim : rim + 1));
            indices.add((short) (facing > 0f ? rim + 1 : rim));
        }
    }

    // One quad lying flat and facing up, its UVs across it once
    private static void buildPlane(FloatArrayList vertices, ShortArrayList indices) {

        float half = MaterialViewerSetting.PLANE_HALF_EXTENT;

        for (int c = 0; c < MaterialViewerSetting.QUAD_CORNERS.length; c++) {

            float cu = MaterialViewerSetting.QUAD_CORNERS[c][0];
            float cv = MaterialViewerSetting.QUAD_CORNERS[c][1];

            addVertex(
                    vertices,
                    cu * half,
                    0f,
                    -cv * half,
                    0f,
                    1f,
                    0f,
                    (cu + 1f) * MaterialViewerSetting.CAP_UV_MIDDLE,
                    (cv + 1f) * MaterialViewerSetting.CAP_UV_MIDDLE);
        }

        addQuad(indices, 0, 1, 2, 3);
    }

    // Utility \\

    // A grid of rows by columns of quads over vertices laid row by row, each row one wider than its columns
    private static void addGrid(ShortArrayList indices, int base, int rows, int columns) {

        int stride = columns + 1;

        for (int r = 0; r < rows; r++)
            for (int s = 0; s < columns; s++) {

                int top = base + r * stride + s;
                int bottom = top + stride;

                addQuad(indices, top, bottom, bottom + 1, top + 1);
            }
    }

    // A quad's two triangles, its corners counter-clockwise seen from outside
    private static void addQuad(ShortArrayList indices, int a, int b, int c, int d) {
        indices.add((short) a);
        indices.add((short) b);
        indices.add((short) c);
        indices.add((short) c);
        indices.add((short) d);
        indices.add((short) a);
    }

    private static void addVertex(
            FloatArrayList vertices,
            float x,
            float y,
            float z,
            float nx,
            float ny,
            float nz,
            float u,
            float v) {
        vertices.add(x);
        vertices.add(y);
        vertices.add(z);
        vertices.add(nx);
        vertices.add(ny);
        vertices.add(nz);
        vertices.add(u);
        vertices.add(v);
        vertices.add(0f);
    }

    private static int toVertexCount(FloatArrayList vertices) {
        return vertices.size() / EngineSetting.PAINTED_ITEM_VERTEX_STRIDE;
    }
}
