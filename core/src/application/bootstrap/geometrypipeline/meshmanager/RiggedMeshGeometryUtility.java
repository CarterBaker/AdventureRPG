package application.bootstrap.geometrypipeline.meshmanager;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

class RiggedMeshGeometryUtility extends EngineUtility {

    /*
     * Expands a rigged mesh into the vertex data the skinned shader reads:
     * every quad corner its own vertex of position, UV snapped to its tile,
     * bone indices, bone weights and a smooth normal — the sum of the face
     * normals meeting at the corner's exact position within the smoothing
     * angle, so a low-poly body shades as one continuous surface. Authored
     * winding is free to vary, so each face is turned to the corner's own
     * side before it is added and only the line of the result is kept; the
     * shader turns it toward the face it shades. Texture tiles arrive
     * resolved, RIGGED_MESH_TILE_FLOATS per palette entry.
     */

    // Build \\

    static void build(
            RiggedMeshStruct mesh,
            float[] textureTiles,
            RiggedMeshScratchStruct scratch,
            FloatArrayList vertices,
            ShortArrayList indices) {

        int faceCount = mesh.getFaceCount();
        int cornerCount = faceCount * EngineSetting.QUAD_VERTEX_COUNT;

        vertices.clear();
        indices.clear();
        vertices.ensureCapacity(cornerCount * EngineSetting.RIGGED_MESH_VERTEX_STRIDE);
        indices.ensureCapacity(faceCount * EngineSetting.QUAD_INDEX_COUNT);

        resolveFaceNormals(mesh, scratch.faceNormals);
        resolvePositionGroups(mesh, scratch);

        for (int f = 0; f < faceCount; f++) {

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
                writeCorner(mesh, f, c, textureTiles, scratch, vertices);

            int base = f * EngineSetting.QUAD_VERTEX_COUNT;

            indices.add((short) base);
            indices.add((short) (base + 1));
            indices.add((short) (base + 2));
            indices.add((short) (base + 2));
            indices.add((short) (base + 3));
            indices.add((short) base);
        }
    }

    private static void writeCorner(
            RiggedMeshStruct mesh,
            int faceIndex,
            int corner,
            float[] textureTiles,
            RiggedMeshScratchStruct scratch,
            FloatArrayList vertices) {

        int vertexIndex = mesh.getFaceVertex(faceIndex, corner);
        int tile = mesh.getFaceTexture(faceIndex) * EngineSetting.RIGGED_MESH_TILE_FLOATS;
        int influenceCount = mesh.getInfluenceCount(vertexIndex);

        vertices.add(mesh.getX(vertexIndex));
        vertices.add(mesh.getY(vertexIndex));
        vertices.add(mesh.getZ(vertexIndex));
        vertices.add(MeshQuadUtility.snapUV(
                mesh.getFaceU(faceIndex, corner),
                textureTiles[tile],
                textureTiles[tile + 1],
                (int) textureTiles[tile + 4]));
        vertices.add(MeshQuadUtility.snapUV(
                mesh.getFaceV(faceIndex, corner),
                textureTiles[tile + 2],
                textureTiles[tile + 3],
                (int) textureTiles[tile + 5]));

        for (int i = 0; i < EngineSetting.MAX_BONE_INFLUENCES; i++)
            vertices.add(i < influenceCount ? mesh.getInfluenceBone(vertexIndex, i) : 0f);

        for (int i = 0; i < EngineSetting.MAX_BONE_INFLUENCES; i++)
            vertices.add(i < influenceCount ? mesh.getInfluenceWeight(vertexIndex, i) : 0f);

        writeSmoothNormal(faceIndex, scratch.vertexGroups.getInt(vertexIndex), scratch, vertices);
    }

    // Normals \\

    // Each face's diagonals crossed: its normal, as long as twice its area, so a larger face weighs more when smoothed
    private static void resolveFaceNormals(RiggedMeshStruct mesh, FloatArrayList faceNormals) {

        faceNormals.clear();

        for (int f = 0; f < mesh.getFaceCount(); f++) {

            int c0 = mesh.getFaceVertex(f, 0);
            int c1 = mesh.getFaceVertex(f, 1);
            int c2 = mesh.getFaceVertex(f, 2);
            int c3 = mesh.getFaceVertex(f, 3);

            float ax = mesh.getX(c2) - mesh.getX(c0);
            float ay = mesh.getY(c2) - mesh.getY(c0);
            float az = mesh.getZ(c2) - mesh.getZ(c0);
            float bx = mesh.getX(c3) - mesh.getX(c1);
            float by = mesh.getY(c3) - mesh.getY(c1);
            float bz = mesh.getZ(c3) - mesh.getZ(c1);

            faceNormals.add(ay * bz - az * by);
            faceNormals.add(az * bx - ax * bz);
            faceNormals.add(ax * by - ay * bx);
        }
    }

