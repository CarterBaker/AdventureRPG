package editor.bootstrap.charactereditorpipeline.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.root.StructPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.matrices.Matrix4;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class GltfExportUtility extends EngineUtility {

    /*
     * Writes a rigged mesh, its rig and its morphs as one .glb file (see
     * .claude/docs/GLTF.md). Corners that share a mesh vertex, a UV and a
     * normal become one glTF vertex; every texture and zone pair is a
     * material naming both in its extras and a primitive of its own; a quad
     * is two triangles fanned from one corner, the FB_ngon_encoding way, so
     * an importer that knows it gets the quads back; the rig is a skin whose
     * joints are its bones in order; morphs are targets named in the mesh's
     * extras; masks ride as the _MASKS attribute. The same mesh always
     * writes the same bytes.
     */

    // Export \\

    // normals holds every corner's packed smooth normal, face by face, as the vertex buffer holds it
    public static byte[] export(RiggedMeshStruct mesh, RigHandle rigHandle, String meshName, int[] normals) {

        ExportStruct export = new ExportStruct();

        shareVertices(mesh, normals, export);
        writeVertexAccessors(mesh, rigHandle, export);
        writeMorphAccessors(mesh, export);
        writePrimitives(mesh, export);

        ArpgObjectStruct json = new ArpgObjectStruct();
        ArpgObjectStruct asset = new ArpgObjectStruct();
        ArpgArrayStruct extensions = new ArpgArrayStruct();
        ArpgArrayStruct buffers = new ArpgArrayStruct();
        ArpgObjectStruct buffer = new ArpgObjectStruct();

        asset.addProperty("version", EditorSetting.GLTF_ASSET_VERSION);
        asset.addProperty("generator", EditorSetting.GLTF_GENERATOR);
        extensions.add(EditorSetting.GLTF_EXTENSION_NGON);
        json.add("asset", asset);
        json.add("extensionsUsed", extensions);
        json.addProperty("scene", 0L);
        writeNodes(rigHandle, meshName, export, json);
        writeMesh(mesh, meshName, export, json);
        json.add("accessors", export.accessors);
        json.add("bufferViews", export.views);
        buffer.addProperty("byteLength", (long) export.binary.position());
        buffers.add(buffer);
        json.add("buffers", buffers);

        return writeFile(json, export.binary);
    }

    // Vertices \\

    private static void shareVertices(RiggedMeshStruct mesh, int[] normals, ExportStruct export) {

        Object2IntOpenHashMap<String> key2Vertex = new Object2IntOpenHashMap<>();
        StringBuilder key = new StringBuilder();
        int faceCount = mesh.getFaceCount();

        key2Vertex.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
        export.quads = new int[faceCount * EngineSetting.QUAD_VERTEX_COUNT];

        for (int f = 0; f < faceCount; f++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int corner = f * EngineSetting.QUAD_VERTEX_COUNT + c;
                float u = mesh.getFaceU(f, c);
                float v = mesh.getFaceV(f, c);

                key.setLength(0);
                key.append(mesh.getFaceVertex(f, c)).append(',').append(Float.floatToIntBits(u)).append(',')
                        .append(Float.floatToIntBits(v)).append(',').append(normals[corner]);

                String vertexKey = key.toString();
                int vertex = key2Vertex.getInt(vertexKey);

                if (vertex == EngineSetting.INDEX_NOT_FOUND) {

                    vertex = key2Vertex.size();
                    key2Vertex.put(vertexKey, vertex);
                    export.meshVertices.add(mesh.getFaceVertex(f, c));
                    export.uvs.add(u);
                    export.uvs.add(1f - v);
                    export.normals.add(normals[corner]);
                }

                export.quads[corner] = vertex;
            }
    }

    private static void writeVertexAccessors(RiggedMeshStruct mesh, RigHandle rigHandle, ExportStruct export) {

        int vertexCount = export.meshVertices.size();
        float[] positions = new float[vertexCount * 3];
        float[] normals = new float[vertexCount * 3];
        float[] weights = new float[vertexCount * EngineSetting.MAX_BONE_INFLUENCES];
        float[] masks = new float[vertexCount * EngineSetting.RIGGED_MESH_MASK_FLOATS];
        int[] joints = new int[vertexCount * EngineSetting.MAX_BONE_INFLUENCES];
        float[] inverseBinds = new float[rigHandle.getBoneCount() * EditorSetting.GLTF_MATRIX_FLOATS];

        for (int i = 0; i < vertexCount; i++) {

            int vertexIndex = export.meshVertices.getInt(i);

            positions[i * 3] = mesh.getX(vertexIndex);
            positions[i * 3 + 1] = mesh.getY(vertexIndex);
            positions[i * 3 + 2] = mesh.getZ(vertexIndex);
            unpackNormal(export.normals.getInt(i), normals, i * 3);

            for (int n = 0; n < mesh.getInfluenceCount(vertexIndex); n++) {
                joints[i * EngineSetting.MAX_BONE_INFLUENCES + n] = mesh.getInfluenceBone(vertexIndex, n);
                weights[i * EngineSetting.MAX_BONE_INFLUENCES + n] = mesh.getInfluenceWeight(vertexIndex, n);
            }

            for (int m = 0; m < EngineSetting.RIGGED_MESH_MASK_FLOATS; m++)
                masks[i * EngineSetting.RIGGED_MESH_MASK_FLOATS + m] = mesh.getMask(vertexIndex, m);
        }

        for (int b = 0; b < rigHandle.getBoneCount(); b++)
            System.arraycopy(
                    rigHandle.getBindWorldInverseMatrix(b).val, 0,
                    inverseBinds, b * EditorSetting.GLTF_MATRIX_FLOATS, EditorSetting.GLTF_MATRIX_FLOATS);

        export.positionAccessor = addFloats(export, positions, EditorSetting.GLTF_TYPE_VEC3, true, true);
        export.normalAccessor = addFloats(export, normals, EditorSetting.GLTF_TYPE_VEC3, true, false);
        export.uvAccessor = addFloats(export, export.uvs.toFloatArray(), EditorSetting.GLTF_TYPE_VEC2, true, false);
        export.jointAccessor = addWholes(
                export, joints, EditorSetting.GLTF_TYPE_VEC4, EditorSetting.GLTF_COMPONENT_UNSIGNED_SHORT,
                EditorSetting.GLTF_TARGET_ARRAY_BUFFER);
        export.weightAccessor = addFloats(export, weights, EditorSetting.GLTF_TYPE_VEC4, true, false);
        export.maskAccessor = addFloats(export, masks, EditorSetting.GLTF_TYPE_VEC4, true, false);
        export.inverseBindAccessor = addFloats(export, inverseBinds, EditorSetting.GLTF_TYPE_MAT4, false, false);
    }

    // A packed normal as three floats of unit length; one with no direction points up
    private static void unpackNormal(int packed, float[] normals, int base) {

        float length = 0f;

        for (int a = 0; a < 3; a++) {

            int bits = packed >> (a * EngineSetting.RIGGED_MESH_NORMAL_BITS) & EngineSetting.RIGGED_MESH_NORMAL_MASK;
            int shift = Integer.SIZE - EngineSetting.RIGGED_MESH_NORMAL_BITS;

            normals[base + a] = (bits << shift >> shift) / (float) EngineSetting.RIGGED_MESH_NORMAL_SCALE;
            length += normals[base + a] * normals[base + a];
        }

        length = (float) Math.sqrt(length);

        for (int a = 0; a < 3; a++)
            normals[base + a] = length > 0f ? normals[base + a] / length : a == 1 ? 1f : 0f;
    }

    // One dense position accessor per morph, a delta for every glTF vertex
    private static void writeMorphAccessors(RiggedMeshStruct mesh, ExportStruct export) {

        int vertexCount = export.meshVertices.size();
        float[] deltas = new float[vertexCount * 3];

        export.morphAccessors = new int[mesh.getMorphCount()];

        for (int m = 0; m < mesh.getMorphCount(); m++) {

            Arrays.fill(deltas, 0f);

            for (int i = 0; i < vertexCount; i++) {

                int entry = mesh.findMorphEntry(m, export.meshVertices.getInt(i));

                if (entry == EngineSetting.INDEX_NOT_FOUND)
                    continue;

                for (int a = 0; a < 3; a++)
                    deltas[i * 3 + a] = mesh.getMorphDelta(m, entry, a);
            }

            export.morphAccessors[m] = addFloats(export, deltas, EditorSetting.GLTF_TYPE_VEC3, true, true);
        }
    }

    // Primitives \\

    // Every texture and zone pair in the order faces first use it, each with the triangles of its quads. A quad fans
    // from its first corner, or from the opposite one when the quad before it fanned from the same vertex, which
    // keeps the same two triangles and keeps the two quads apart for an importer
    private static void writePrimitives(RiggedMeshStruct mesh, ExportStruct export) {

        Object2IntOpenHashMap<String> key2Material = new Object2IntOpenHashMap<>();
        ObjectArrayList<IntArrayList> triangles = new ObjectArrayList<>();
        IntArrayList fans = new IntArrayList();

        key2Material.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);

        for (int f = 0; f < mesh.getFaceCount(); f++) {

            String texture = mesh.getTextureName(mesh.getFaceTexture(f));
            String zone = ArpgUtility.toEnumName(mesh.getFaceZone(f));
            String key = texture + '\n' + zone;
            int material = key2Material.getInt(key);

            if (material == EngineSetting.INDEX_NOT_FOUND) {

                ArpgObjectStruct materialArpg = new ArpgObjectStruct();
                ArpgObjectStruct extras = new ArpgObjectStruct();

                material = key2Material.size();
                key2Material.put(key, material);
                triangles.add(new IntArrayList());
                fans.add(EngineSetting.INDEX_NOT_FOUND);
                extras.addProperty("texture", texture);
                extras.addProperty("zone", zone);
                materialArpg.addProperty("name", texture + " (" + zone + ")");
                materialArpg.add("extras", extras);
                export.materials.add(materialArpg);
            }

            int base = f * EngineSetting.QUAD_VERTEX_COUNT;
            int start = export.quads[base] == fans.getInt(material) ? 2 : 0;
            IntArrayList indices = triangles.get(material);

            for (int i = 0; i < EngineSetting.QUAD_INDEX_COUNT; i++)
                indices.add(export.quads[base + (start + EditorSetting.GLTF_QUAD_FAN_CORNERS[i])
                        % EngineSetting.QUAD_VERTEX_COUNT]);

            fans.set(material, export.quads[base + start]);
        }

        export.indexAccessors = new int[triangles.size()];

        for (int m = 0; m < triangles.size(); m++)
            export.indexAccessors[m] = addWholes(
                    export, triangles.get(m).toIntArray(), EditorSetting.GLTF_TYPE_SCALAR,
                    EditorSetting.GLTF_COMPONENT_UNSIGNED_INT, EditorSetting.GLTF_TARGET_ELEMENT_ARRAY_BUFFER);
    }

    // Document \\

    // The rig's bones as nodes in bone order, each placed against its parent by its bind matrices, then the mesh
    private static void writeNodes(RigHandle rigHandle, String meshName, ExportStruct export, ArpgObjectStruct json) {

        int boneCount = rigHandle.getBoneCount();
        ArpgArrayStruct nodes = new ArpgArrayStruct();
        ArpgArrayStruct roots = new ArpgArrayStruct();
        ArpgArrayStruct joints = new ArpgArrayStruct();
        ArpgArrayStruct scenes = new ArpgArrayStruct();
        ArpgArrayStruct skins = new ArpgArrayStruct();
        ArpgObjectStruct scene = new ArpgObjectStruct();
        ArpgObjectStruct skin = new ArpgObjectStruct();
        ArpgObjectStruct meshNode = new ArpgObjectStruct();
        float[] local = new float[EditorSetting.GLTF_MATRIX_FLOATS];

        for (int b = 0; b < boneCount; b++) {

            ArpgObjectStruct node = new ArpgObjectStruct();
            ArpgArrayStruct matrix = new ArpgArrayStruct();
            ArpgArrayStruct children = new ArpgArrayStruct();
            int parent = rigHandle.getBone(b).getParentIndex();

            resolveLocal(rigHandle, b, parent, local);

            for (float value : local)
                matrix.add(value);

            for (int child = 0; child < boneCount; child++)
                if (rigHandle.getBone(child).getParentIndex() == b)
                    children.add((long) child);

            node.addProperty("name", rigHandle.getBone(b).getName());
            node.add("matrix", matrix);

            if (children.size() > 0)
                node.add("children", children);

            if (parent == EngineSetting.INDEX_NOT_FOUND)
                roots.add((long) b);

            nodes.add(node);
            joints.add((long) b);
        }

        meshNode.addProperty("name", meshName);
        meshNode.addProperty("mesh", 0L);
        meshNode.addProperty("skin", 0L);
        nodes.add(meshNode);
        roots.add((long) boneCount);
        scene.add("nodes", roots);
        scenes.add(scene);
        skin.add("joints", joints);
        skin.addProperty("inverseBindMatrices", (long) export.inverseBindAccessor);
        skins.add(skin);
        json.add("scenes", scenes);
        json.add("nodes", nodes);
        json.add("skins", skins);
    }

    // A bone's bind matrix against its parent's: the parent's inverse bind times its own bind, column by column
    private static void resolveLocal(RigHandle rigHandle, int bone, int parent, float[] local) {

        float[] world = rigHandle.getBindWorldMatrix(bone).val;

        if (parent == EngineSetting.INDEX_NOT_FOUND) {
            System.arraycopy(world, 0, local, 0, local.length);
            return;
        }

        float[] inverse = rigHandle.getBindWorldInverseMatrix(parent).val;

        for (int column = 0; column < 4; column++)
            for (int row = 0; row < 4; row++) {

                float sum = 0f;

                for (int k = 0; k < 4; k++)
                    sum += inverse[k * 4 + row] * world[column * 4 + k];

                local[column * 4 + row] = sum;
            }
    }

    private static void writeMesh(RiggedMeshStruct mesh, String meshName, ExportStruct export, ArpgObjectStruct json) {

        ArpgArrayStruct meshes = new ArpgArrayStruct();
        ArpgArrayStruct primitives = new ArpgArrayStruct();
        ArpgArrayStruct targetNames = new ArpgArrayStruct();
        ArpgObjectStruct meshArpg = new ArpgObjectStruct();
        ArpgObjectStruct extras = new ArpgObjectStruct();

        for (int m = 0; m < export.indexAccessors.length; m++) {

            ArpgObjectStruct primitive = new ArpgObjectStruct();
            ArpgObjectStruct attributes = new ArpgObjectStruct();
            ArpgObjectStruct extensions = new ArpgObjectStruct();
            ArpgArrayStruct targets = new ArpgArrayStruct();

            attributes.addProperty("POSITION", (long) export.positionAccessor);
            attributes.addProperty("NORMAL", (long) export.normalAccessor);
            attributes.addProperty("TEXCOORD_0", (long) export.uvAccessor);
            attributes.addProperty("JOINTS_0", (long) export.jointAccessor);
            attributes.addProperty("WEIGHTS_0", (long) export.weightAccessor);
            attributes.addProperty(EditorSetting.GLTF_ATTRIBUTE_MASKS, (long) export.maskAccessor);

            for (int accessor : export.morphAccessors) {
                ArpgObjectStruct target = new ArpgObjectStruct();
                target.addProperty("POSITION", (long) accessor);
                targets.add(target);
            }

            extensions.add(EditorSetting.GLTF_EXTENSION_NGON, new ArpgObjectStruct());
            primitive.add("attributes", attributes);
            primitive.addProperty("indices", (long) export.indexAccessors[m]);
            primitive.addProperty("material", (long) m);
            primitive.addProperty("mode", (long) EditorSetting.GLTF_MODE_TRIANGLES);

            if (targets.size() > 0)
                primitive.add("targets", targets);

            primitive.add("extensions", extensions);
            primitives.add(primitive);
        }

        for (int m = 0; m < mesh.getMorphCount(); m++)
            targetNames.add(mesh.getMorphName(m));

        extras.add("targetNames", targetNames);
        meshArpg.addProperty("name", meshName);
        meshArpg.add("primitives", primitives);
        meshArpg.add("extras", extras);
        meshes.add(meshArpg);
        json.add("meshes", meshes);
        json.add("materials", export.materials);
    }

    // Binary \\

    private static int addFloats(ExportStruct export, float[] values, String type, boolean vertexData, boolean bounds) {

        int components = GltfReadUtility.componentCount(type);
        int offset = export.reserve(values.length * Float.BYTES);

        for (int i = 0; i < values.length; i++)
            export.binary.putFloat(offset + i * Float.BYTES, values[i]);

        ArpgObjectStruct accessor = addAccessor(
                export, offset, values.length * Float.BYTES, values.length / components, type,
                EditorSetting.GLTF_COMPONENT_FLOAT,
                vertexData ? EditorSetting.GLTF_TARGET_ARRAY_BUFFER : EngineSetting.INDEX_NOT_FOUND);

        if (bounds)
            addBounds(accessor, values, components);

        return export.accessors.size() - 1;
    }

    private static int addWholes(ExportStruct export, int[] values, String type, int componentType, int target) {

        boolean wide = componentType == EditorSetting.GLTF_COMPONENT_UNSIGNED_INT;
        int size = wide ? Integer.BYTES : Short.BYTES;
        int offset = export.reserve(values.length * size);

        for (int i = 0; i < values.length; i++)
            if (wide)
                export.binary.putInt(offset + i * size, values[i]);
            else
                export.binary.putShort(offset + i * size, (short) values[i]);

        addAccessor(
                export, offset, values.length * size, values.length / GltfReadUtility.componentCount(type), type,
                componentType, target);

        return export.accessors.size() - 1;
    }

    private static ArpgObjectStruct addAccessor(
            ExportStruct export,
            int offset,
            int length,
            int count,
            String type,
            int componentType,
            int target) {

        ArpgObjectStruct view = new ArpgObjectStruct();
        ArpgObjectStruct accessor = new ArpgObjectStruct();

        view.addProperty("buffer", 0L);
        view.addProperty("byteOffset", (long) offset);
        view.addProperty("byteLength", (long) length);

        if (target != EngineSetting.INDEX_NOT_FOUND)
            view.addProperty("target", (long) target);

        accessor.addProperty("bufferView", (long) export.views.size());
        accessor.addProperty("componentType", (long) componentType);
        accessor.addProperty("count", (long) count);
        accessor.addProperty("type", type);
        export.views.add(view);
        export.accessors.add(accessor);

        return accessor;
    }

    private static void addBounds(ArpgObjectStruct accessor, float[] values, int components) {

        ArpgArrayStruct minimum = new ArpgArrayStruct();
        ArpgArrayStruct maximum = new ArpgArrayStruct();

        for (int c = 0; c < components; c++) {

            float low = values.length > 0 ? Float.MAX_VALUE : 0f;
            float high = values.length > 0 ? -Float.MAX_VALUE : 0f;

            for (int i = c; i < values.length; i += components) {
                low = Math.min(low, values[i]);
                high = Math.max(high, values[i]);
            }

            minimum.add(low);
            maximum.add(high);
        }

        accessor.add("min", minimum);
        accessor.add("max", maximum);
    }

    // The header, the JSON chunk padded with spaces and the binary chunk padded with zeros
    private static byte[] writeFile(ArpgObjectStruct json, ByteBuffer binary) {

        byte[] text = ArpgUtility.formatCompactText(json).getBytes(StandardCharsets.UTF_8);
        int textLength = align(text.length);
        int binaryLength = align(binary.position());
        int total = EditorSetting.GLTF_HEADER_BYTES + EditorSetting.GLTF_CHUNK_HEADER_BYTES * 2 + textLength
                + binaryLength;
        ByteBuffer file = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);

        file.putInt(EditorSetting.GLTF_MAGIC).putInt(EditorSetting.GLTF_VERSION).putInt(total);
        file.putInt(textLength).putInt(EditorSetting.GLTF_CHUNK_JSON).put(text);

        for (int i = text.length; i < textLength; i++)
            file.put((byte) ' ');

        file.putInt(binaryLength).putInt(EditorSetting.GLTF_CHUNK_BIN).put(binary.array(), 0, binary.position());

        return file.array();
    }

    private static int align(int length) {
        return (length + EditorSetting.GLTF_ALIGNMENT - 1) / EditorSetting.GLTF_ALIGNMENT * EditorSetting.GLTF_ALIGNMENT;
    }

    private static final class ExportStruct extends StructPackage {

        /*
         * What one export gathers: the glTF vertices its corners share, the
         * growing binary chunk, and the accessors, views and materials of
         * the document.
         */

        // Vertices
        final IntArrayList meshVertices = new IntArrayList();
        final FloatArrayList uvs = new FloatArrayList();
        final IntArrayList normals = new IntArrayList();
        int[] quads;

        // Document
        final ArpgArrayStruct accessors = new ArpgArrayStruct();
        final ArpgArrayStruct views = new ArpgArrayStruct();
        final ArpgArrayStruct materials = new ArpgArrayStruct();
        ByteBuffer binary = ByteBuffer.allocate(EngineSetting.ARPG_BUFFER_INITIAL_CAPACITY)
                .order(ByteOrder.LITTLE_ENDIAN);

        // Accessors
        int positionAccessor;
        int normalAccessor;
        int uvAccessor;
        int jointAccessor;
        int weightAccessor;
        int maskAccessor;
        int inverseBindAccessor;
        int[] morphAccessors;
        int[] indexAccessors;

        // The offset of a new block of the binary chunk, every block starting on a 4-byte boundary
        int reserve(int length) {

            int offset = align(binary.position());
            int end = offset + length;

            if (end > binary.capacity()) {

                ByteBuffer grown = ByteBuffer.allocate(Math.max(binary.capacity() * 2, end))
                        .order(ByteOrder.LITTLE_ENDIAN);

                grown.put(binary.array(), 0, binary.position());
                this.binary = grown;
            }

            binary.position(end);
            return offset;
        }
    }
}
