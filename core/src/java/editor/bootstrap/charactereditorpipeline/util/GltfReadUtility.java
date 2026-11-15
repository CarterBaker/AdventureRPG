package editor.bootstrap.charactereditorpipeline.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import editor.runtime.EditorSetting;
import engine.root.EngineUtility;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

public class GltfReadUtility extends EngineUtility {

    /*
     * Opens a .glb file and reads its accessors (see .claude/docs/GLTF.md).
     * The JSON chunk is parsed through ArpgUtility.parseText and the binary
     * chunk is read in place. An accessor comes back as floats, integer
     * component types divided down when it is normalised, or as whole
     * numbers, through its buffer view's offset and stride with any sparse
     * values laid over it. Only the binary chunk is a buffer: a file that
     * points at outside files or data URIs is refused, and every refusal is a
     * catchable InternalException saying why.
     */

    // Open \\

    public static GltfDocumentStruct read(byte[] file) {

        ByteBuffer bytes = ByteBuffer.wrap(file).order(ByteOrder.LITTLE_ENDIAN);

        if (file.length < EditorSetting.GLTF_HEADER_BYTES || bytes.getInt(0) != EditorSetting.GLTF_MAGIC)
            throw new InternalException("Not a .glb file");

        if (bytes.getInt(Integer.BYTES) != EditorSetting.GLTF_VERSION)
            throw new InternalException("Only glTF 2.0 files are read");

        ArpgObjectStruct json = null;
        ByteBuffer binary = ByteBuffer.allocate(0).order(ByteOrder.LITTLE_ENDIAN);
        int position = EditorSetting.GLTF_HEADER_BYTES;

        while (position + EditorSetting.GLTF_CHUNK_HEADER_BYTES <= file.length) {

            int length = bytes.getInt(position);
            int type = bytes.getInt(position + Integer.BYTES);
            int start = position + EditorSetting.GLTF_CHUNK_HEADER_BYTES;

            if (length < 0 || start + length > file.length)
                throw new InternalException("The file ends inside one of its chunks");

            if (type == EditorSetting.GLTF_CHUNK_JSON)
                json = parseJson(new String(file, start, length, StandardCharsets.UTF_8));

            if (type == EditorSetting.GLTF_CHUNK_BIN)
                binary = ByteBuffer.wrap(file, start, length).slice().order(ByteOrder.LITTLE_ENDIAN);

            position = start + length;
        }

        if (json == null)
            throw new InternalException("The file has no JSON chunk");

        return new GltfDocumentStruct(json, binary);
    }

    private static ArpgObjectStruct parseJson(String text) {

        ArpgElementStruct root = ArpgUtility.parseText(text);

        if (!root.isObject())
            throw new InternalException("The file's JSON is not an object");

        return root.getAsObject();
    }

    // Tree \\

    static ArpgArrayStruct list(ArpgObjectStruct object, String key) {
        return ArpgUtility.hasArray(object, key) ? object.getAsArray(key) : new ArpgArrayStruct();
    }

    static ArpgObjectStruct entry(GltfDocumentStruct document, String key, int index) {

        ArpgArrayStruct entries = list(document.getJson(), key);

        if (index < 0 || index >= entries.size() || !entries.get(index).isObject())
            throw new InternalException("The file names entry " + index + " of '" + key + "', which it does not hold");

        return entries.get(index).getAsObject();
    }

    static int number(ArpgObjectStruct object, String key, int defaultValue) {
        return ArpgUtility.hasNumber(object, key) ? object.get(key).getAsInt() : defaultValue;
    }

    static String text(ArpgObjectStruct object, String key, String defaultValue) {
        return ArpgUtility.hasString(object, key) ? object.get(key).getAsString() : defaultValue;
    }

    // Accessors \\

    static int componentCount(String type) {

        return switch (type) {
            case EditorSetting.GLTF_TYPE_SCALAR -> 1;
            case EditorSetting.GLTF_TYPE_VEC2 -> 2;
            case EditorSetting.GLTF_TYPE_VEC3 -> 3;
            case EditorSetting.GLTF_TYPE_VEC4 -> 4;
            case EditorSetting.GLTF_TYPE_MAT4 -> EditorSetting.GLTF_MATRIX_FLOATS;
            default -> throw new InternalException("Accessor type " + type + " is not read");
        };
    }

    private static int componentBytes(int componentType) {

        return switch (componentType) {
            case EditorSetting.GLTF_COMPONENT_BYTE, EditorSetting.GLTF_COMPONENT_UNSIGNED_BYTE -> Byte.BYTES;
            case EditorSetting.GLTF_COMPONENT_SHORT, EditorSetting.GLTF_COMPONENT_UNSIGNED_SHORT -> Short.BYTES;
            case EditorSetting.GLTF_COMPONENT_UNSIGNED_INT, EditorSetting.GLTF_COMPONENT_FLOAT -> Integer.BYTES;
            default -> throw new InternalException("Component type " + componentType + " is not read");
        };
    }

