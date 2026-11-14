package application.bootstrap.geometrypipeline.meshmanager;

import java.util.Arrays;

import application.bootstrap.geometrypipeline.mesh.MeshZone;
import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

class RiggedMeshArpgUtility extends EngineUtility {

    /*
     * The single definition of the rigged mesh format: a mesh ARPG file naming
     * its "rig" whose "vbo" lists textured quad entries, each with four
     * "quad" corners, the "bones" weighting it — once for the quad or once
     * per corner, a lone bone weighing in full without a "weight" — its
     * "texture", optionally its own corner "uvs" and the "zone" it is made of.
     * Corners standing on one position with the same influences parse into one
     * shared vertex, numbered in the order they first appear; "masks" and
     * "morphs" name vertices by that number, each mask channel and morph
     * target listing the "vertices" it touches beside their "values" or
     * three "deltas" each. Writing
     * keeps every other field of the source file in place and names a quad's
     * bones once wherever its corners agree, so a mesh read and written
     * unchanged comes back exactly as it was. MeshBuilder parses through here
     * and the character editor reads and writes through here.
     */

    // Detection \\

    static boolean isRigged(ArpgObjectStruct meshArpg) {
        return meshArpg.has("rig") && !meshArpg.get("rig").isNull();
    }

    static String getRigName(ArpgObjectStruct meshArpg) {
        return ArpgUtility.validateString(meshArpg, "rig");
    }

    // Parse \\

    static RiggedMeshStruct parse(ArpgObjectStruct meshArpg, RigHandle rigHandle, String sourceName) {

        if (meshArpg.has("ibo") && !meshArpg.get("ibo").isNull())
            throwException("Rigged mesh declares an 'ibo' — its indices come from its quad entries alone. File: "
                    + sourceName);

        ArpgArrayStruct vboArpg = ArpgUtility.validateArray(meshArpg, "vbo");
        RiggedMeshStruct mesh = new RiggedMeshStruct(rigHandle);
        Object2IntOpenHashMap<String> corner2Vertex = new Object2IntOpenHashMap<>();
        StringBuilder cornerKey = new StringBuilder();
        int[] cornerVertices = new int[EngineSetting.QUAD_VERTEX_COUNT];

        corner2Vertex.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);

        for (ArpgElementStruct element : vboArpg) {

            if (!element.isObject())
                throwException("Rigged mesh vertex data must be quad entries — a raw vertex array cannot carry "
                        + "bone weights. File: " + sourceName);

            parseQuad(mesh, element.getAsObject(), corner2Vertex, cornerKey, cornerVertices, sourceName);
        }

        parseMasks(mesh, meshArpg, sourceName);
        parseMorphs(mesh, meshArpg, sourceName);

