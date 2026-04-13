package program.bootstrap.shaderpipeline.uniforms;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import program.bootstrap.shaderpipeline.uniforms.matrices.*;
import program.bootstrap.shaderpipeline.uniforms.matrixArrays.*;
import program.bootstrap.shaderpipeline.uniforms.samplers.*;
import program.bootstrap.shaderpipeline.uniforms.scalarArrays.*;
import program.bootstrap.shaderpipeline.uniforms.scalars.*;
import program.bootstrap.shaderpipeline.uniforms.vectorarrays.*;
import program.bootstrap.shaderpipeline.uniforms.vectors.*;
import program.core.engine.EngineUtility;
import program.core.util.mathematics.matrices.*;
import program.core.util.mathematics.vectors.*;

public final class UniformUtility extends EngineUtility {

    /*
     * Stateless helpers for std140 layout calculation, UniformAttributeStruct
     * construction, and JSON value parsing. Never instantiated.
     */

    private UniformUtility() {
    }

    // Std140 Layout \\

    public static int align(int offset, int alignment) {
        return ((offset + alignment - 1) / alignment) * alignment;
    }

    public static int getStd140Alignment(UniformData ud) {
        if (ud.getCount() > 1)
            return 16;
        return ud.getUniformType().getStd140Alignment();
    }

    public static int getStd140Size(UniformData ud) {
        int base = ud.getUniformType().getStd140Size();
        if (ud.getCount() > 1)
            return align(base, 16) * ud.getCount();
        return base;
    }

    // Attribute Creation \\

    public static UniformAttributeStruct<?> createUniformAttribute(UniformData ud) {

        int count = ud.getCount();
        boolean isArray = count > 1;

        return switch (ud.getUniformType()) {
            case FLOAT -> isArray ? new FloatArrayUniform(count) : new FloatUniform();
            case DOUBLE -> isArray ? new DoubleArrayUniform(count) : new DoubleUniform();
            case INT -> isArray ? new IntegerArrayUniform(count) : new IntegerUniform();
            case BOOL -> isArray ? new BooleanArrayUniform(count) : new BooleanUniform();
            case VECTOR2 -> isArray ? new Vector2ArrayUniform(count) : new Vector2Uniform();
            case VECTOR3 -> isArray ? new Vector3ArrayUniform(count) : new Vector3Uniform();
            case VECTOR4 -> isArray ? new Vector4ArrayUniform(count) : new Vector4Uniform();
            case VECTOR2_DOUBLE -> isArray ? new Vector2DoubleArrayUniform(count) : new Vector2DoubleUniform();
            case VECTOR3_DOUBLE -> isArray ? new Vector3DoubleArrayUniform(count) : new Vector3DoubleUniform();
            case VECTOR4_DOUBLE -> isArray ? new Vector4DoubleArrayUniform(count) : new Vector4DoubleUniform();
            case VECTOR2_INT -> isArray ? new Vector2IntArrayUniform(count) : new Vector2IntUniform();
            case VECTOR3_INT -> isArray ? new Vector3IntArrayUniform(count) : new Vector3IntUniform();
            case VECTOR4_INT -> isArray ? new Vector4IntArrayUniform(count) : new Vector4IntUniform();
            case VECTOR2_BOOLEAN -> isArray ? new Vector2BooleanArrayUniform(count) : new Vector2BooleanUniform();
            case VECTOR3_BOOLEAN -> isArray ? new Vector3BooleanArrayUniform(count) : new Vector3BooleanUniform();
            case VECTOR4_BOOLEAN -> isArray ? new Vector4BooleanArrayUniform(count) : new Vector4BooleanUniform();
            case MATRIX2 -> isArray ? new Matrix2ArrayUniform(count) : new Matrix2Uniform();
            case MATRIX3 -> isArray ? new Matrix3ArrayUniform(count) : new Matrix3Uniform();
            case MATRIX4 -> isArray ? new Matrix4ArrayUniform(count) : new Matrix4Uniform();
            case MATRIX2_DOUBLE -> isArray ? new Matrix2DoubleArrayUniform(count) : new Matrix2DoubleUniform();
            case MATRIX3_DOUBLE -> isArray ? new Matrix3DoubleArrayUniform(count) : new Matrix3DoubleUniform();
            case MATRIX4_DOUBLE -> isArray ? new Matrix4DoubleArrayUniform(count) : new Matrix4DoubleUniform();
            case SAMPLE_IMAGE_2D -> new SampleImage2DUniform();
            case SAMPLE_IMAGE_2D_ARRAY -> new SampleImage2DArrayUniform();
            default -> {
                throwException("Unsupported uniform type: " + ud.getUniformType());
                yield null;
            }
        };
    }