    // An accessor's values as floats, components back to back
    static float[] readFloats(GltfDocumentStruct document, int accessorIndex) {

        ArpgObjectStruct accessor = entry(document, "accessors", accessorIndex);
        int componentType = number(accessor, "componentType", EditorSetting.GLTF_COMPONENT_FLOAT);
        boolean normalized = ArpgUtility.getBoolean(accessor, "normalized", false);
        double[] values = readValues(document, accessor);
        float[] floats = new float[values.length];
        double scale = normalized ? normalScale(componentType) : 1.0;

        // A normalised signed value never falls below minus one
        for (int i = 0; i < values.length; i++)
            floats[i] = (float) (normalized ? Math.max(-1.0, values[i] / scale) : values[i]);

        return floats;
    }

    // An accessor's values as whole numbers, components back to back
    static int[] readInts(GltfDocumentStruct document, int accessorIndex) {

        ArpgObjectStruct accessor = entry(document, "accessors", accessorIndex);
        double[] values = readValues(document, accessor);
        int[] wholes = new int[values.length];

        for (int i = 0; i < values.length; i++)
            wholes[i] = (int) values[i];

        return wholes;
    }

    private static double normalScale(int componentType) {

        return switch (componentType) {
            case EditorSetting.GLTF_COMPONENT_BYTE -> Byte.MAX_VALUE;
            case EditorSetting.GLTF_COMPONENT_UNSIGNED_BYTE -> (1 << Byte.SIZE) - 1;
            case EditorSetting.GLTF_COMPONENT_SHORT -> Short.MAX_VALUE;
            case EditorSetting.GLTF_COMPONENT_UNSIGNED_SHORT -> (1 << Short.SIZE) - 1;
            default -> 1.0;
        };
    }

    private static double[] readValues(GltfDocumentStruct document, ArpgObjectStruct accessor) {

        int count = number(accessor, "count", 0);
        int components = componentCount(text(accessor, "type", EditorSetting.GLTF_TYPE_SCALAR));
        int componentType = number(accessor, "componentType", EditorSetting.GLTF_COMPONENT_FLOAT);
        double[] values = new double[count * components];

        if (ArpgUtility.hasNumber(accessor, "bufferView"))
            readView(
                    document, accessor.get("bufferView").getAsInt(), number(accessor, "byteOffset", 0),
                    componentType, components, count, values);

        if (ArpgUtility.hasObject(accessor, "sparse"))
            readSparse(document, accessor.getAsObject("sparse"), componentType, components, values);

        return values;
    }

    private static void readSparse(
            GltfDocumentStruct document,
            ArpgObjectStruct sparse,
            int componentType,
            int components,
            double[] values) {

        int count = number(sparse, "count", 0);
        ArpgObjectStruct indexArpg = ArpgUtility.validateObject(sparse, "indices");
        ArpgObjectStruct valueArpg = ArpgUtility.validateObject(sparse, "values");
        double[] indices = new double[count];
        double[] replaced = new double[count * components];

        readView(
                document, number(indexArpg, "bufferView", 0), number(indexArpg, "byteOffset", 0),
                number(indexArpg, "componentType", EditorSetting.GLTF_COMPONENT_UNSIGNED_INT), 1, count, indices);
        readView(
                document, number(valueArpg, "bufferView", 0), number(valueArpg, "byteOffset", 0),
                componentType, components, count, replaced);

        for (int i = 0; i < count; i++)
            System.arraycopy(replaced, i * components, values, (int) indices[i] * components, components);
    }

    private static void readView(
            GltfDocumentStruct document,
            int viewIndex,
            int accessorOffset,
            int componentType,
            int components,
            int count,
            double[] values) {

        ArpgObjectStruct view = entry(document, "bufferViews", viewIndex);
        ArpgObjectStruct buffer = entry(document, "buffers", number(view, "buffer", 0));
        ByteBuffer binary = document.getBinary();
        int size = componentBytes(componentType);
        int stride = number(view, "byteStride", size * components);
        int start = number(view, "byteOffset", 0) + accessorOffset;

        if (number(view, "buffer", 0) != 0 || buffer.has("uri"))
            throw new InternalException("The file keeps data outside itself; only a self-contained .glb is read");

        if (count > 0 && start + (long) stride * (count - 1) + (long) size * components > binary.limit())
            throw new InternalException("An accessor reads past the end of the file's data");

        for (int i = 0; i < count; i++)
            for (int c = 0; c < components; c++)
                values[i * components + c] = readComponent(binary, start + i * stride + c * size, componentType);
    }

    private static double readComponent(ByteBuffer binary, int offset, int componentType) {

        return switch (componentType) {
            case EditorSetting.GLTF_COMPONENT_BYTE -> binary.get(offset);
            case EditorSetting.GLTF_COMPONENT_UNSIGNED_BYTE -> Byte.toUnsignedInt(binary.get(offset));
            case EditorSetting.GLTF_COMPONENT_SHORT -> binary.getShort(offset);
            case EditorSetting.GLTF_COMPONENT_UNSIGNED_SHORT -> Short.toUnsignedInt(binary.getShort(offset));
            case EditorSetting.GLTF_COMPONENT_UNSIGNED_INT -> Integer.toUnsignedLong(binary.getInt(offset));
            default -> binary.getFloat(offset);
        };
    }
}