        return mesh;
    }

    private static void parseQuad(
            RiggedMeshStruct mesh,
            ArpgObjectStruct quadArpg,
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

        parseInfluences(mesh, vertexIndex, influencesArpg, sourceName);

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

    private static void parseInfluences(
            RiggedMeshStruct mesh,
            int vertexIndex,
            ArpgArrayStruct influencesArpg,
            String sourceName) {

        int influenceCount = influencesArpg.size();
        RigHandle rigHandle = mesh.getRigHandle();

        if (influenceCount == 0 || influenceCount > EngineSetting.MAX_BONE_INFLUENCES)
            throwException("Quad \"bones\" must declare between 1 and "
                    + EngineSetting.MAX_BONE_INFLUENCES + " entries, found " + influenceCount
                    + " in file: " + sourceName);

        float weightSum = 0f;

        for (int i = 0; i < influenceCount; i++) {

            ArpgObjectStruct entry = influencesArpg.get(i).getAsObject();
            String boneName = ArpgUtility.validateString(entry, "bone");
            float weight = entry.has("weight") ? entry.get("weight").getAsFloat() : 1f;

            if (!rigHandle.hasBone(boneName))
                throwException("Quad references unknown bone \"" + boneName
                        + "\" for rig in file: " + sourceName);

            if (weight < 0f)
                throwException("Bone weight cannot be negative for bone \"" + boneName
                        + "\" in file: " + sourceName);

            mesh.addInfluence(vertexIndex, rigHandle.getBoneIndex(boneName), weight);
            weightSum += weight;
        }

        if (Math.abs(weightSum - 1f) > EngineSetting.BONE_WEIGHT_SUM_EPSILON)
            throwException("Quad bone weights must sum to 1.0, got " + weightSum
                    + " in file: " + sourceName);
    }

    // Masks \\

    private static void parseMasks(RiggedMeshStruct mesh, ArpgObjectStruct meshArpg, String sourceName) {

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

    private static int findMaskChannel(String channelName) {

        for (int i = 0; i < EngineSetting.RIGGED_MESH_MASK_CHANNELS.length; i++)
            if (EngineSetting.RIGGED_MESH_MASK_CHANNELS[i].equals(channelName))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Morphs \\

    private static void parseMorphs(RiggedMeshStruct mesh, ArpgObjectStruct meshArpg, String sourceName) {

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

    private static int validateVertex(RiggedMeshStruct mesh, int vertexIndex, String sourceName) {

        if (vertexIndex < 0 || vertexIndex >= mesh.getVertexCount())
            throwException("Rigged mesh names vertex " + vertexIndex + " but holds " + mesh.getVertexCount()
                    + " vertices. File: " + sourceName);

        return vertexIndex;
    }

    // Write \\

    // The source file's fields in their order, its quads, masks and morphs replaced by the mesh's own
    static ArpgObjectStruct write(RiggedMeshStruct mesh, ArpgObjectStruct sourceArpg) {

        ArpgObjectStruct meshArpg = new ArpgObjectStruct();
        int[] fileVertices = resolveFileVertices(mesh);

        for (String key : sourceArpg.keySet())
            if (!key.equals("masks") && !key.equals("morphs"))
                meshArpg.add(key, key.equals("vbo") ? writeQuads(mesh) : sourceArpg.get(key).deepCopy());

        if (!meshArpg.has("vbo"))
            meshArpg.add("vbo", writeQuads(mesh));

        ArpgObjectStruct masksArpg = writeMasks(mesh, fileVertices);
        ArpgObjectStruct morphsArpg = writeMorphs(mesh, fileVertices);

        if (masksArpg.size() > 0)
            meshArpg.add("masks", masksArpg);

        if (morphsArpg.size() > 0)
            meshArpg.add("morphs", morphsArpg);

        return meshArpg;
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

    private static ArpgObjectStruct writeMasks(RiggedMeshStruct mesh, int[] fileVertices) {

        ArpgObjectStruct masksArpg = new ArpgObjectStruct();

        for (int channel = 0; channel < EngineSetting.RIGGED_MESH_MASK_CHANNELS.length; channel++) {

            ArpgArrayStruct vertices = new ArpgArrayStruct();
            ArpgArrayStruct values = new ArpgArrayStruct();

            for (int v = 0; v < mesh.getVertexCount(); v++) {

                float value = mesh.getMask(v, channel);

                if (fileVertices[v] == EngineSetting.INDEX_NOT_FOUND || value <= 0f)
                    continue;

                vertices.add(fileVertices[v]);
                values.add(value);
            }

            if (vertices.size() == 0)
                continue;

            ArpgObjectStruct channelArpg = new ArpgObjectStruct();
            channelArpg.add("vertices", vertices);
            channelArpg.add("values", values);
            masksArpg.add(EngineSetting.RIGGED_MESH_MASK_CHANNELS[channel], channelArpg);
        }

        return masksArpg;
    }

    private static ArpgObjectStruct writeMorphs(RiggedMeshStruct mesh, int[] fileVertices) {

        ArpgObjectStruct morphsArpg = new ArpgObjectStruct();

        for (int m = 0; m < mesh.getMorphCount(); m++) {

            ArpgArrayStruct vertices = new ArpgArrayStruct();
            ArpgArrayStruct deltas = new ArpgArrayStruct();

            for (int e = 0; e < mesh.getMorphEntryCount(m); e++) {

                int fileVertex = fileVertices[mesh.getMorphVertex(m, e)];

                if (fileVertex == EngineSetting.INDEX_NOT_FOUND)
                    continue;

                vertices.add(fileVertex);

                for (int a = 0; a < EngineSetting.RIGGED_MESH_POSITION_FLOATS; a++)
                    deltas.add(mesh.getMorphDelta(m, e, a));
            }

            ArpgObjectStruct morphArpg = new ArpgObjectStruct();
            morphArpg.add("vertices", vertices);
            morphArpg.add("deltas", deltas);
            morphsArpg.add(mesh.getMorphName(m), morphArpg);
        }

        return morphsArpg;
    }

    private static ArpgArrayStruct writeQuads(RiggedMeshStruct mesh) {

        ArpgArrayStruct quadsArpg = new ArpgArrayStruct();

        for (int f = 0; f < mesh.getFaceCount(); f++)
            quadsArpg.add(writeQuad(mesh, f));

        return quadsArpg;
    }

    private static ArpgObjectStruct writeQuad(RiggedMeshStruct mesh, int faceIndex) {

        ArpgObjectStruct quadArpg = new ArpgObjectStruct();
        ArpgArrayStruct cornersArpg = new ArpgArrayStruct();

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            int vertexIndex = mesh.getFaceVertex(faceIndex, c);
            ArpgArrayStruct cornerArpg = new ArpgArrayStruct();

            cornerArpg.add(mesh.getX(vertexIndex));
            cornerArpg.add(mesh.getY(vertexIndex));
            cornerArpg.add(mesh.getZ(vertexIndex));
            cornersArpg.add(cornerArpg);
        }

        quadArpg.add("quad", cornersArpg);
        quadArpg.add("bones", writeBones(mesh, faceIndex));
        quadArpg.addProperty("texture", mesh.getTextureName(mesh.getFaceTexture(faceIndex)));

        if (mesh.hasAuthoredUVs(faceIndex))
            quadArpg.add("uvs", writeUVs(mesh, faceIndex));

        if (mesh.getFaceZone(faceIndex) != MeshZone.SKIN)
            quadArpg.addProperty("zone", ArpgUtility.toEnumName(mesh.getFaceZone(faceIndex)));

        return quadArpg;
    }

    // One influence list for the quad when its corners agree, one per corner when they do not
    private static ArpgArrayStruct writeBones(RiggedMeshStruct mesh, int faceIndex) {

        if (mesh.hasUniformInfluences(faceIndex))
            return writeInfluences(mesh, mesh.getFaceVertex(faceIndex, 0));

        ArpgArrayStruct bonesArpg = new ArpgArrayStruct();

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
            bonesArpg.add(writeInfluences(mesh, mesh.getFaceVertex(faceIndex, c)));

        return bonesArpg;
    }

    private static ArpgArrayStruct writeInfluences(RiggedMeshStruct mesh, int vertexIndex) {

        ArpgArrayStruct influencesArpg = new ArpgArrayStruct();
        int influenceCount = mesh.getInfluenceCount(vertexIndex);
        RigHandle rigHandle = mesh.getRigHandle();

        for (int i = 0; i < influenceCount; i++) {

            ArpgObjectStruct entry = new ArpgObjectStruct();
            entry.addProperty("bone", rigHandle.getBone(mesh.getInfluenceBone(vertexIndex, i)).getName());

            if (influenceCount > 1)
                entry.addProperty("weight", mesh.getInfluenceWeight(vertexIndex, i));

            influencesArpg.add(entry);
        }

        return influencesArpg;
    }

    private static ArpgArrayStruct writeUVs(RiggedMeshStruct mesh, int faceIndex) {

        ArpgArrayStruct uvsArpg = new ArpgArrayStruct();

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            ArpgArrayStruct pair = new ArpgArrayStruct();

            pair.add(mesh.getFaceU(faceIndex, c));
            pair.add(mesh.getFaceV(faceIndex, c));
            uvsArpg.add(pair);
        }

        return uvsArpg;
    }
}
