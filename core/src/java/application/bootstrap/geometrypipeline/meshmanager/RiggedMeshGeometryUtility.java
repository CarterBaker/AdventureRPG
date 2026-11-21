package application.bootstrap.geometrypipeline.meshmanager;

import java.util.Arrays;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class RiggedMeshGeometryUtility extends EngineUtility {

    /*
     * Builds a rigged mesh into the shared, packed vertex data the skinned
     * shader reads. Every quad corner is packed into RIGGED_MESH_VERTEX_INTS
     * words: its position as floats, its UV snapped to its tile as normalised
     * shorts, bone indices as bytes, bone weights as normalised bytes summing
     * to one, a smooth normal in one 10-bit-per-axis word, where its vertex's
     * morph entries start and how many there are, its face's zone with the
     * tile its texture is drawn from, and its vertex's mask channels as
     * normalised bytes. Corners whose words all match become one indexed
     * vertex, so a vertex is stored once however many quads meet at it. The
     * normal is the sum of the face normals meeting at the corner's exact
     * position within the smoothing angle, so a low-poly body shades as one
     * continuous surface (a mesh file that stores its corners' normals hands
     * them in and none are computed); authored winding is free to vary, so each face is
     * turned to the corner's own side before it is added and only the line of
     * the result is kept, which the shader turns toward the face it shades.
     * The morph entries — a delta and the morph it belongs to, grouped vertex
     * by vertex — fill the entry list the mesh's morph texture is made from.
     * Texture tiles arrive resolved, RIGGED_MESH_TILE_INTS per palette entry:
     * the tile ID and its size.
     */

    // Build \\

    static void build(
            RiggedMeshStruct mesh,
            int[] textureTiles,
            RiggedMeshScratchStruct scratch,
            int[] storedNormals,
            IntArrayList vertices,
            IntArrayList indices,
            FloatArrayList morphEntries) {

        int faceCount = mesh.getFaceCount();
        int cornerCount = faceCount * EngineSetting.QUAD_VERTEX_COUNT;

        vertices.clear();
        indices.clear();
        vertices.ensureCapacity(cornerCount * EngineSetting.RIGGED_MESH_VERTEX_INTS);
        indices.ensureCapacity(faceCount * EngineSetting.QUAD_INDEX_COUNT);

        if (storedNormals == null) {
            resolveFaceNormals(mesh, scratch.faceNormals);
            resolvePositionGroups(mesh, scratch);
        }

        resolveMorphEntries(mesh, scratch.vertexMorphStarts, morphEntries);
        clearVertexTable(scratch, cornerCount);

        int[] quad = scratch.quadVertices;

        for (int f = 0; f < faceCount; f++) {

            int corners = mesh.getFaceCornerCount(f);

            for (int c = 0; c < corners; c++) {
                packCorner(mesh, f, c, textureTiles, storedNormals, scratch);
                quad[c] = shareVertex(scratch, vertices);
            }

            indices.add(quad[0]);
            indices.add(quad[1]);
            indices.add(quad[2]);

            if (corners == EngineSetting.TRIANGLE_VERTEX_COUNT)
                continue;

            indices.add(quad[2]);
            indices.add(quad[3]);
            indices.add(quad[0]);
        }
    }

    // Every corner's packed smooth normal, face by face, as a mesh file stores them
    static void packNormals(RiggedMeshStruct mesh, RiggedMeshScratchStruct scratch, int[] normals) {

        resolveFaceNormals(mesh, scratch.faceNormals);
        resolvePositionGroups(mesh, scratch);

        for (int f = 0; f < mesh.getFaceCount(); f++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
                normals[f * EngineSetting.QUAD_VERTEX_COUNT + c] = packSmoothNormal(
                        f, scratch.vertexGroups.getInt(mesh.getFaceVertex(f, c)), scratch);
    }

    // Packing \\

    private static void packCorner(
            RiggedMeshStruct mesh,
            int faceIndex,
            int corner,
            int[] textureTiles,
            int[] storedNormals,
            RiggedMeshScratchStruct scratch) {

        int[] packed = scratch.corner;
        int vertexIndex = mesh.getFaceVertex(faceIndex, corner);
        int tile = mesh.getFaceTexture(faceIndex) * EngineSetting.RIGGED_MESH_TILE_INTS;
        int morphStart = scratch.vertexMorphStarts.getInt(vertexIndex);
        int u = packUnit(
                MeshQuadUtility.snapUV(mesh.getFaceU(faceIndex, corner), textureTiles[tile + 1]),
                EngineSetting.RIGGED_MESH_UV_SCALE);
        int v = packUnit(
                MeshQuadUtility.snapUV(mesh.getFaceV(faceIndex, corner), textureTiles[tile + 1]),
                EngineSetting.RIGGED_MESH_UV_SCALE);
        int masks = 0;

        for (int i = 0; i < EngineSetting.RIGGED_MESH_MASK_FLOATS; i++)
            masks |= packUnit(mesh.getMask(vertexIndex, i), EngineSetting.RIGGED_MESH_BYTE_SCALE) << (i * Byte.SIZE);

        packed[0] = Float.floatToRawIntBits(mesh.getX(vertexIndex) + 0f);
        packed[1] = Float.floatToRawIntBits(mesh.getY(vertexIndex) + 0f);
        packed[2] = Float.floatToRawIntBits(mesh.getZ(vertexIndex) + 0f);
        packed[3] = u | v << Short.SIZE;
        packed[4] = packBones(mesh, vertexIndex);
        packed[5] = packWeights(mesh, vertexIndex);
        packed[6] = storedNormals != null
                ? storedNormals[faceIndex * EngineSetting.QUAD_VERTEX_COUNT + corner]
                : packSmoothNormal(faceIndex, scratch.vertexGroups.getInt(vertexIndex), scratch);
        packed[7] = morphStart;
        packed[8] = scratch.vertexMorphStarts.getInt(vertexIndex + 1) - morphStart;
        packed[9] = mesh.getFaceZone(faceIndex).ordinal() | textureTiles[tile] << Short.SIZE;
        packed[10] = masks;
    }

    // A value in 0..1 as a whole number of steps of the scale
    private static int packUnit(float value, int scale) {
        return Math.round(Math.max(0f, Math.min(1f, value)) * scale);
    }

    private static int packBones(RiggedMeshStruct mesh, int vertexIndex) {

        int influenceCount = mesh.getInfluenceCount(vertexIndex);
        int bones = 0;

        for (int i = 0; i < influenceCount; i++)
            bones |= mesh.getInfluenceBone(vertexIndex, i) << (i * Byte.SIZE);

        return bones;
    }

    // Each weight as a byte; whatever rounding leaves over or short goes to the heaviest, so they still sum to one
    private static int packWeights(RiggedMeshStruct mesh, int vertexIndex) {

        int influenceCount = mesh.getInfluenceCount(vertexIndex);
        int weights = 0;
        int sum = 0;
        int heaviest = 0;
        int heaviestWeight = -1;

        for (int i = 0; i < influenceCount; i++) {

            int weight = packUnit(mesh.getInfluenceWeight(vertexIndex, i), EngineSetting.RIGGED_MESH_BYTE_SCALE);

            if (weight > heaviestWeight) {
                heaviestWeight = weight;
                heaviest = i;
            }

            weights |= weight << (i * Byte.SIZE);
            sum += weight;
        }

        if (influenceCount == 0 || sum == 0)
            return weights;

        int corrected = Math.max(0, Math.min(
                EngineSetting.RIGGED_MESH_BYTE_SCALE,
                heaviestWeight + EngineSetting.RIGGED_MESH_BYTE_SCALE - sum));

        return weights & ~(EngineSetting.RIGGED_MESH_BYTE_SCALE << (heaviest * Byte.SIZE))
                | corrected << (heaviest * Byte.SIZE);
    }

    // Sharing \\

    // Empties the open-addressed table of vertices, grown to at least twice the corners it may be asked to hold
    private static void clearVertexTable(RiggedMeshScratchStruct scratch, int cornerCount) {

        int capacity = Integer.highestOneBit(Math.max(1, cornerCount) * 2 - 1) * 2;

        if (scratch.vertexTable.length < capacity)
            scratch.vertexTable = new int[capacity];

        Arrays.fill(scratch.vertexTable, EngineSetting.INDEX_NOT_FOUND);
    }

    // The index of the vertex whose words all match the packed corner, appending it when there is none yet
    private static int shareVertex(RiggedMeshScratchStruct scratch, IntArrayList vertices) {

        int[] packed = scratch.corner;
        int[] table = scratch.vertexTable;
        int mask = table.length - 1;
        int hash = 0;

        for (int i = 0; i < EngineSetting.RIGGED_MESH_VERTEX_INTS; i++)
            hash = hash * 31 + packed[i];

        int slot = (hash ^ hash >>> 16) & mask;

        while (table[slot] != EngineSetting.INDEX_NOT_FOUND) {

            if (matchesVertex(vertices.elements(), table[slot] * EngineSetting.RIGGED_MESH_VERTEX_INTS, packed))
                return table[slot];

            slot = (slot + 1) & mask;
        }

        int index = vertices.size() / EngineSetting.RIGGED_MESH_VERTEX_INTS;

        table[slot] = index;
        vertices.addElements(vertices.size(), packed, 0, EngineSetting.RIGGED_MESH_VERTEX_INTS);

        return index;
    }

    private static boolean matchesVertex(int[] vertices, int base, int[] packed) {

        for (int i = 0; i < EngineSetting.RIGGED_MESH_VERTEX_INTS; i++)
            if (vertices[base + i] != packed[i])
                return false;

        return true;
    }

    // Morphs \\

    // Every morph delta that moves its vertex, grouped vertex by vertex as (dx, dy, dz, morph); starts receives each
    // vertex's first entry, with one more slot closing the last vertex's run
    private static void resolveMorphEntries(RiggedMeshStruct mesh, IntArrayList starts, FloatArrayList morphEntries) {

        int vertexCount = mesh.getVertexCount();
        int morphCount = mesh.getMorphCount();

        starts.clear();
        starts.size(vertexCount + 1);

        for (int m = 0; m < morphCount; m++)
            for (int e = 0; e < mesh.getMorphEntryCount(m); e++)
                if (movesVertex(mesh, m, e)) {
                    int vertexIndex = mesh.getMorphVertex(m, e);
                    starts.set(vertexIndex + 1, starts.getInt(vertexIndex + 1) + 1);
                }

        for (int v = 0; v < vertexCount; v++)
            starts.set(v + 1, starts.getInt(v + 1) + starts.getInt(v));

        morphEntries.clear();
        morphEntries.size(starts.getInt(vertexCount) * EngineSetting.RIGGED_MESH_MORPH_ENTRY_FLOATS);

        float[] entries = morphEntries.elements();

        for (int m = 0; m < morphCount; m++)
            for (int e = 0; e < mesh.getMorphEntryCount(m); e++) {

                if (!movesVertex(mesh, m, e))
                    continue;

                int vertexIndex = mesh.getMorphVertex(m, e);
                int slot = starts.getInt(vertexIndex);
                int base = slot * EngineSetting.RIGGED_MESH_MORPH_ENTRY_FLOATS;

                entries[base] = mesh.getMorphDelta(m, e, 0);
                entries[base + 1] = mesh.getMorphDelta(m, e, 1);
                entries[base + 2] = mesh.getMorphDelta(m, e, 2);
                entries[base + 3] = m;
                starts.set(vertexIndex, slot + 1);
            }

        for (int v = vertexCount; v > 0; v--)
            starts.set(v, starts.getInt(v - 1));

        starts.set(0, 0);
    }

    private static boolean movesVertex(RiggedMeshStruct mesh, int morphIndex, int entry) {
        return Math.abs(mesh.getMorphDelta(morphIndex, entry, 0)) > EngineSetting.RIGGED_MESH_MORPH_DELTA_EPSILON
                || Math.abs(mesh.getMorphDelta(morphIndex, entry, 1)) > EngineSetting.RIGGED_MESH_MORPH_DELTA_EPSILON
                || Math.abs(mesh.getMorphDelta(morphIndex, entry, 2)) > EngineSetting.RIGGED_MESH_MORPH_DELTA_EPSILON;
    }

    // Normals \\

    // Each face's diagonals crossed: its normal, as long as twice its area, so a larger face weighs more when smoothed;
    // a triangle's fourth slot repeats its third, so its diagonals cross as its edges do
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
    // to the corner's side; a corner with nothing to sum keeps its own face's normal. Packed 10 bits to the axis
    private static int packSmoothNormal(int faceIndex, int group, RiggedMeshScratchStruct scratch) {

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

        if (sumX == 0f && sumY == 0f && sumZ == 0f) {
            sumX = faceNormals[self];
            sumY = faceNormals[self + 1];
            sumZ = faceNormals[self + 2];
        }

        float length = (float) Math.sqrt(sumX * sumX + sumY * sumY + sumZ * sumZ);

        if (length <= 0f)
            return 0;

        return packAxis(sumX / length)
                | packAxis(sumY / length) << EngineSetting.RIGGED_MESH_NORMAL_BITS
                | packAxis(sumZ / length) << (EngineSetting.RIGGED_MESH_NORMAL_BITS * 2);
    }

    // A value in -1..1 as a signed whole number of steps, in the bits one axis has
    private static int packAxis(float value) {
        return Math.round(value * EngineSetting.RIGGED_MESH_NORMAL_SCALE) & EngineSetting.RIGGED_MESH_NORMAL_MASK;
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

        int cornerTotal = 0;

        for (int f = 0; f < faceCount; f++)
            for (int c = 0; c < mesh.getFaceCornerCount(f); c++) {

                int group = scratch.vertexGroups.getInt(mesh.getFaceVertex(f, c));
                starts.set(group + 1, starts.getInt(group + 1) + 1);
                cornerTotal++;
            }

        for (int g = 0; g < groupCount; g++)
            starts.set(g + 1, starts.getInt(g + 1) + starts.getInt(g));

        cursors.clear();
        cursors.addElements(0, starts.elements(), 0, groupCount);
        scratch.groupFaces.size(cornerTotal);

        for (int f = 0; f < faceCount; f++)
            for (int c = 0; c < mesh.getFaceCornerCount(f); c++) {

                int group = scratch.vertexGroups.getInt(mesh.getFaceVertex(f, c));
                int slot = cursors.getInt(group);

                scratch.groupFaces.set(slot, f);
                cursors.set(group, slot + 1);
            }
    }
}