    // Apply — JSON Object \\

    public static void applyFromJsonObject(
            UniformAttributeStruct<?> attribute,
            String uniformName,
            JsonObject uniformData) {

        if (!uniformData.has("type"))
            throwException("UniformStruct '" + uniformName + "' missing required 'type' field");

        if (!uniformData.has("value"))
            throwException("UniformStruct '" + uniformName + "' missing required 'value' field");

        String type = uniformData.get("type").getAsString();
        JsonElement value = uniformData.get("value");
        boolean isArray = uniformData.has("count");

        if (isArray)
            applyArray(attribute, type, value);
        else
            applySingle(attribute, type, value);
    }

    // Apply — Single \\

    @SuppressWarnings("unchecked")
    public static void applySingle(UniformAttributeStruct<?> attribute, String type, JsonElement value) {
        switch (type) {
            case "FLOAT" -> ((UniformAttributeStruct<Float>) attribute).set(value.getAsFloat());
            case "DOUBLE" -> ((UniformAttributeStruct<Double>) attribute).set(value.getAsDouble());
            case "INT" -> ((UniformAttributeStruct<Integer>) attribute).set(value.getAsInt());
            case "BOOL" -> ((UniformAttributeStruct<Boolean>) attribute).set(value.getAsBoolean());
            case "VECTOR2" -> ((UniformAttributeStruct<Vector2>) attribute).set(parseVector2(value.getAsJsonArray()));
            case "VECTOR2_DOUBLE" ->
                ((UniformAttributeStruct<Vector2Double>) attribute).set(parseVector2Double(value.getAsJsonArray()));
            case "VECTOR2_INT" ->
                ((UniformAttributeStruct<Vector2Int>) attribute).set(parseVector2Int(value.getAsJsonArray()));
            case "VECTOR2_BOOLEAN" ->
                ((UniformAttributeStruct<Vector2Boolean>) attribute).set(parseVector2Boolean(value.getAsJsonArray()));
            case "VECTOR3" -> ((UniformAttributeStruct<Vector3>) attribute).set(parseVector3(value.getAsJsonArray()));
            case "VECTOR3_DOUBLE" ->
                ((UniformAttributeStruct<Vector3Double>) attribute).set(parseVector3Double(value.getAsJsonArray()));
            case "VECTOR3_INT" ->
                ((UniformAttributeStruct<Vector3Int>) attribute).set(parseVector3Int(value.getAsJsonArray()));
            case "VECTOR3_BOOLEAN" ->
                ((UniformAttributeStruct<Vector3Boolean>) attribute).set(parseVector3Boolean(value.getAsJsonArray()));
            case "VECTOR4" -> ((UniformAttributeStruct<Vector4>) attribute).set(parseVector4(value.getAsJsonArray()));
            case "VECTOR4_DOUBLE" ->
                ((UniformAttributeStruct<Vector4Double>) attribute).set(parseVector4Double(value.getAsJsonArray()));
            case "VECTOR4_INT" ->
                ((UniformAttributeStruct<Vector4Int>) attribute).set(parseVector4Int(value.getAsJsonArray()));
            case "VECTOR4_BOOLEAN" ->
                ((UniformAttributeStruct<Vector4Boolean>) attribute).set(parseVector4Boolean(value.getAsJsonArray()));
            case "MATRIX2" -> ((UniformAttributeStruct<Matrix2>) attribute).set(parseMatrix2(value.getAsJsonArray()));
            case "MATRIX3" -> ((UniformAttributeStruct<Matrix3>) attribute).set(parseMatrix3(value.getAsJsonArray()));
            case "MATRIX4" -> ((UniformAttributeStruct<Matrix4>) attribute).set(parseMatrix4(value.getAsJsonArray()));
            case "MATRIX2_DOUBLE" ->
                ((UniformAttributeStruct<Matrix2Double>) attribute).set(parseMatrix2Double(value.getAsJsonArray()));
            case "MATRIX3_DOUBLE" ->
                ((UniformAttributeStruct<Matrix3Double>) attribute).set(parseMatrix3Double(value.getAsJsonArray()));
            case "MATRIX4_DOUBLE" ->
                ((UniformAttributeStruct<Matrix4Double>) attribute).set(parseMatrix4Double(value.getAsJsonArray()));
            default -> {
                throwException("Unsupported uniform type: " + type);
            }
        }
    }

