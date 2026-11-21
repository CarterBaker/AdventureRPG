package editor.bootstrap.charactereditorpipeline.util;

import java.util.Arrays;

import application.bootstrap.geometrypipeline.mesh.MeshZone;
import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.root.StructPackage;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class GltfImportUtility extends EngineUtility {

    /*
     * Turns an opened .glb file into a rigged mesh in two steps (see
     * .claude/docs/GLTF.md). inspect reads the first node that carries a
     * mesh: every primitive's vertices joined into one list, its triangles
     * turned back into quads (the fans FB_ngon_encoding marks, otherwise
     * pairs of triangles that share an edge and lie flat) and a triangle left
     * alone kept as a triangle face, its materials,
     * joints and morph names. build then makes the mesh against a rig once
     * the user has confirmed a texture and zone for every material: glTF
     * vertices standing on one position with the same influences become one
     * mesh vertex, joints become the rig's bones by name, and morph targets
     * keep their names. Anything the format cannot hold, a polygon of more
     * than four corners or a joint the rig lacks, is refused
     * with a catchable InternalException saying why; nothing is ever dropped
     * or remapped silently.
     */

    // Inspect \\

    public static GltfImportStruct inspect(GltfDocumentStruct document) {

        GltfImportStruct model = new GltfImportStruct();
        ArpgObjectStruct node = findMeshNode(document);
        ArpgObjectStruct mesh = GltfReadUtility.entry(document, "meshes", node.get("mesh").getAsInt());
        ArpgArrayStruct primitives = GltfReadUtility.list(mesh, "primitives");
        GeometryStruct geometry = new GeometryStruct();

        model.meshName = GltfReadUtility.text(mesh, "name", GltfReadUtility.text(node, "name", ""));
        model.skinned = ArpgUtility.hasNumber(node, "skin");

        if (model.skinned)
            readJoints(document, node.get("skin").getAsInt(), model);

        readMaterials(document, model);
        readMorphNames(mesh, primitives, model, geometry);

        for (ArpgElementStruct primitive : primitives)
            readPrimitive(document, primitive.getAsObject(), model, geometry);

        if (geometry.quads.isEmpty())
            throw new InternalException("The model has no faces");

        model.positions = geometry.positions.toFloatArray();
        model.uvs = geometry.hasUVs ? geometry.uvs.toFloatArray() : null;
        model.weights = geometry.weights.toFloatArray();
        model.joints = geometry.joints.toIntArray();
        model.masks = geometry.hasMasks ? geometry.masks.toFloatArray() : null;
        model.quads = geometry.quads.toIntArray();
        model.quadCorners = geometry.quadCorners.toIntArray();
        model.quadMaterials = geometry.quadMaterials.toIntArray();
        model.morphDeltas = new float[geometry.morphDeltas.size()][];

        for (int m = 0; m < model.morphDeltas.length; m++)
            model.morphDeltas[m] = geometry.morphDeltas.get(m).toFloatArray();

        return model;
    }

    private static ArpgObjectStruct findMeshNode(GltfDocumentStruct document) {

        ArpgObjectStruct found = null;

        for (ArpgElementStruct element : GltfReadUtility.list(document.getJson(), "nodes")) {

            ArpgObjectStruct node = element.getAsObject();

            if (!ArpgUtility.hasNumber(node, "mesh"))
                continue;

            if (ArpgUtility.hasNumber(node, "skin"))
                return node;

            found = found == null ? node : found;
        }

        if (found == null)
            throw new InternalException("The file holds no model");

        return found;
    }

    private static void readJoints(GltfDocumentStruct document, int skinIndex, GltfImportStruct model) {

        ArpgObjectStruct skin = GltfReadUtility.entry(document, "skins", skinIndex);
        ArpgArrayStruct joints = GltfReadUtility.list(skin, "joints");
        ArpgArrayStruct nodes = GltfReadUtility.list(document.getJson(), "nodes");
        int[] parents = new int[nodes.size()];
        float[][] worlds = new float[nodes.size()][];

        Arrays.fill(parents, EngineSetting.INDEX_NOT_FOUND);

        for (int n = 0; n < nodes.size(); n++)
            for (ArpgElementStruct child : GltfReadUtility.list(nodes.get(n).getAsObject(), "children"))
                parents[child.getAsInt()] = n;

        model.jointPoints = new float[joints.size() * 3];

        for (int j = 0; j < joints.size(); j++) {

            int node = joints.get(j).getAsInt();
            float[] world = resolveWorld(nodes, parents, worlds, node);
            int parent = parents[node];

            model.jointNames.add(GltfReadUtility.text(GltfReadUtility.entry(document, "nodes", node), "name", ""));
            model.jointParents.add(parent == EngineSetting.INDEX_NOT_FOUND
                    ? ""
                    : GltfReadUtility.text(nodes.get(parent).getAsObject(), "name", ""));
            System.arraycopy(world, 12, model.jointPoints, j * 3, 3);
        }
    }

    // A node's matrix in the world, column by column: its parents' matrices times its own
    private static float[] resolveWorld(ArpgArrayStruct nodes, int[] parents, float[][] worlds, int node) {

        if (worlds[node] != null)
            return worlds[node];

        float[] local = resolveLocal(nodes.get(node).getAsObject());

        if (parents[node] == EngineSetting.INDEX_NOT_FOUND)
            return worlds[node] = local;

        float[] parent = resolveWorld(nodes, parents, worlds, parents[node]);
        float[] world = new float[EditorSetting.GLTF_MATRIX_FLOATS];

        for (int column = 0; column < 4; column++)
            for (int row = 0; row < 4; row++)
                for (int k = 0; k < 4; k++)
                    world[column * 4 + row] += parent[k * 4 + row] * local[column * 4 + k];

        return worlds[node] = world;
    }

    // A node's own matrix: the one it states, or its translation, rotation and scale put together
    private static float[] resolveLocal(ArpgObjectStruct node) {

        float[] local = new float[EditorSetting.GLTF_MATRIX_FLOATS];
        ArpgArrayStruct matrix = GltfReadUtility.list(node, "matrix");

        if (matrix.size() == local.length) {

            for (int i = 0; i < local.length; i++)
                local[i] = matrix.get(i).getAsFloat();

            return local;
        }

        ArpgArrayStruct translation = GltfReadUtility.list(node, "translation");
        ArpgArrayStruct rotation = GltfReadUtility.list(node, "rotation");
        ArpgArrayStruct scale = GltfReadUtility.list(node, "scale");
        float x = rotation.size() == 4 ? rotation.get(0).getAsFloat() : 0f;
        float y = rotation.size() == 4 ? rotation.get(1).getAsFloat() : 0f;
        float z = rotation.size() == 4 ? rotation.get(2).getAsFloat() : 0f;
        float w = rotation.size() == 4 ? rotation.get(3).getAsFloat() : 1f;
        float sx = scale.size() == 3 ? scale.get(0).getAsFloat() : 1f;
        float sy = scale.size() == 3 ? scale.get(1).getAsFloat() : 1f;
        float sz = scale.size() == 3 ? scale.get(2).getAsFloat() : 1f;

        local[0] = (1f - 2f * (y * y + z * z)) * sx;
        local[1] = 2f * (x * y + z * w) * sx;
        local[2] = 2f * (x * z - y * w) * sx;
        local[4] = 2f * (x * y - z * w) * sy;
        local[5] = (1f - 2f * (x * x + z * z)) * sy;
        local[6] = 2f * (y * z + x * w) * sy;
        local[8] = 2f * (x * z + y * w) * sz;
        local[9] = 2f * (y * z - x * w) * sz;
        local[10] = (1f - 2f * (x * x + y * y)) * sz;
        local[15] = 1f;

        for (int a = 0; a < translation.size() && a < 3; a++)
            local[12 + a] = translation.get(a).getAsFloat();

        return local;
    }

    // Every material of the file, then one more that faces without a material fall to
    private static void readMaterials(GltfDocumentStruct document, GltfImportStruct model) {

        for (ArpgElementStruct element : GltfReadUtility.list(document.getJson(), "materials")) {

            ArpgObjectStruct material = element.getAsObject();
            ArpgObjectStruct extras = ArpgUtility.hasObject(material, "extras")
                    ? material.getAsObject("extras")
                    : new ArpgObjectStruct();

            model.materialNames.add(GltfReadUtility.text(material, "name", ""));
            model.materialTextures.add(GltfReadUtility.text(extras, "texture", null));
            model.materialZones.add(GltfReadUtility.text(extras, "zone", null));
        }

        model.materialNames.add("");
        model.materialTextures.add(null);
        model.materialZones.add(null);
    }

    private static void readMorphNames(
            ArpgObjectStruct mesh,
            ArpgArrayStruct primitives,
            GltfImportStruct model,
            GeometryStruct geometry) {

        ArpgArrayStruct names = ArpgUtility.hasObject(mesh, "extras")
                ? GltfReadUtility.list(mesh.getAsObject("extras"), "targetNames")
                : new ArpgArrayStruct();
        int targetCount = primitives.size() > 0
                ? GltfReadUtility.list(primitives.get(0).getAsObject(), "targets").size()
                : 0;

        for (int t = 0; t < targetCount; t++) {
            model.morphNames.add(t < names.size() ? names.get(t).getAsString() : EditorSetting.GLTF_MORPH_NAME_PREFIX + t);
            geometry.morphDeltas.add(new FloatArrayList());
        }
    }

    // Primitives \\

    private static void readPrimitive(
            GltfDocumentStruct document,
            ArpgObjectStruct primitive,
            GltfImportStruct model,
            GeometryStruct geometry) {

        if (GltfReadUtility.number(primitive, "mode", EditorSetting.GLTF_MODE_TRIANGLES)
                != EditorSetting.GLTF_MODE_TRIANGLES)
            throw new InternalException("The model is not made of triangles (lines, points and strips are not read)");

        ArpgObjectStruct attributes = ArpgUtility.validateObject(primitive, "attributes");

        if (!ArpgUtility.hasNumber(attributes, "POSITION"))
            throw new InternalException("A part of the model has no positions");

        float[] positions = GltfReadUtility.readFloats(document, attributes.get("POSITION").getAsInt());
        int base = geometry.positions.size() / 3;
        int vertexCount = positions.length / 3;
        ArpgArrayStruct targets = GltfReadUtility.list(primitive, "targets");

        if (targets.size() != geometry.morphDeltas.size())
            throw new InternalException("The parts of the model do not agree on their morph targets");

        geometry.positions.addElements(geometry.positions.size(), positions);
        geometry.hasUVs |= appendAttribute(document, attributes, "TEXCOORD_0", 2, vertexCount, geometry.uvs);
        geometry.hasMasks |= appendAttribute(
                document, attributes, EditorSetting.GLTF_ATTRIBUTE_MASKS, EngineSetting.RIGGED_MESH_MASK_FLOATS,
                vertexCount, geometry.masks);
        appendAttribute(
                document, attributes, "WEIGHTS_0", EngineSetting.MAX_BONE_INFLUENCES, vertexCount, geometry.weights);
        appendJoints(document, attributes, vertexCount, geometry.joints);

        if (model.skinned && !ArpgUtility.hasNumber(attributes, "JOINTS_0"))
            throw new InternalException("A part of the skinned model carries no bone weights");

        if (ArpgUtility.hasNumber(attributes, "JOINTS_1"))
            throw new InternalException("The model weighs vertices onto more than "
                    + EngineSetting.MAX_BONE_INFLUENCES + " bones each");

        for (int t = 0; t < targets.size(); t++)
            appendAttribute(document, targets.get(t).getAsObject(), "POSITION", 3, vertexCount,
                    geometry.morphDeltas.get(t));

        int[] indices = ArpgUtility.hasNumber(primitive, "indices")
                ? GltfReadUtility.readInts(document, primitive.get("indices").getAsInt())
                : countUp(vertexCount);
        int material = GltfReadUtility.number(primitive, "material", model.materialNames.size() - 1);
        boolean fans = ArpgUtility.hasObject(primitive, "extensions")
                && primitive.getAsObject("extensions").has(EditorSetting.GLTF_EXTENSION_NGON);

        if (indices.length % EngineSetting.TRIANGLE_VERTEX_COUNT != 0)
            throw new InternalException("A part of the model does not list whole triangles");

        for (int index : indices)
            if (index < 0 || index >= vertexCount)
                throw new InternalException("A triangle names a vertex the model does not hold");

        if (fans)
            joinFans(indices, base, material, geometry);
        else
            joinPairs(indices, positions, base, material, geometry);
    }

    // The attribute's floats added to the list, or zeros where the primitive has none; true when it has
    private static boolean appendAttribute(
            GltfDocumentStruct document,
            ArpgObjectStruct attributes,
            String name,
            int components,
            int vertexCount,
            FloatArrayList values) {

        if (!ArpgUtility.hasNumber(attributes, name)) {
            values.size(values.size() + vertexCount * components);
            return false;
        }

        float[] read = GltfReadUtility.readFloats(document, attributes.get(name).getAsInt());

        if (read.length != vertexCount * components)
            throw new InternalException("The model's " + name + " does not match its vertex count");

        values.addElements(values.size(), read);
        return true;
    }

    private static void appendJoints(
            GltfDocumentStruct document,
            ArpgObjectStruct attributes,
            int vertexCount,
            IntArrayList joints) {

        int count = vertexCount * EngineSetting.MAX_BONE_INFLUENCES;

        if (!ArpgUtility.hasNumber(attributes, "JOINTS_0")) {
            joints.size(joints.size() + count);
            return;
        }

        int[] read = GltfReadUtility.readInts(document, attributes.get("JOINTS_0").getAsInt());

        if (read.length != count)
            throw new InternalException("The model's JOINTS_0 does not match its vertex count");

        joints.addElements(joints.size(), read);
    }

    private static int[] countUp(int count) {

        int[] indices = new int[count];

        for (int i = 0; i < count; i++)
            indices[i] = i;

        return indices;
    }

    // Quads \\

    // FB_ngon_encoding: the triangles of one polygon follow each other and share their first vertex; one triangle is a
    // triangle face, two a quad
    private static void joinFans(int[] indices, int base, int material, GeometryStruct geometry) {

        int triangleCount = indices.length / EngineSetting.TRIANGLE_VERTEX_COUNT;
        int t = 0;

        while (t < triangleCount) {

            int first = indices[t * EngineSetting.TRIANGLE_VERTEX_COUNT];
            int end = t + 1;

            while (end < triangleCount && indices[end * EngineSetting.TRIANGLE_VERTEX_COUNT] == first)
                end++;

            int a = t * EngineSetting.TRIANGLE_VERTEX_COUNT;

            if (end - t == 1) {
                addTriangle(geometry, base, material, first, indices[a + 1], indices[a + 2]);
                t = end;
                continue;
            }

            if (end - t != 2)
                throw new InternalException("The model has a face of " + (end - t + 2)
                        + " corners; only triangles and quads are read. Split it in the tool it came from");

            if (indices[a + 2] != indices[a + 4])
                throw new InternalException("The model's faces are not fanned the FB_ngon_encoding way");

            addQuad(geometry, base, material, first, indices[a + 1], indices[a + 2], indices[a + 5]);
            t = end;
        }
    }

    // Without the extension: a triangle joins the one listed right after it when they share an edge, the two halves
    // of a quad in nearly every exporter's output however the quad is bent, and otherwise the next one in the list
    // that shares an edge with it and lies flat against it; one with no such neighbour stays a triangle face
    private static void joinPairs(int[] indices, float[] positions, int base, int material, GeometryStruct geometry) {

        int triangleCount = indices.length / EngineSetting.TRIANGLE_VERTEX_COUNT;
        boolean[] joined = new boolean[triangleCount];

        for (int t = 0; t < triangleCount; t++) {

            if (joined[t])
                continue;

            int partner = EngineSetting.INDEX_NOT_FOUND;
            float flatness = EditorSetting.GLTF_QUAD_NEIGHBOUR_COSINE;

            for (int other = t + 1; other < triangleCount && partner == EngineSetting.INDEX_NOT_FOUND; other++) {

                if (!joined[other] && joinTriangles(indices, positions, t, other, flatness, base, material, geometry))
                    partner = other;

                flatness = EditorSetting.GLTF_QUAD_FLAT_COSINE;
            }

            joined[t] = true;

            if (partner == EngineSetting.INDEX_NOT_FOUND) {

                int a = t * EngineSetting.TRIANGLE_VERTEX_COUNT;

                addTriangle(geometry, base, material, indices[a], indices[a + 1], indices[a + 2]);
                continue;
            }

            joined[partner] = true;
        }
    }

    // Adds the quad two triangles make when one's edge is the other's turned round and their normals agree
    private static boolean joinTriangles(
            int[] indices,
            float[] positions,
            int first,
            int second,
            float flatness,
            int base,
            int material,
            GeometryStruct geometry) {

        int a = first * EngineSetting.TRIANGLE_VERTEX_COUNT;
        int b = second * EngineSetting.TRIANGLE_VERTEX_COUNT;

        for (int i = 0; i < EngineSetting.TRIANGLE_VERTEX_COUNT; i++)
            for (int j = 0; j < EngineSetting.TRIANGLE_VERTEX_COUNT; j++) {

                int from = indices[a + i];
                int to = indices[a + (i + 1) % EngineSetting.TRIANGLE_VERTEX_COUNT];
                int apex = indices[a + (i + 2) % EngineSetting.TRIANGLE_VERTEX_COUNT];
                int far = indices[b + (j + 2) % EngineSetting.TRIANGLE_VERTEX_COUNT];

                if (!samePosition(positions, from, indices[b + (j + 1) % EngineSetting.TRIANGLE_VERTEX_COUNT])
                        || !samePosition(positions, to, indices[b + j])
                        || !liesFlat(positions, from, to, apex, far, flatness))
                    continue;

                addQuad(geometry, base, material, to, apex, from, far);
                return true;
            }

        return false;
    }

    private static boolean samePosition(float[] positions, int a, int b) {
        return a == b || (positions[a * 3] == positions[b * 3]
                && positions[a * 3 + 1] == positions[b * 3 + 1]
                && positions[a * 3 + 2] == positions[b * 3 + 2]);
    }

    // Whether triangle (from, to, apex) and its neighbour (to, from, far) face the same way, to the cosine given; a
    // triangle with no area agrees only with the neighbour listed right after it
    private static boolean liesFlat(float[] positions, int from, int to, int apex, int far, float flatness) {

        float[] first = new float[3];
        float[] second = new float[3];

        cross(positions, from, to, apex, first);
        cross(positions, to, from, far, second);

        float lengths = (float) Math.sqrt(
                (first[0] * first[0] + first[1] * first[1] + first[2] * first[2])
                        * (second[0] * second[0] + second[1] * second[1] + second[2] * second[2]));

        if (lengths <= 0f)
            return flatness == EditorSetting.GLTF_QUAD_NEIGHBOUR_COSINE;

        return (first[0] * second[0] + first[1] * second[1] + first[2] * second[2]) / lengths >= flatness;
    }

    private static void cross(float[] positions, int a, int b, int c, float[] out) {

        float ux = positions[b * 3] - positions[a * 3];
        float uy = positions[b * 3 + 1] - positions[a * 3 + 1];
        float uz = positions[b * 3 + 2] - positions[a * 3 + 2];
        float vx = positions[c * 3] - positions[a * 3];
        float vy = positions[c * 3 + 1] - positions[a * 3 + 1];
        float vz = positions[c * 3 + 2] - positions[a * 3 + 2];

        out[0] = uy * vz - uz * vy;
        out[1] = uz * vx - ux * vz;
        out[2] = ux * vy - uy * vx;
    }

    private static void addQuad(GeometryStruct geometry, int base, int material, int a, int b, int c, int d) {

        geometry.quads.add(base + a);
        geometry.quads.add(base + b);
        geometry.quads.add(base + c);
        geometry.quads.add(base + d);
        geometry.quadCorners.add(EngineSetting.QUAD_VERTEX_COUNT);
        geometry.quadMaterials.add(material);
    }

    private static void addTriangle(GeometryStruct geometry, int base, int material, int a, int b, int c) {

        geometry.quads.add(base + a);
        geometry.quads.add(base + b);
        geometry.quads.add(base + c);
        geometry.quads.add(base + c);
        geometry.quadCorners.add(EngineSetting.TRIANGLE_VERTEX_COUNT);
        geometry.quadMaterials.add(material);
    }

    // Build \\

    // The joints the model weighs onto that the rig has no bone for, by name
    public static ObjectArrayList<String> findMissingBones(GltfImportStruct model, RigHandle rigHandle) {

        ObjectArrayList<String> missing = new ObjectArrayList<>();
        boolean[] used = new boolean[model.jointNames.size()];

        for (int i = 0; i < model.joints.length; i++)
            if (model.skinned && model.weights[i] > 0f && model.joints[i] < used.length)
                used[model.joints[i]] = true;

        for (int j = 0; j < used.length; j++)
            if (used[j] && !rigHandle.hasBone(model.jointNames.get(j)))
                missing.add(model.jointNames.get(j));

        return missing;
    }

    // textures and zones name, material by material, what the user confirmed; unskinnedBone is the bone a model with
    // no skin hangs from whole
    public static RiggedMeshStruct build(
            GltfImportStruct model,
            RigHandle rigHandle,
            String[] textures,
            MeshZone[] zones,
            String unskinnedBone) {

        if (!findMissingBones(model, rigHandle).isEmpty())
            throw new InternalException("The rig has no bone named " + findMissingBones(model, rigHandle));

        if (!model.skinned && !rigHandle.hasBone(unskinnedBone))
            throw new InternalException("The model has no skin and no bone was chosen for it to hang from");

        RiggedMeshStruct mesh = new RiggedMeshStruct(rigHandle);
        int[] meshVertices = buildVertices(model, rigHandle, unskinnedBone, mesh);

        for (int q = 0; q < model.quadMaterials.length; q++) {

            int material = model.quadMaterials[q];
            int base = q * EngineSetting.QUAD_VERTEX_COUNT;

            if (textures[material] == null || zones[material] == null)
                throw new InternalException("Material \"" + model.materialNames.get(material)
                        + "\" has no texture and zone chosen for it");

            int corners = model.quadCorners[q];
            int face = corners == EngineSetting.TRIANGLE_VERTEX_COUNT
                    ? mesh.addTriangle(
                            meshVertices[model.quads[base]],
                            meshVertices[model.quads[base + 1]],
                            meshVertices[model.quads[base + 2]],
                            mesh.resolveTexture(textures[material]))
                    : mesh.addFace(
                            meshVertices[model.quads[base]],
                            meshVertices[model.quads[base + 1]],
                            meshVertices[model.quads[base + 2]],
                            meshVertices[model.quads[base + 3]],
                            mesh.resolveTexture(textures[material]));

            mesh.setFaceZone(face, zones[material]);

            for (int c = 0; model.uvs != null && c < corners; c++)
                mesh.setFaceUV(face, c, model.uvs[model.quads[base + c] * 2], 1f - model.uvs[model.quads[base + c] * 2 + 1]);
        }

        buildMorphs(model, meshVertices, mesh);

        return mesh;
    }

    // glTF vertices on one position with the same influences become one mesh vertex, in the order quads first use them
    private static int[] buildVertices(
            GltfImportStruct model,
            RigHandle rigHandle,
            String unskinnedBone,
            RiggedMeshStruct mesh) {

        Object2IntOpenHashMap<String> key2Vertex = new Object2IntOpenHashMap<>();
        StringBuilder key = new StringBuilder();
        int[] meshVertices = new int[model.positions.length / 3];

        key2Vertex.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
        Arrays.fill(meshVertices, EngineSetting.INDEX_NOT_FOUND);

        for (int vertex : model.quads) {

            if (meshVertices[vertex] != EngineSetting.INDEX_NOT_FOUND)
                continue;

            int vertexIndex = mesh.addVertex(
                    model.positions[vertex * 3], model.positions[vertex * 3 + 1], model.positions[vertex * 3 + 2]);

            if (model.skinned)
                addInfluences(model, rigHandle, vertex, vertexIndex, mesh);
            else
                mesh.addInfluence(vertexIndex, rigHandle.getBoneIndex(unskinnedBone), 1f);

            key.setLength(0);
            key.append(Float.floatToIntBits(mesh.getX(vertexIndex) + 0f)).append(',')
                    .append(Float.floatToIntBits(mesh.getY(vertexIndex) + 0f)).append(',')
                    .append(Float.floatToIntBits(mesh.getZ(vertexIndex) + 0f));

            for (int i = 0; i < mesh.getInfluenceCount(vertexIndex); i++)
                key.append('|').append(mesh.getInfluenceBone(vertexIndex, i)).append(':')
                        .append(Float.floatToIntBits(mesh.getInfluenceWeight(vertexIndex, i)));

            String vertexKey = key.toString();
            int existing = key2Vertex.getInt(vertexKey);

            if (existing != EngineSetting.INDEX_NOT_FOUND) {
                mesh.removeLastVertex();
                meshVertices[vertex] = existing;
                continue;
            }

            key2Vertex.put(vertexKey, vertexIndex);
            meshVertices[vertex] = vertexIndex;

            for (int m = 0; model.masks != null && m < EngineSetting.RIGGED_MESH_MASK_FLOATS; m++)
                mesh.setMask(vertexIndex, m, model.masks[vertex * EngineSetting.RIGGED_MESH_MASK_FLOATS + m]);
        }

        return meshVertices;
    }

    // The vertex's weighted joints as the rig's bones; weights that do not sum to one are scaled until they do
    private static void addInfluences(
            GltfImportStruct model,
            RigHandle rigHandle,
            int vertex,
            int vertexIndex,
            RiggedMeshStruct mesh) {

        int base = vertex * EngineSetting.MAX_BONE_INFLUENCES;
        float sum = 0f;

        for (int i = 0; i < EngineSetting.MAX_BONE_INFLUENCES; i++)
            sum += Math.max(0f, model.weights[base + i]);

        if (sum <= 0f)
            throw new InternalException("A vertex of the skinned model is weighed onto no bone");

        float scale = Math.abs(sum - 1f) > EngineSetting.BONE_WEIGHT_SUM_EPSILON ? 1f / sum : 1f;

        for (int i = 0; i < EngineSetting.MAX_BONE_INFLUENCES; i++)
            if (model.weights[base + i] > 0f)
                mesh.addInfluence(
                        vertexIndex,
                        rigHandle.getBoneIndex(model.jointNames.get(model.joints[base + i])),
                        model.weights[base + i] * scale);
    }

    private static void buildMorphs(GltfImportStruct model, int[] meshVertices, RiggedMeshStruct mesh) {

        for (int m = 0; m < model.morphDeltas.length; m++) {

            float[] deltas = model.morphDeltas[m];
            int morphIndex = mesh.resolveMorph(model.morphNames.get(m));

            for (int vertex = 0; vertex < meshVertices.length; vertex++) {

                int vertexIndex = meshVertices[vertex];
                boolean moves = Math.abs(deltas[vertex * 3]) > EngineSetting.RIGGED_MESH_MORPH_DELTA_EPSILON
                        || Math.abs(deltas[vertex * 3 + 1]) > EngineSetting.RIGGED_MESH_MORPH_DELTA_EPSILON
                        || Math.abs(deltas[vertex * 3 + 2]) > EngineSetting.RIGGED_MESH_MORPH_DELTA_EPSILON;

                if (vertexIndex == EngineSetting.INDEX_NOT_FOUND || !moves
                        || mesh.findMorphEntry(morphIndex, vertexIndex) != EngineSetting.INDEX_NOT_FOUND)
                    continue;

                mesh.setMorphDelta(
                        morphIndex, vertexIndex, deltas[vertex * 3], deltas[vertex * 3 + 1], deltas[vertex * 3 + 2]);
            }
        }
    }

    private static final class GeometryStruct extends StructPackage {

        /*
         * The lists one inspection fills as it walks the primitives: every
         * attribute per glTF vertex, zeros where a primitive lacks one, and
         * the quads rebuilt so far.
         */

        // Vertices
        final FloatArrayList positions = new FloatArrayList();
        final FloatArrayList uvs = new FloatArrayList();
        final FloatArrayList weights = new FloatArrayList();
        final FloatArrayList masks = new FloatArrayList();
        final IntArrayList joints = new IntArrayList();
        final ObjectArrayList<FloatArrayList> morphDeltas = new ObjectArrayList<>();
        boolean hasUVs;
        boolean hasMasks;

        // Faces — four vertex slots, a corner count and a material each
        final IntArrayList quads = new IntArrayList();
        final IntArrayList quadCorners = new IntArrayList();
        final IntArrayList quadMaterials = new IntArrayList();
    }
}
