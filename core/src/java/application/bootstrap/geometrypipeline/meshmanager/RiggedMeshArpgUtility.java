package application.bootstrap.geometrypipeline.meshmanager;

import java.util.Arrays;

import application.bootstrap.geometrypipeline.mesh.MeshZone;
import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgBufferStruct;
import engine.util.arpg.ArpgBufferType;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RiggedMeshArpgUtility extends EngineUtility {

    /*
     * The single definition of the rigged mesh format: a mesh ARPG file naming
     * its "rig" whose "mesh" object holds the model as typed arrays, quad by
     * quad. "positions" lists each distinct vertex once; "bones" names the
     * bones the file weighs onto, resolved against the rig by name at load,
     * with "influence_counts" per vertex over the runs of "influence_bones"
     * and "influence_weights"; "quads" indexes four vertices per face beside
     * "quad_textures" and "quad_zones", indices into the "textures" and
     * "zones" name lists, "quad_uv_flags" marking the faces that author the
     * eight floats they take in "quad_uvs" (and, in the bits above, the
     * corners pinned for unwrapping), the optional "seams" the edges cut for
     * unwrapping as vertex pairs; "masks" and "morphs" are sparse, each
     * channel and target listing the "vertices" it touches beside their
     * "values" or three "deltas" each; and "normals" stores every corner's
     * smoothed normal, packed as the vertex buffer holds it, so loading does
     * not compute them. A file still in the first layout, a "vbo" of quad
     * entries each carrying its own corners, bones, texture, UVs and zone, is
     * read by rewriting its tree into this one first; only this layout is
     * ever written. Writing keeps every other field of the source file in
     * place, so a mesh read and written unchanged comes back exactly as it
     * was. MeshBuilder parses through here, the character editor reads and
     * writes through here and the ARPG tool upgrades files through here.
     */

    // Detection \\

    static boolean isRigged(ArpgObjectStruct meshArpg) {
        return meshArpg.has("rig") && !meshArpg.get("rig").isNull();
    }

    static String getRigName(ArpgObjectStruct meshArpg) {
        return ArpgUtility.validateString(meshArpg, "rig");
    }

    // A rigged file whose geometry is still quad entries under "vbo"
    static boolean isQuadLayout(ArpgObjectStruct meshArpg) {
        return isRigged(meshArpg) && !ArpgUtility.hasObject(meshArpg, "mesh") && ArpgUtility.hasArray(meshArpg, "vbo");
    }

    // Parse \\

    static RiggedMeshStruct parse(ArpgObjectStruct meshArpg, RigHandle rigHandle, String sourceName) {

        if (meshArpg.has("ibo") && !meshArpg.get("ibo").isNull())
            throwException("Rigged mesh declares an 'ibo' — its indices come from its quads alone. File: "
                    + sourceName);

        ArpgObjectStruct compactArpg = ArpgUtility.validateObject(
                isQuadLayout(meshArpg) ? upgrade(meshArpg, sourceName) : meshArpg, "mesh");
        RiggedMeshStruct mesh = new RiggedMeshStruct(rigHandle);

        parseGeometry(mesh, compactArpg, sourceName);
        parseInfluences(mesh, compactArpg, sourceName);
        parseMasks(mesh, compactArpg, sourceName);
        parseMorphs(mesh, compactArpg, sourceName);

        return mesh;
    }

    // Every corner's stored normal, or null where the file stores none that fit the mesh and they must be computed
    static int[] parseNormals(ArpgObjectStruct meshArpg, RiggedMeshStruct mesh) {

        if (!ArpgUtility.hasObject(meshArpg, "mesh") || !ArpgUtility.hasBuffer(meshArpg.getAsObject("mesh"), "normals"))
            return null;

        ArpgBufferStruct normals = meshArpg.getAsObject("mesh").get("normals").getAsBuffer();
        int cornerCount = mesh.getFaceCount() * EngineSetting.QUAD_VERTEX_COUNT;

        if (normals.getBufferType() != ArpgBufferType.INT32 || normals.size() != cornerCount)
            return null;

        int[] packed = new int[cornerCount];

        for (int i = 0; i < cornerCount; i++)
            packed[i] = normals.getInt(i);

        return packed;
    }

    private static void parseGeometry(RiggedMeshStruct mesh, ArpgObjectStruct compactArpg, String sourceName) {

        ArpgBufferStruct positions = ArpgUtility.validateBuffer(compactArpg, "positions", ArpgBufferType.FLOAT32);
        ArpgBufferStruct quads = ArpgUtility.validateBuffer(compactArpg, "quads");
        ArpgBufferStruct quadTextures = ArpgUtility.validateBuffer(compactArpg, "quad_textures");
        ArpgBufferStruct quadZones = ArpgUtility.validateBuffer(compactArpg, "quad_zones");
        ArpgBufferStruct uvFlags = ArpgUtility.validateBuffer(compactArpg, "quad_uv_flags");
        ArpgBufferStruct uvs = ArpgUtility.validateBuffer(compactArpg, "quad_uvs", ArpgBufferType.FLOAT32);
        ArpgArrayStruct texturesArpg = ArpgUtility.validateArray(compactArpg, "textures");
        ArpgArrayStruct zonesArpg = ArpgUtility.validateArray(compactArpg, "zones");
        int vertexCount = positions.size() / EngineSetting.RIGGED_MESH_POSITION_FLOATS;
        int faceCount = quads.size() / EngineSetting.QUAD_VERTEX_COUNT;
        int uvFloats = EngineSetting.QUAD_VERTEX_COUNT * EngineSetting.RIGGED_MESH_UV_FLOATS;

        if (positions.size() % EngineSetting.RIGGED_MESH_POSITION_FLOATS != 0
                || quads.size() % EngineSetting.QUAD_VERTEX_COUNT != 0
                || quadTextures.size() != faceCount
                || quadZones.size() != faceCount
                || uvFlags.size() != faceCount)
            throwException("Rigged mesh blocks do not agree on its vertex and quad counts. File: " + sourceName);

        int[] textures = new int[texturesArpg.size()];
        MeshZone[] zones = new MeshZone[zonesArpg.size()];

        for (int t = 0; t < textures.length; t++)
            textures[t] = mesh.resolveTexture(texturesArpg.get(t).getAsString());

        for (int z = 0; z < zones.length; z++)
            zones[z] = ArpgUtility.toEnum(zonesArpg.get(z).getAsString(), MeshZone.class);

        for (int v = 0; v < vertexCount; v++) {

            int base = v * EngineSetting.RIGGED_MESH_POSITION_FLOATS;
            mesh.addVertex(positions.getFloat(base), positions.getFloat(base + 1), positions.getFloat(base + 2));
        }

        int uvBase = 0;

        for (int f = 0; f < faceCount; f++) {

            int base = f * EngineSetting.QUAD_VERTEX_COUNT;
            int texture = quadTextures.getInt(f);
            int zone = quadZones.getInt(f);

            if (texture >= textures.length || zone >= zones.length)
                throwException("Rigged mesh quad " + f + " names a texture or zone the file does not list. File: "
                        + sourceName);

            int faceIndex = mesh.addFace(
                    validateVertex(mesh, quads.getInt(base), sourceName),
                    validateVertex(mesh, quads.getInt(base + 1), sourceName),
                    validateVertex(mesh, quads.getInt(base + 2), sourceName),
                    validateVertex(mesh, quads.getInt(base + 3), sourceName),
                    textures[texture]);

            mesh.setFaceZone(faceIndex, zones[zone]);

            int uvFlag = uvFlags.getInt(f);

            if ((uvFlag & EngineSetting.RIGGED_MESH_UV_AUTHORED_BIT) == 0)
                continue;

            if (uvBase + uvFloats > uvs.size())
                throwException("Rigged mesh lists fewer UVs than its quads author. File: " + sourceName);

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
                mesh.setFaceUV(
                        faceIndex,
                        c,
                        uvs.getFloat(uvBase + c * EngineSetting.RIGGED_MESH_UV_FLOATS),
                        uvs.getFloat(uvBase + c * EngineSetting.RIGGED_MESH_UV_FLOATS + 1));

            mesh.setFacePins(faceIndex, uvFlag >> EngineSetting.RIGGED_MESH_UV_PIN_SHIFT);
            uvBase += uvFloats;
        }

        parseSeams(mesh, compactArpg, sourceName);
    }

    // The optional seams, two vertex numbers each
    private static void parseSeams(RiggedMeshStruct mesh, ArpgObjectStruct compactArpg, String sourceName) {

        ArpgBufferStruct seams = ArpgUtility.getBuffer(compactArpg, "seams");

        if (seams == null)
            return;

        if (seams.size() % 2 != 0)
            throwException("Rigged mesh seams must come in vertex pairs. File: " + sourceName);

        for (int i = 0; i < seams.size(); i += 2)
            mesh.setSeam(
                    validateVertex(mesh, seams.getInt(i), sourceName),
                    validateVertex(mesh, seams.getInt(i + 1), sourceName),
                    true);
    }

    private static void parseInfluences(RiggedMeshStruct mesh, ArpgObjectStruct compactArpg, String sourceName) {

        ArpgArrayStruct bonesArpg = ArpgUtility.validateArray(compactArpg, "bones");
        ArpgBufferStruct counts = ArpgUtility.validateBuffer(compactArpg, "influence_counts");
        ArpgBufferStruct bones = ArpgUtility.validateBuffer(compactArpg, "influence_bones");
        ArpgBufferStruct weights = ArpgUtility.validateBuffer(
                compactArpg, "influence_weights", ArpgBufferType.FLOAT32);
        RigHandle rigHandle = mesh.getRigHandle();
        int[] rigBones = new int[bonesArpg.size()];

        if (counts.size() != mesh.getVertexCount() || bones.size() != weights.size())
            throwException("Rigged mesh influence blocks do not agree on their counts. File: " + sourceName);

        for (int b = 0; b < rigBones.length; b++) {

            String boneName = bonesArpg.get(b).getAsString();

            if (!rigHandle.hasBone(boneName))
                throwException("Rigged mesh references unknown bone \"" + boneName
                        + "\" for rig in file: " + sourceName);

            rigBones[b] = rigHandle.getBoneIndex(boneName);
        }

        int cursor = 0;

        for (int v = 0; v < mesh.getVertexCount(); v++) {

            int influenceCount = counts.getInt(v);
            float weightSum = 0f;

            if (influenceCount == 0 || influenceCount > EngineSetting.MAX_BONE_INFLUENCES
                    || cursor + influenceCount > bones.size())
                throwException("Rigged mesh vertex " + v + " must hold between 1 and "
                        + EngineSetting.MAX_BONE_INFLUENCES + " bone influences, found " + influenceCount
                        + " in file: " + sourceName);

            for (int i = 0; i < influenceCount; i++) {

                int bone = bones.getInt(cursor);
                float weight = weights.getFloat(cursor++);

                if (bone >= rigBones.length || weight < 0f)
                    throwException("Rigged mesh vertex " + v + " holds an unlisted bone or a negative weight in "
                            + "file: " + sourceName);

                mesh.addInfluence(v, rigBones[bone], weight);
                weightSum += weight;
            }

            if (Math.abs(weightSum - 1f) > EngineSetting.BONE_WEIGHT_SUM_EPSILON)
                throwException("Rigged mesh bone weights must sum to 1.0, got " + weightSum
                        + " on vertex " + v + " in file: " + sourceName);
        }
    }

    private static void parseMasks(RiggedMeshStruct mesh, ArpgObjectStruct compactArpg, String sourceName) {

        if (!ArpgUtility.hasObject(compactArpg, "masks"))
            return;

        ArpgObjectStruct masksArpg = compactArpg.getAsObject("masks");

        for (String channelName : masksArpg.keySet()) {

            int channel = findMaskChannel(channelName);

            if (channel == EngineSetting.INDEX_NOT_FOUND)
                throwException("Rigged mesh declares unknown mask channel \"" + channelName + "\". File: "
                        + sourceName);

            ArpgObjectStruct channelArpg = masksArpg.getAsObject(channelName);
            ArpgBufferStruct vertices = ArpgUtility.validateBuffer(channelArpg, "vertices");
            ArpgBufferStruct values = ArpgUtility.validateBuffer(channelArpg, "values", ArpgBufferType.FLOAT32);

            if (values.size() != vertices.size())
                throwException("Mask channel \"" + channelName + "\" lists " + vertices.size() + " vertices but "
                        + values.size() + " values. File: " + sourceName);

            for (int i = 0; i < vertices.size(); i++)
                mesh.setMask(validateVertex(mesh, vertices.getInt(i), sourceName), channel, values.getFloat(i));
        }
    }

    private static void parseMorphs(RiggedMeshStruct mesh, ArpgObjectStruct compactArpg, String sourceName) {

        if (!ArpgUtility.hasObject(compactArpg, "morphs"))
            return;

        ArpgObjectStruct morphsArpg = compactArpg.getAsObject("morphs");

        for (String morphName : morphsArpg.keySet()) {

            ArpgObjectStruct morphArpg = morphsArpg.getAsObject(morphName);
            ArpgBufferStruct vertices = ArpgUtility.validateBuffer(morphArpg, "vertices");
            ArpgBufferStruct deltas = ArpgUtility.validateBuffer(morphArpg, "deltas", ArpgBufferType.FLOAT32);
            int morphIndex = mesh.resolveMorph(morphName);

            if (deltas.size() != vertices.size() * EngineSetting.RIGGED_MESH_POSITION_FLOATS)
                throwException("Morph \"" + morphName + "\" lists " + vertices.size() + " vertices but "
                        + deltas.size() + " delta floats, expected three per vertex. File: " + sourceName);

            for (int i = 0; i < vertices.size(); i++) {

                int vertexIndex = validateVertex(mesh, vertices.getInt(i), sourceName);
                int base = i * EngineSetting.RIGGED_MESH_POSITION_FLOATS;

                if (mesh.findMorphEntry(morphIndex, vertexIndex) != EngineSetting.INDEX_NOT_FOUND)
                    throwException("Morph \"" + morphName + "\" lists vertex " + vertexIndex + " twice. File: "
                            + sourceName);

                mesh.setMorphDelta(
                        morphIndex,
                        vertexIndex,
                        deltas.getFloat(base),
                        deltas.getFloat(base + 1),
                        deltas.getFloat(base + 2));
            }
        }
    }

    private static int findMaskChannel(String channelName) {

        for (int i = 0; i < EngineSetting.RIGGED_MESH_MASK_CHANNELS.length; i++)
            if (EngineSetting.RIGGED_MESH_MASK_CHANNELS[i].equals(channelName))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    private static int validateVertex(RiggedMeshStruct mesh, int vertexIndex, String sourceName) {

        if (vertexIndex < 0 || vertexIndex >= mesh.getVertexCount())
            throwException("Rigged mesh names vertex " + vertexIndex + " but holds " + mesh.getVertexCount()
                    + " vertices. File: " + sourceName);

        return vertexIndex;
    }

    // Write \\

    // The source file's fields in their order, its geometry replaced by the mesh's own in the compact layout
    static ArpgObjectStruct write(RiggedMeshStruct mesh, ArpgObjectStruct sourceArpg) {

        RigHandle rigHandle = mesh.getRigHandle();
        ObjectArrayList<String> boneNames = new ObjectArrayList<>(rigHandle.getBoneCount());

        for (int b = 0; b < rigHandle.getBoneCount(); b++)
            boneNames.add(rigHandle.getBone(b).getName());

        return write(mesh, boneNames, sourceArpg);
    }

    private static ArpgObjectStruct write(
            RiggedMeshStruct mesh,
            ObjectArrayList<String> boneNames,
            ArpgObjectStruct sourceArpg) {

        ArpgObjectStruct meshArpg = new ArpgObjectStruct();
        ArpgObjectStruct compactArpg = writeCompact(mesh, boneNames);

        for (String key : sourceArpg.keySet()) {

            boolean geometry = key.equals("vbo") || key.equals("mesh");

            if (geometry && !meshArpg.has("mesh"))
                meshArpg.add("mesh", compactArpg);

            if (!geometry && !key.equals("masks") && !key.equals("morphs"))
                meshArpg.add(key, sourceArpg.get(key).deepCopy());
        }

        if (!meshArpg.has("mesh"))
            meshArpg.add("mesh", compactArpg);

        return meshArpg;
    }

    private static ArpgObjectStruct writeCompact(RiggedMeshStruct mesh, ObjectArrayList<String> boneNames) {

        ArpgObjectStruct compactArpg = new ArpgObjectStruct();
        int[] fileVertices = resolveFileVertices(mesh);
        int[] vertices = resolveFileOrder(fileVertices);

        writeVertices(mesh, vertices, boneNames, compactArpg);
        writeQuads(mesh, fileVertices, compactArpg);

        ArpgObjectStruct masksArpg = writeMasks(mesh, fileVertices);
        ArpgObjectStruct morphsArpg = writeMorphs(mesh, fileVertices);

        if (masksArpg.size() > 0)
            compactArpg.add("masks", masksArpg);

        if (morphsArpg.size() > 0)
            compactArpg.add("morphs", morphsArpg);

        int[] normals = new int[mesh.getFaceCount() * EngineSetting.QUAD_VERTEX_COUNT];

        RiggedMeshGeometryUtility.packNormals(mesh, new RiggedMeshScratchStruct(), normals);
        compactArpg.add("normals", ArpgUtility.toBuffer(ArpgBufferType.INT32, normals, normals.length));

        return compactArpg;
    }

    // The number each vertex takes when the written file is parsed back — corners sharing a position and influences
    // become one vertex, in the order they first appear — or INDEX_NOT_FOUND for a vertex no face uses, or one
    // folded into an earlier vertex
    private static int[] resolveFileVertices(RiggedMeshStruct mesh) {

        int[] fileVertices = new int[mesh.getVertexCount()];
        Object2IntOpenHashMap<String> key2File = new Object2IntOpenHashMap<>();
        StringBuilder key = new StringBuilder();

        key2File.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
        Arrays.fill(fileVertices, EngineSetting.INDEX_NOT_FOUND);

        for (int f = 0; f < mesh.getFaceCount(); f++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int vertexIndex = mesh.getFaceVertex(f, c);

                key.setLength(0);
                appendVertexKey(mesh, vertexIndex, key);

                String vertexKey = key.toString();

                if (key2File.getInt(vertexKey) != EngineSetting.INDEX_NOT_FOUND)
                    continue;

                key2File.put(vertexKey, key2File.size());
                fileVertices[vertexIndex] = key2File.size() - 1;
            }

        return fileVertices;
    }

    // The mesh vertex each file vertex is written from, in file order
    private static int[] resolveFileOrder(int[] fileVertices) {

        int count = 0;

        for (int fileVertex : fileVertices)
            count = Math.max(count, fileVertex + 1);

        int[] vertices = new int[count];

        for (int v = 0; v < fileVertices.length; v++)
            if (fileVertices[v] != EngineSetting.INDEX_NOT_FOUND)
                vertices[fileVertices[v]] = v;

        return vertices;
    }

    private static void writeVertices(
            RiggedMeshStruct mesh,
            int[] vertices,
            ObjectArrayList<String> boneNames,
            ArpgObjectStruct compactArpg) {

        float[] positions = new float[vertices.length * EngineSetting.RIGGED_MESH_POSITION_FLOATS];
        int[] counts = new int[vertices.length];
        int[] fileBones = new int[boneNames.size()];
        ArpgArrayStruct bonesArpg = new ArpgArrayStruct();
        IntArrayList bones = new IntArrayList();
        FloatArrayList weights = new FloatArrayList();

        Arrays.fill(fileBones, EngineSetting.INDEX_NOT_FOUND);

        for (int i = 0; i < vertices.length; i++) {

            int vertexIndex = vertices[i];
            int base = i * EngineSetting.RIGGED_MESH_POSITION_FLOATS;

            positions[base] = mesh.getX(vertexIndex);
            positions[base + 1] = mesh.getY(vertexIndex);
            positions[base + 2] = mesh.getZ(vertexIndex);
            counts[i] = mesh.getInfluenceCount(vertexIndex);

            for (int n = 0; n < counts[i]; n++) {

                int bone = mesh.getInfluenceBone(vertexIndex, n);

                if (fileBones[bone] == EngineSetting.INDEX_NOT_FOUND) {
                    fileBones[bone] = bonesArpg.size();
                    bonesArpg.add(boneNames.get(bone));
                }

                bones.add(fileBones[bone]);
                weights.add(mesh.getInfluenceWeight(vertexIndex, n));
            }
        }

        compactArpg.add("positions", ArpgUtility.toBuffer(positions, positions.length));
        compactArpg.add("bones", bonesArpg);
        compactArpg.add("influence_counts", ArpgUtility.toBuffer(ArpgBufferType.UINT8, counts, counts.length));
        compactArpg.add("influence_bones", ArpgUtility.toIndexBuffer(bones.elements(), bones.size()));
        compactArpg.add("influence_weights", ArpgUtility.toBuffer(weights.elements(), weights.size()));
    }

    private static void writeQuads(
            RiggedMeshStruct mesh,
            int[] fileVertices,
            ArpgObjectStruct compactArpg) {

        int faceCount = mesh.getFaceCount();
        int[] quads = new int[faceCount * EngineSetting.QUAD_VERTEX_COUNT];
        int[] quadTextures = new int[faceCount];
        int[] quadZones = new int[faceCount];
        int[] uvFlags = new int[faceCount];
        int[] fileTextures = new int[mesh.getTextureCount()];
        int[] fileZones = new int[MeshZone.VALUES.length];
        ArpgArrayStruct texturesArpg = new ArpgArrayStruct();
        ArpgArrayStruct zonesArpg = new ArpgArrayStruct();
        FloatArrayList uvs = new FloatArrayList();

        Arrays.fill(fileTextures, EngineSetting.INDEX_NOT_FOUND);
        Arrays.fill(fileZones, EngineSetting.INDEX_NOT_FOUND);

        for (int f = 0; f < faceCount; f++) {

            int texture = mesh.getFaceTexture(f);
            int zone = mesh.getFaceZone(f).ordinal();

            if (fileTextures[texture] == EngineSetting.INDEX_NOT_FOUND) {
                fileTextures[texture] = texturesArpg.size();
                texturesArpg.add(mesh.getTextureName(texture));
            }

            if (fileZones[zone] == EngineSetting.INDEX_NOT_FOUND) {
                fileZones[zone] = zonesArpg.size();
                zonesArpg.add(ArpgUtility.toEnumName(mesh.getFaceZone(f)));
            }

            quadTextures[f] = fileTextures[texture];
            quadZones[f] = fileZones[zone];
            uvFlags[f] = mesh.hasAuthoredUVs(f)
                    ? EngineSetting.RIGGED_MESH_UV_AUTHORED_BIT
                            | mesh.getFacePins(f) << EngineSetting.RIGGED_MESH_UV_PIN_SHIFT
                    : 0;

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                quads[f * EngineSetting.QUAD_VERTEX_COUNT + c] = fileVertices[mesh.getFaceVertex(f, c)];

                if (mesh.hasAuthoredUVs(f)) {
                    uvs.add(mesh.getFaceU(f, c));
                    uvs.add(mesh.getFaceV(f, c));
                }
            }
        }

        compactArpg.add("quads", ArpgUtility.toIndexBuffer(quads, quads.length));
        compactArpg.add("textures", texturesArpg);
        compactArpg.add("quad_textures", ArpgUtility.toIndexBuffer(quadTextures, faceCount));
        compactArpg.add("zones", zonesArpg);
        compactArpg.add("quad_zones", ArpgUtility.toIndexBuffer(quadZones, faceCount));
        compactArpg.add("quad_uv_flags", ArpgUtility.toBuffer(ArpgBufferType.UINT8, uvFlags, faceCount));
        compactArpg.add("quad_uvs", ArpgUtility.toBuffer(uvs.elements(), uvs.size()));
        writeSeams(mesh, fileVertices, compactArpg);
    }

    // Every seam between two vertices the file keeps apart, as file vertex pairs; written only when there are any
    private static void writeSeams(RiggedMeshStruct mesh, int[] fileVertices, ArpgObjectStruct compactArpg) {

        if (mesh.getSeamCount() == 0)
            return;

        IntArrayList seams = new IntArrayList();

        for (long seamKey : mesh.getSortedSeams()) {

            int vertexA = fileVertices[RiggedMeshStruct.getSeamVertex(seamKey, 0)];
            int vertexB = fileVertices[RiggedMeshStruct.getSeamVertex(seamKey, 1)];

            if (vertexA == EngineSetting.INDEX_NOT_FOUND || vertexB == EngineSetting.INDEX_NOT_FOUND
                    || vertexA == vertexB)
                continue;

            seams.add(vertexA);
            seams.add(vertexB);
        }

        if (!seams.isEmpty())
            compactArpg.add("seams", ArpgUtility.toIndexBuffer(seams.elements(), seams.size()));
    }

    private static ArpgObjectStruct writeMasks(RiggedMeshStruct mesh, int[] fileVertices) {

        ArpgObjectStruct masksArpg = new ArpgObjectStruct();
        IntArrayList vertices = new IntArrayList();
        FloatArrayList values = new FloatArrayList();

        for (int channel = 0; channel < EngineSetting.RIGGED_MESH_MASK_CHANNELS.length; channel++) {

            vertices.clear();
            values.clear();

            for (int v = 0; v < mesh.getVertexCount(); v++) {

                float value = mesh.getMask(v, channel);

                if (fileVertices[v] == EngineSetting.INDEX_NOT_FOUND || value <= 0f)
                    continue;

                vertices.add(fileVertices[v]);
                values.add(value);
            }

            if (vertices.isEmpty())
                continue;

            ArpgObjectStruct channelArpg = new ArpgObjectStruct();
            channelArpg.add("vertices", ArpgUtility.toIndexBuffer(vertices.elements(), vertices.size()));
            channelArpg.add("values", ArpgUtility.toBuffer(values.elements(), values.size()));
            masksArpg.add(EngineSetting.RIGGED_MESH_MASK_CHANNELS[channel], channelArpg);
        }

        return masksArpg;
    }

    private static ArpgObjectStruct writeMorphs(RiggedMeshStruct mesh, int[] fileVertices) {

        ArpgObjectStruct morphsArpg = new ArpgObjectStruct();
        IntArrayList vertices = new IntArrayList();
        FloatArrayList deltas = new FloatArrayList();

        for (int m = 0; m < mesh.getMorphCount(); m++) {

            vertices.clear();
            deltas.clear();

            for (int e = 0; e < mesh.getMorphEntryCount(m); e++) {

                int fileVertex = fileVertices[mesh.getMorphVertex(m, e)];

                if (fileVertex == EngineSetting.INDEX_NOT_FOUND)
                    continue;

                vertices.add(fileVertex);

                for (int a = 0; a < EngineSetting.RIGGED_MESH_POSITION_FLOATS; a++)
                    deltas.add(mesh.getMorphDelta(m, e, a));
            }

            ArpgObjectStruct morphArpg = new ArpgObjectStruct();
            morphArpg.add("vertices", ArpgUtility.toIndexBuffer(vertices.elements(), vertices.size()));
            morphArpg.add("deltas", ArpgUtility.toBuffer(deltas.elements(), deltas.size()));
            morphsArpg.add(mesh.getMorphName(m), morphArpg);
        }

        return morphsArpg;
    }

    // Upgrade \\

    // A file in the quad layout rewritten in the compact one: its quad entries parsed against the bone names they use
    // rather than a rig, then written as any mesh is
    static ArpgObjectStruct upgrade(ArpgObjectStruct meshArpg, String sourceName) {

        ArpgArrayStruct vboArpg = ArpgUtility.validateArray(meshArpg, "vbo");
        RiggedMeshStruct mesh = new RiggedMeshStruct((RigHandle) null);
        ObjectArrayList<String> boneNames = new ObjectArrayList<>();
        Object2IntOpenHashMap<String> corner2Vertex = new Object2IntOpenHashMap<>();
        StringBuilder cornerKey = new StringBuilder();
        int[] cornerVertices = new int[EngineSetting.QUAD_VERTEX_COUNT];

        corner2Vertex.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);

        for (ArpgElementStruct element : vboArpg) {

            if (!element.isObject())
                throwException("Rigged mesh vertex data must be quad entries — a raw vertex array cannot carry "
                        + "bone weights. File: " + sourceName);

            parseQuad(mesh, element.getAsObject(), boneNames, corner2Vertex, cornerKey, cornerVertices, sourceName);
        }

        parseQuadMasks(mesh, meshArpg, sourceName);
        parseQuadMorphs(mesh, meshArpg, sourceName);

        return write(mesh, boneNames, meshArpg);
    }

    private static int resolveName(ObjectArrayList<String> names, String name) {

        int index = names.indexOf(name);

        if (index != EngineSetting.INDEX_NOT_FOUND)
            return index;

        names.add(name);
        return names.size() - 1;
    }

    private static void parseQuad(
            RiggedMeshStruct mesh,
            ArpgObjectStruct quadArpg,
            ObjectArrayList<String> boneNames,
            Object2IntOpenHashMap<String> corner2Vertex,
            StringBuilder cornerKey,
            int[] cornerVertices,
            String sourceName) {

        if (!quadArpg.has("quad") || quadArpg.get("quad").isNull())
            throwException("Quad object missing 'quad' array in file: " + sourceName);

        ArpgArrayStruct corners = quadArpg.getAsArray("quad");

        if (corners.size() != EngineSetting.QUAD_VERTEX_COUNT)
            throwException("Quad 'quad' array must have exactly 4 corners in file: " + sourceName);

        if (!quadArpg.has("texture") || quadArpg.get("texture").isNull())
            throwException("Rigged mesh quad has no 'texture' — every corner of a rigged mesh carries a UV. File: "
                    + sourceName);

        ArpgArrayStruct bonesArray = validateBones(quadArpg, sourceName);
        boolean perCorner = bonesArray.size() > 0 && bonesArray.get(0).isArray();
        int textureIndex = mesh.resolveTexture(quadArpg.get("texture").getAsString());
        float[] cornerUVs = MeshQuadUtility.parseCornerUVs(quadArpg, sourceName);

        if (perCorner && bonesArray.size() != EngineSetting.QUAD_VERTEX_COUNT)
            throwException("Per-corner quad \"bones\" must declare exactly " + EngineSetting.QUAD_VERTEX_COUNT
                    + " influence lists, found " + bonesArray.size() + " in file: " + sourceName);

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
            cornerVertices[c] = resolveCornerVertex(
                    mesh,
                    corners.get(c).getAsArray(),
                    c,
                    perCorner ? bonesArray.get(c).getAsArray() : bonesArray,
                    boneNames,
                    corner2Vertex,
                    cornerKey,
                    sourceName);

        int faceIndex = mesh.addFace(
                cornerVertices[0], cornerVertices[1], cornerVertices[2], cornerVertices[3], textureIndex);

        mesh.setFaceZone(faceIndex, ArpgUtility.getEnum(quadArpg, "zone", MeshZone.class, MeshZone.SKIN));

        if (cornerUVs == null)
            return;

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
            mesh.setFaceUV(
                    faceIndex,
                    c,
                    cornerUVs[c * EngineSetting.RIGGED_MESH_UV_FLOATS],
                    cornerUVs[c * EngineSetting.RIGGED_MESH_UV_FLOATS + 1]);
    }

    private static ArpgArrayStruct validateBones(ArpgObjectStruct quadArpg, String sourceName) {

        if (!quadArpg.has("bones") || quadArpg.get("bones").isNull())
            throwException("Quad is missing \"bones\" in a rig-declaring mesh. Every quad must "
                    + "specify at least one bone. File: " + sourceName);

        return quadArpg.getAsArray("bones");
    }

    // The vertex a corner stands on — the one already at its position with its influences, or a new one
    private static int resolveCornerVertex(
            RiggedMeshStruct mesh,
            ArpgArrayStruct cornerArpg,
            int corner,
            ArpgArrayStruct influencesArpg,
            ObjectArrayList<String> boneNames,
            Object2IntOpenHashMap<String> corner2Vertex,
            StringBuilder cornerKey,
            String sourceName) {

        if (cornerArpg.size() != EngineSetting.RIGGED_MESH_POSITION_FLOATS)
            throwException("Textured quad corner " + corner + " has " + cornerArpg.size()
                    + " floats, expected " + EngineSetting.RIGGED_MESH_POSITION_FLOATS + " in file: " + sourceName);

        float x = cornerArpg.get(0).getAsFloat();
        float y = cornerArpg.get(1).getAsFloat();
        float z = cornerArpg.get(2).getAsFloat();
        int vertexIndex = mesh.addVertex(x, y, z);

        parseQuadInfluences(mesh, vertexIndex, influencesArpg, boneNames, sourceName);

        cornerKey.setLength(0);
        appendVertexKey(mesh, vertexIndex, cornerKey);

        int existing = corner2Vertex.getInt(cornerKey.toString());

        if (existing == EngineSetting.INDEX_NOT_FOUND) {
            corner2Vertex.put(cornerKey.toString(), vertexIndex);
            return vertexIndex;
        }

        mesh.removeLastVertex();
        return existing;
    }

    // Position and influences by their exact bits; adding zero folds negative zero into zero
    private static void appendVertexKey(RiggedMeshStruct mesh, int vertexIndex, StringBuilder key) {

        key.append(Float.floatToIntBits(mesh.getX(vertexIndex) + 0f)).append(',')
                .append(Float.floatToIntBits(mesh.getY(vertexIndex) + 0f)).append(',')
                .append(Float.floatToIntBits(mesh.getZ(vertexIndex) + 0f));

        for (int i = 0; i < mesh.getInfluenceCount(vertexIndex); i++)
            key.append('|').append(mesh.getInfluenceBone(vertexIndex, i)).append(':')
                    .append(Float.floatToIntBits(mesh.getInfluenceWeight(vertexIndex, i)));
    }

    private static void parseQuadInfluences(
            RiggedMeshStruct mesh,
            int vertexIndex,
            ArpgArrayStruct influencesArpg,
            ObjectArrayList<String> boneNames,
            String sourceName) {

        int influenceCount = influencesArpg.size();

        if (influenceCount == 0 || influenceCount > EngineSetting.MAX_BONE_INFLUENCES)
            throwException("Quad \"bones\" must declare between 1 and "
                    + EngineSetting.MAX_BONE_INFLUENCES + " entries, found " + influenceCount
                    + " in file: " + sourceName);

        float weightSum = 0f;

        for (int i = 0; i < influenceCount; i++) {

            ArpgObjectStruct entry = influencesArpg.get(i).getAsObject();
            String boneName = ArpgUtility.validateString(entry, "bone");
            float weight = entry.has("weight") ? entry.get("weight").getAsFloat() : 1f;

            if (weight < 0f)
                throwException("Bone weight cannot be negative for bone \"" + boneName
                        + "\" in file: " + sourceName);

            mesh.addInfluence(vertexIndex, resolveName(boneNames, boneName), weight);
            weightSum += weight;
        }

        if (Math.abs(weightSum - 1f) > EngineSetting.BONE_WEIGHT_SUM_EPSILON)
            throwException("Quad bone weights must sum to 1.0, got " + weightSum
                    + " in file: " + sourceName);
    }

    private static void parseQuadMasks(RiggedMeshStruct mesh, ArpgObjectStruct meshArpg, String sourceName) {

        if (!ArpgUtility.hasObject(meshArpg, "masks"))
            return;

        ArpgObjectStruct masksArpg = meshArpg.getAsObject("masks");

        for (String channelName : masksArpg.keySet()) {

            int channel = findMaskChannel(channelName);

            if (channel == EngineSetting.INDEX_NOT_FOUND)
                throwException("Rigged mesh declares unknown mask channel \"" + channelName + "\". File: "
                        + sourceName);

            ArpgObjectStruct channelArpg = masksArpg.getAsObject(channelName);
            ArpgArrayStruct vertices = ArpgUtility.validateArray(channelArpg, "vertices");
            ArpgArrayStruct values = ArpgUtility.validateArray(channelArpg, "values");

            if (values.size() != vertices.size())
                throwException("Mask channel \"" + channelName + "\" lists " + vertices.size() + " vertices but "
                        + values.size() + " values. File: " + sourceName);

            for (int i = 0; i < vertices.size(); i++)
                mesh.setMask(validateVertex(mesh, vertices.get(i).getAsInt(), sourceName), channel,
                        values.get(i).getAsFloat());
        }
    }


    private static void parseQuadMorphs(RiggedMeshStruct mesh, ArpgObjectStruct meshArpg, String sourceName) {

        if (!ArpgUtility.hasObject(meshArpg, "morphs"))
            return;

        ArpgObjectStruct morphsArpg = meshArpg.getAsObject("morphs");

        for (String morphName : morphsArpg.keySet()) {

            ArpgObjectStruct morphArpg = morphsArpg.getAsObject(morphName);
            ArpgArrayStruct vertices = ArpgUtility.validateArray(morphArpg, "vertices");
            ArpgArrayStruct deltas = ArpgUtility.validateArray(morphArpg, "deltas");
            int morphIndex = mesh.resolveMorph(morphName);

            if (deltas.size() != vertices.size() * EngineSetting.RIGGED_MESH_POSITION_FLOATS)
                throwException("Morph \"" + morphName + "\" lists " + vertices.size() + " vertices but "
                        + deltas.size() + " delta floats, expected three per vertex. File: " + sourceName);

            for (int i = 0; i < vertices.size(); i++) {

                int vertexIndex = validateVertex(mesh, vertices.get(i).getAsInt(), sourceName);
                int base = i * EngineSetting.RIGGED_MESH_POSITION_FLOATS;

                if (mesh.findMorphEntry(morphIndex, vertexIndex) != EngineSetting.INDEX_NOT_FOUND)
                    throwException("Morph \"" + morphName + "\" lists vertex " + vertexIndex + " twice. File: "
                            + sourceName);

                mesh.setMorphDelta(
                        morphIndex,
                        vertexIndex,
                        deltas.get(base).getAsFloat(),
                        deltas.get(base + 1).getAsFloat(),
                        deltas.get(base + 2).getAsFloat());
            }
        }
    }

}