    // The corner's normal is the sum of the face normals at its exact position within the smoothing angle, each turned
    // to the corner's side; a corner with nothing to sum keeps its own face's normal
    private static void writeSmoothNormal(
            int faceIndex,
            int group,
            RiggedMeshScratchStruct scratch,
            FloatArrayList vertices) {

        float[] faceNormals = scratch.faceNormals.elements();
        int self = faceIndex * EngineSetting.SKINNED_NORMAL_FLOATS;
        int end = scratch.groupStarts.getInt(group + 1);
        float sumX = 0f;
        float sumY = 0f;
        float sumZ = 0f;

        for (int e = scratch.groupStarts.getInt(group); e < end; e++) {

            int other = scratch.groupFaces.getInt(e) * EngineSetting.SKINNED_NORMAL_FLOATS;
            float alignment = resolveAlignment(faceNormals, self, other);

            if (Math.abs(alignment) < EngineSetting.SKINNED_NORMAL_SMOOTH_COSINE)
                continue;

            float side = Math.signum(alignment);

            sumX += faceNormals[other] * side;
            sumY += faceNormals[other + 1] * side;
            sumZ += faceNormals[other + 2] * side;
        }

        float length = (float) Math.sqrt(sumX * sumX + sumY * sumY + sumZ * sumZ);

        if (length <= 0f) {
            vertices.add(faceNormals[self]);
            vertices.add(faceNormals[self + 1]);
            vertices.add(faceNormals[self + 2]);
            return;
        }

        vertices.add(sumX / length);
        vertices.add(sumY / length);
        vertices.add(sumZ / length);
    }

    // The cosine between two face normals, signed by whether they point the same way; 0 when either has no area
    private static float resolveAlignment(float[] faceNormals, int a, int b) {

        float ax = faceNormals[a];
        float ay = faceNormals[a + 1];
        float az = faceNormals[a + 2];
        float bx = faceNormals[b];
        float by = faceNormals[b + 1];
        float bz = faceNormals[b + 2];
        float lengths = (float) Math.sqrt((ax * ax + ay * ay + az * az) * (bx * bx + by * by + bz * bz));

        if (lengths <= 0f)
            return 0f;

        return (ax * bx + ay * by + az * bz) / lengths;
    }

    // Position Groups \\

    // Numbers every distinct vertex position, then lists the corners standing at each, group by group, in face order
    private static void resolvePositionGroups(RiggedMeshStruct mesh, RiggedMeshScratchStruct scratch) {

        int vertexCount = mesh.getVertexCount();
        int groupCount = 0;

        scratch.position2Group.clear();
        scratch.vertexGroups.clear();

        while (scratch.positionKeys.size() < vertexCount)
            scratch.positionKeys.add(new Vector3());

        for (int v = 0; v < vertexCount; v++) {

            Vector3 key = scratch.positionKeys.get(v).set(
                    mesh.getX(v) + 0f,
                    mesh.getY(v) + 0f,
                    mesh.getZ(v) + 0f);
            int group = scratch.position2Group.getInt(key);

            if (group == EngineSetting.INDEX_NOT_FOUND) {
                group = groupCount++;
                scratch.position2Group.put(key, group);
            }

            scratch.vertexGroups.add(group);
        }

        layOutGroupCorners(mesh, scratch, groupCount);
    }

    private static void layOutGroupCorners(RiggedMeshStruct mesh, RiggedMeshScratchStruct scratch, int groupCount) {

        IntArrayList starts = scratch.groupStarts;
        IntArrayList cursors = scratch.groupCursors;
        int faceCount = mesh.getFaceCount();

        starts.clear();
        starts.size(groupCount + 1);

        for (int f = 0; f < faceCount; f++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int group = scratch.vertexGroups.getInt(mesh.getFaceVertex(f, c));
                starts.set(group + 1, starts.getInt(group + 1) + 1);
            }

        for (int g = 0; g < groupCount; g++)
            starts.set(g + 1, starts.getInt(g + 1) + starts.getInt(g));

        cursors.clear();
        cursors.addElements(0, starts.elements(), 0, groupCount);
        scratch.groupFaces.size(faceCount * EngineSetting.QUAD_VERTEX_COUNT);

        for (int f = 0; f < faceCount; f++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int group = scratch.vertexGroups.getInt(mesh.getFaceVertex(f, c));
                int slot = cursors.getInt(group);

                scratch.groupFaces.set(slot, f);
                cursors.set(group, slot + 1);
            }
    }
}
