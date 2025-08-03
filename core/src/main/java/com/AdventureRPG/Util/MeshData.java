package com.AdventureRPG.Util;

import java.util.ArrayList;
import java.util.List;

public class MeshData {

    public final List<Float> vertices = new ArrayList<>();
    public final List<Float> uvs = new ArrayList<>();
    public final List<Integer> indices = new ArrayList<>();

    private int vertexCount = 0;

    public void clear() {
        vertices.clear();
        uvs.clear();
        indices.clear();
        vertexCount = 0;
    }

    public void addFace(int x, int y, int z, Direction dir, float[] uv) {
        float[][] faceVertices = getFaceVertices(x, y, z, dir);

        for (int i = 0; i < 4; i++) {
            float[] vert = faceVertices[i];
            vertices.add(vert[0]);
            vertices.add(vert[1]);
            vertices.add(vert[2]);

            uvs.add(uv[i * 2]);
            uvs.add(uv[i * 2 + 1]);
        }

        // Two triangles (0, 1, 2) and (2, 3, 0)
        indices.add(vertexCount);
        indices.add(vertexCount + 1);
        indices.add(vertexCount + 2);
        indices.add(vertexCount + 2);
        indices.add(vertexCount + 3);
        indices.add(vertexCount);
        vertexCount += 4;
    }

    private float[][] getFaceVertices(int x, int y, int z, Direction dir) {
        switch (dir) {
            case UP:
                return new float[][] {
                        { x, y + 1, z },
                        { x + 1, y + 1, z },
                        { x + 1, y + 1, z + 1 },
                        { x, y + 1, z + 1 }
                };
            case DOWN:
                return new float[][] {
                        { x, y, z },
                        { x + 1, y, z },
                        { x + 1, y, z + 1 },
                        { x, y, z + 1 }
                };
            case FRONT:
                return new float[][] {
                        { x + 1, y, z + 1 },
                        { x + 1, y + 1, z + 1 },
                        { x, y + 1, z + 1 },
                        { x, y, z + 1 }
                };
            case BACK:
                return new float[][] {
                        { x, y, z },
                        { x, y + 1, z },
                        { x + 1, y + 1, z },
                        { x + 1, y, z }
                };
            case RIGHT:
                return new float[][] {
                        { x + 1, y, z },
                        { x + 1, y + 1, z },
                        { x + 1, y + 1, z + 1 },
                        { x + 1, y, z + 1 }
                };
            case LEFT:
                return new float[][] {
                        { x, y, z + 1 },
                        { x, y + 1, z + 1 },
                        { x, y + 1, z },
                        { x, y, z }
                };
            default:
                throw new IllegalArgumentException("Unknown face direction: " + dir);
        }
    }
}