    // Apply — Array \\

    @SuppressWarnings("unchecked")
    public static void applyArray(UniformAttributeStruct<?> attribute, String type, JsonElement valueElement) {

        if (!valueElement.isJsonArray())
            throwException("Array uniform value must be a JSON array");

        JsonArray array = valueElement.getAsJsonArray();

        switch (type) {
            case "FLOAT" -> {
                float[] v = new float[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = array.get(i).getAsFloat();
                ((UniformAttributeStruct<float[]>) attribute).set(v);
            }
            case "DOUBLE" -> {
                double[] v = new double[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = array.get(i).getAsDouble();
                ((UniformAttributeStruct<double[]>) attribute).set(v);
            }
            case "INT" -> {
                Integer[] v = new Integer[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = array.get(i).getAsInt();
                ((UniformAttributeStruct<Integer[]>) attribute).set(v);
            }
            case "BOOL" -> {
                Boolean[] v = new Boolean[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = array.get(i).getAsBoolean();
                ((UniformAttributeStruct<Boolean[]>) attribute).set(v);
            }
            case "VECTOR2" -> {
                Vector2[] v = new Vector2[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseVector2(array.get(i).getAsJsonArray());
                ((UniformAttributeStruct<Vector2[]>) attribute).set(v);
            }
            case "VECTOR3" -> {
                Vector3[] v = new Vector3[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseVector3(array.get(i).getAsJsonArray());
                ((UniformAttributeStruct<Vector3[]>) attribute).set(v);
            }
            case "VECTOR4" -> {
                Vector4[] v = new Vector4[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseVector4(array.get(i).getAsJsonArray());
                ((UniformAttributeStruct<Vector4[]>) attribute).set(v);
            }
            case "VECTOR2_DOUBLE" -> {
                Vector2Double[] v = new Vector2Double[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseVector2Double(array.get(i).getAsJsonArray());
                ((UniformAttributeStruct<Vector2Double[]>) attribute).set(v);
            }
            case "VECTOR3_DOUBLE" -> {
                Vector3Double[] v = new Vector3Double[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseVector3Double(array.get(i).getAsJsonArray());
                ((UniformAttributeStruct<Vector3Double[]>) attribute).set(v);
            }
            case "VECTOR4_DOUBLE" -> {
                Vector4Double[] v = new Vector4Double[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseVector4Double(array.get(i).getAsJsonArray());
                ((UniformAttributeStruct<Vector4Double[]>) attribute).set(v);
            }
            case "VECTOR2_INT" -> {
                Vector2Int[] v = new Vector2Int[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseVector2Int(array.get(i).getAsJsonArray());
                ((UniformAttributeStruct<Vector2Int[]>) attribute).set(v);
            }
            case "VECTOR3_INT" -> {
                Vector3Int[] v = new Vector3Int[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseVector3Int(array.get(i).getAsJsonArray());
                ((UniformAttributeStruct<Vector3Int[]>) attribute).set(v);
            }
            case "VECTOR4_INT" -> {
                Vector4Int[] v = new Vector4Int[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseVector4Int(array.get(i).getAsJsonArray());
                ((UniformAttributeStruct<Vector4Int[]>) attribute).set(v);
            }
            case "VECTOR2_BOOLEAN" -> {
                Vector2Boolean[] v = new Vector2Boolean[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseVector2Boolean(array.get(i).getAsJsonArray());
                ((UniformAttributeStruct<Vector2Boolean[]>) attribute).set(v);
            }
            case "VECTOR3_BOOLEAN" -> {
                Vector3Boolean[] v = new Vector3Boolean[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseVector3Boolean(array.get(i).getAsJsonArray());
                ((UniformAttributeStruct<Vector3Boolean[]>) attribute).set(v);
            }
            case "VECTOR4_BOOLEAN" -> {
                Vector4Boolean[] v = new Vector4Boolean[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseVector4Boolean(array.get(i).getAsJsonArray());
                ((UniformAttributeStruct<Vector4Boolean[]>) attribute).set(v);
            }
            case "MATRIX2" -> {
                Matrix2[] v = new Matrix2[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseMatrix2(array.get(i).getAsJsonArray());
                ((UniformAttributeStruct<Matrix2[]>) attribute).set(v);
            }
            case "MATRIX3" -> {
                Matrix3[] v = new Matrix3[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseMatrix3(array.get(i).getAsJsonArray());
                ((UniformAttributeStruct<Matrix3[]>) attribute).set(v);
            }
            case "MATRIX4" -> {
                Matrix4[] v = new Matrix4[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseMatrix4(array.get(i).getAsJsonArray());
                ((UniformAttributeStruct<Matrix4[]>) attribute).set(v);
            }
            case "MATRIX2_DOUBLE" -> {
                Matrix2Double[] v = new Matrix2Double[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseMatrix2Double(array.get(i).getAsJsonArray());
                ((UniformAttributeStruct<Matrix2Double[]>) attribute).set(v);
            }
            case "MATRIX3_DOUBLE" -> {
                Matrix3Double[] v = new Matrix3Double[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseMatrix3Double(array.get(i).getAsJsonArray());
                ((UniformAttributeStruct<Matrix3Double[]>) attribute).set(v);
            }
            case "MATRIX4_DOUBLE" -> {
                Matrix4Double[] v = new Matrix4Double[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseMatrix4Double(array.get(i).getAsJsonArray());
                ((UniformAttributeStruct<Matrix4Double[]>) attribute).set(v);
            }
            default -> throwException("Unsupported uniform array type: " + type);
        }
    }

    // Parse — Vector2 \\

    public static Vector2 parseVector2(JsonArray array) {
        if (array.size() != 2)
            throwException("Vector2 requires 2 values, got " + array.size());
        return new Vector2(array.get(0).getAsFloat(), array.get(1).getAsFloat());
    }

    public static Vector2Double parseVector2Double(JsonArray array) {
        if (array.size() != 2)
            throwException("Vector2Double requires 2 values, got " + array.size());
        return new Vector2Double(array.get(0).getAsDouble(), array.get(1).getAsDouble());
    }

    public static Vector2Int parseVector2Int(JsonArray array) {
        if (array.size() != 2)
            throwException("Vector2Int requires 2 values, got " + array.size());
        return new Vector2Int(array.get(0).getAsInt(), array.get(1).getAsInt());
    }

    public static Vector2Boolean parseVector2Boolean(JsonArray array) {
        if (array.size() != 2)
            throwException("Vector2Boolean requires 2 values, got " + array.size());
        return new Vector2Boolean(array.get(0).getAsBoolean(), array.get(1).getAsBoolean());
    }

    // Parse — Vector3 \\

    public static Vector3 parseVector3(JsonArray array) {
        if (array.size() != 3)
            throwException("Vector3 requires 3 values, got " + array.size());
        return new Vector3(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    public static Vector3Double parseVector3Double(JsonArray array) {
        if (array.size() != 3)
            throwException("Vector3Double requires 3 values, got " + array.size());
        return new Vector3Double(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
    }

    public static Vector3Int parseVector3Int(JsonArray array) {
        if (array.size() != 3)
            throwException("Vector3Int requires 3 values, got " + array.size());
        return new Vector3Int(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt());
    }

    public static Vector3Boolean parseVector3Boolean(JsonArray array) {
        if (array.size() != 3)
            throwException("Vector3Boolean requires 3 values, got " + array.size());
        return new Vector3Boolean(
                array.get(0).getAsBoolean(),
                array.get(1).getAsBoolean(),
                array.get(2).getAsBoolean());
    }

    // Parse — Vector4 \\

    public static Vector4 parseVector4(JsonArray array) {
        if (array.size() != 4)
            throwException("Vector4 requires 4 values, got " + array.size());
        return new Vector4(
                array.get(0).getAsFloat(), array.get(1).getAsFloat(),
                array.get(2).getAsFloat(), array.get(3).getAsFloat());
    }

    public static Vector4Double parseVector4Double(JsonArray array) {
        if (array.size() != 4)
            throwException("Vector4Double requires 4 values, got " + array.size());
        return new Vector4Double(
                array.get(0).getAsDouble(), array.get(1).getAsDouble(),
                array.get(2).getAsDouble(), array.get(3).getAsDouble());
    }

    public static Vector4Int parseVector4Int(JsonArray array) {
        if (array.size() != 4)
            throwException("Vector4Int requires 4 values, got " + array.size());
        return new Vector4Int(
                array.get(0).getAsInt(), array.get(1).getAsInt(),
                array.get(2).getAsInt(), array.get(3).getAsInt());
    }

    public static Vector4Boolean parseVector4Boolean(JsonArray array) {
        if (array.size() != 4)
            throwException("Vector4Boolean requires 4 values, got " + array.size());
        return new Vector4Boolean(
                array.get(0).getAsBoolean(), array.get(1).getAsBoolean(),
                array.get(2).getAsBoolean(), array.get(3).getAsBoolean());
    }

    // Parse — Matrix \\

    public static Matrix2 parseMatrix2(JsonArray array) {
        if (array.size() != 4)
            throwException("Matrix2 requires 4 values, got " + array.size());
        float[] v = new float[4];
        for (int i = 0; i < 4; i++)
            v[i] = array.get(i).getAsFloat();
        return new Matrix2(v);
    }

    public static Matrix3 parseMatrix3(JsonArray array) {
        if (array.size() != 9)
            throwException("Matrix3 requires 9 values, got " + array.size());
        float[] v = new float[9];
        for (int i = 0; i < 9; i++)
            v[i] = array.get(i).getAsFloat();
        return new Matrix3(v);
    }

    public static Matrix4 parseMatrix4(JsonArray array) {
        if (array.size() != 16)
            throwException("Matrix4 requires 16 values, got " + array.size());
        float[] v = new float[16];
        for (int i = 0; i < 16; i++)
            v[i] = array.get(i).getAsFloat();
        return new Matrix4(v);
    }

    public static Matrix2Double parseMatrix2Double(JsonArray array) {
        if (array.size() != 4)
            throwException("Matrix2Double requires 4 values, got " + array.size());
        double[] v = new double[4];
        for (int i = 0; i < 4; i++)
            v[i] = array.get(i).getAsDouble();
        return new Matrix2Double(v);
    }

    public static Matrix3Double parseMatrix3Double(JsonArray array) {
        if (array.size() != 9)
            throwException("Matrix3Double requires 9 values, got " + array.size());
        double[] v = new double[9];
        for (int i = 0; i < 9; i++)
            v[i] = array.get(i).getAsDouble();
        return new Matrix3Double(v);
    }

    public static Matrix4Double parseMatrix4Double(JsonArray array) {
        if (array.size() != 16)
            throwException("Matrix4Double requires 16 values, got " + array.size());
        double[] v = new double[16];
        for (int i = 0; i < 16; i++)
            v[i] = array.get(i).getAsDouble();
        return new Matrix4Double(v);
    }
}