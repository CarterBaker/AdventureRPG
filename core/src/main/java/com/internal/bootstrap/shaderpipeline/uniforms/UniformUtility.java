package com.internal.bootstrap.shaderpipeline.uniforms;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.internal.bootstrap.shaderpipeline.uniforms.matrices.*;
import com.internal.bootstrap.shaderpipeline.uniforms.matrixArrays.*;
import com.internal.bootstrap.shaderpipeline.uniforms.samplers.*;
import com.internal.bootstrap.shaderpipeline.uniforms.scalarArrays.*;
import com.internal.bootstrap.shaderpipeline.uniforms.scalars.*;
import com.internal.bootstrap.shaderpipeline.uniforms.vectorarrays.*;
import com.internal.bootstrap.shaderpipeline.uniforms.vectors.*;
import com.internal.core.util.mathematics.matrices.*;
import com.internal.core.util.mathematics.vectors.*;

/*
 * Stateless utility covering all uniform type operations shared across systems:
 *
 *   JSON parsing  — applySingle / applyArray / applyFromJsonObject / parse*
 *                   Used by material bootstrap to set initial uniform values.
 *
 *   Std140 layout — getStd140Alignment / getStd140Size / align
 *                   Used by UBOManager to compute buffer offsets and total size.
 *
 *   Attribute creation — createUniformAttribute
 *                        Used by UBOManager when populating a UBOHandle's uniform map.
 */
public final class UniformUtility {

    private UniformUtility() {
    }

    // Apply — Single \\

    @SuppressWarnings("unchecked")
    public static void applySingle(UniformAttribute<?> attribute, String type, JsonElement value) {
        switch (type) {
            case "FLOAT" -> ((UniformAttribute<Float>) attribute).set(value.getAsFloat());
            case "DOUBLE" -> ((UniformAttribute<Double>) attribute).set(value.getAsDouble());
            case "INT" -> ((UniformAttribute<Integer>) attribute).set(value.getAsInt());
            case "BOOL" -> ((UniformAttribute<Boolean>) attribute).set(value.getAsBoolean());
            case "VECTOR2" -> ((UniformAttribute<Vector2>) attribute).set(parseVector2(value.getAsJsonArray()));
            case "VECTOR2_DOUBLE" ->
                ((UniformAttribute<Vector2Double>) attribute).set(parseVector2Double(value.getAsJsonArray()));
            case "VECTOR2_INT" ->
                ((UniformAttribute<Vector2Int>) attribute).set(parseVector2Int(value.getAsJsonArray()));
            case "VECTOR2_BOOLEAN" ->
                ((UniformAttribute<Vector2Boolean>) attribute).set(parseVector2Boolean(value.getAsJsonArray()));
            case "VECTOR3" -> ((UniformAttribute<Vector3>) attribute).set(parseVector3(value.getAsJsonArray()));
            case "VECTOR3_DOUBLE" ->
                ((UniformAttribute<Vector3Double>) attribute).set(parseVector3Double(value.getAsJsonArray()));
            case "VECTOR3_INT" ->
                ((UniformAttribute<Vector3Int>) attribute).set(parseVector3Int(value.getAsJsonArray()));
            case "VECTOR3_BOOLEAN" ->
                ((UniformAttribute<Vector3Boolean>) attribute).set(parseVector3Boolean(value.getAsJsonArray()));
            case "VECTOR4" -> ((UniformAttribute<Vector4>) attribute).set(parseVector4(value.getAsJsonArray()));
            case "VECTOR4_DOUBLE" ->
                ((UniformAttribute<Vector4Double>) attribute).set(parseVector4Double(value.getAsJsonArray()));
            case "VECTOR4_INT" ->
                ((UniformAttribute<Vector4Int>) attribute).set(parseVector4Int(value.getAsJsonArray()));
            case "VECTOR4_BOOLEAN" ->
                ((UniformAttribute<Vector4Boolean>) attribute).set(parseVector4Boolean(value.getAsJsonArray()));
            case "MATRIX2" -> ((UniformAttribute<Matrix2>) attribute).set(parseMatrix2(value.getAsJsonArray()));
            case "MATRIX3" -> ((UniformAttribute<Matrix3>) attribute).set(parseMatrix3(value.getAsJsonArray()));
            case "MATRIX4" -> ((UniformAttribute<Matrix4>) attribute).set(parseMatrix4(value.getAsJsonArray()));
            case "MATRIX2_DOUBLE" ->
                ((UniformAttribute<Matrix2Double>) attribute).set(parseMatrix2Double(value.getAsJsonArray()));
            case "MATRIX3_DOUBLE" ->
                ((UniformAttribute<Matrix3Double>) attribute).set(parseMatrix3Double(value.getAsJsonArray()));
            case "MATRIX4_DOUBLE" ->
                ((UniformAttribute<Matrix4Double>) attribute).set(parseMatrix4Double(value.getAsJsonArray()));
            default -> throw new IllegalArgumentException("Unsupported uniform type: " + type);
        }
    }

    // Apply — Array \\

    @SuppressWarnings("unchecked")
    public static void applyArray(UniformAttribute<?> attribute, String type, JsonElement valueElement) {

        if (!valueElement.isJsonArray())
            throw new IllegalArgumentException("Array uniform value must be a JSON array");

        JsonArray array = valueElement.getAsJsonArray();

        switch (type) {
            case "FLOAT" -> {
                Float[] v = new Float[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = array.get(i).getAsFloat();
                ((UniformAttribute<Float[]>) attribute).set(v);
            }
            case "DOUBLE" -> {
                Double[] v = new Double[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = array.get(i).getAsDouble();
                ((UniformAttribute<Double[]>) attribute).set(v);
            }
            case "INT" -> {
                Integer[] v = new Integer[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = array.get(i).getAsInt();
                ((UniformAttribute<Integer[]>) attribute).set(v);
            }
            case "BOOL" -> {
                Boolean[] v = new Boolean[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = array.get(i).getAsBoolean();
                ((UniformAttribute<Boolean[]>) attribute).set(v);
            }
            case "VECTOR2" -> {
                Vector2[] v = new Vector2[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseVector2(array.get(i).getAsJsonArray());
                ((UniformAttribute<Vector2[]>) attribute).set(v);
            }
            case "VECTOR2_DOUBLE" -> {
                Vector2Double[] v = new Vector2Double[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseVector2Double(array.get(i).getAsJsonArray());
                ((UniformAttribute<Vector2Double[]>) attribute).set(v);
            }
            case "VECTOR2_INT" -> {
                Vector2Int[] v = new Vector2Int[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseVector2Int(array.get(i).getAsJsonArray());
                ((UniformAttribute<Vector2Int[]>) attribute).set(v);
            }
            case "VECTOR3" -> {
                Vector3[] v = new Vector3[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseVector3(array.get(i).getAsJsonArray());
                ((UniformAttribute<Vector3[]>) attribute).set(v);
            }
            case "VECTOR3_DOUBLE" -> {
                Vector3Double[] v = new Vector3Double[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseVector3Double(array.get(i).getAsJsonArray());
                ((UniformAttribute<Vector3Double[]>) attribute).set(v);
            }
            case "VECTOR3_INT" -> {
                Vector3Int[] v = new Vector3Int[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseVector3Int(array.get(i).getAsJsonArray());
                ((UniformAttribute<Vector3Int[]>) attribute).set(v);
            }
            case "VECTOR4" -> {
                Vector4[] v = new Vector4[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseVector4(array.get(i).getAsJsonArray());
                ((UniformAttribute<Vector4[]>) attribute).set(v);
            }
            case "VECTOR4_DOUBLE" -> {
                Vector4Double[] v = new Vector4Double[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseVector4Double(array.get(i).getAsJsonArray());
                ((UniformAttribute<Vector4Double[]>) attribute).set(v);
            }
            case "VECTOR4_INT" -> {
                Vector4Int[] v = new Vector4Int[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseVector4Int(array.get(i).getAsJsonArray());
                ((UniformAttribute<Vector4Int[]>) attribute).set(v);
            }
            case "MATRIX2" -> {
                Matrix2[] v = new Matrix2[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseMatrix2(array.get(i).getAsJsonArray());
                ((UniformAttribute<Matrix2[]>) attribute).set(v);
            }
            case "MATRIX3" -> {
                Matrix3[] v = new Matrix3[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseMatrix3(array.get(i).getAsJsonArray());
                ((UniformAttribute<Matrix3[]>) attribute).set(v);
            }
            case "MATRIX4" -> {
                Matrix4[] v = new Matrix4[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseMatrix4(array.get(i).getAsJsonArray());
                ((UniformAttribute<Matrix4[]>) attribute).set(v);
            }
            case "MATRIX2_DOUBLE" -> {
                Matrix2Double[] v = new Matrix2Double[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseMatrix2Double(array.get(i).getAsJsonArray());
                ((UniformAttribute<Matrix2Double[]>) attribute).set(v);
            }
            case "MATRIX3_DOUBLE" -> {
                Matrix3Double[] v = new Matrix3Double[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseMatrix3Double(array.get(i).getAsJsonArray());
                ((UniformAttribute<Matrix3Double[]>) attribute).set(v);
            }
            case "MATRIX4_DOUBLE" -> {
                Matrix4Double[] v = new Matrix4Double[array.size()];
                for (int i = 0; i < array.size(); i++)
                    v[i] = parseMatrix4Double(array.get(i).getAsJsonArray());
                ((UniformAttribute<Matrix4Double[]>) attribute).set(v);
            }
            default -> throw new IllegalArgumentException("Unsupported uniform array type: " + type);
        }
    }

    // Apply — JSON Object \\

    /*
     * Dispatches a full uniform JSON object { "type", "value", "count"? } to
     * applySingle or applyArray. Used by the material builder for initial values.
     */
    public static void applyFromJsonObject(
            UniformAttribute<?> attribute,
            String uniformName,
            JsonObject uniformData) {

        if (!uniformData.has("type"))
            throw new IllegalArgumentException("Uniform '" + uniformName + "' missing required 'type' field");

        if (!uniformData.has("value"))
            throw new IllegalArgumentException("Uniform '" + uniformName + "' missing required 'value' field");

        String type = uniformData.get("type").getAsString();
        JsonElement value = uniformData.get("value");
        boolean isArray = uniformData.has("count");

        if (isArray)
            applyArray(attribute, type, value);
        else
            applySingle(attribute, type, value);
    }

    // Std140 Layout \\

    public static int align(int offset, int alignment) {
        return ((offset + alignment - 1) / alignment) * alignment;
    }

    public static int getStd140Alignment(UniformData ud) {

        if (ud.getCount() > 1)
            return 16;

        return switch (ud.getUniformType()) {
            case FLOAT, INT, BOOL -> 4;
            case DOUBLE -> 8;
            case VECTOR2, VECTOR2_INT, VECTOR2_BOOLEAN -> 8;
            case VECTOR2_DOUBLE -> 16;
            case VECTOR3, VECTOR3_INT, VECTOR3_BOOLEAN,
                    VECTOR4, VECTOR4_INT, VECTOR4_BOOLEAN ->
                16;
            case VECTOR3_DOUBLE, VECTOR4_DOUBLE -> 32;
            default -> 16;
        };
    }

    public static int getStd140Size(UniformData ud) {

        int base = switch (ud.getUniformType()) {
            case FLOAT, INT, BOOL -> 4;
            case DOUBLE -> 8;
            case VECTOR2, VECTOR2_INT, VECTOR2_BOOLEAN -> 8;
            case VECTOR3, VECTOR3_INT, VECTOR3_BOOLEAN -> 12;
            case VECTOR4, VECTOR4_INT, VECTOR4_BOOLEAN -> 16;
            case MATRIX2 -> 32;
            case MATRIX3 -> 48;
            case MATRIX4 -> 64;
            case VECTOR2_DOUBLE -> 16;
            case VECTOR3_DOUBLE -> 24;
            case VECTOR4_DOUBLE -> 32;
            case MATRIX2_DOUBLE -> 64;
            case MATRIX3_DOUBLE -> 96;
            case MATRIX4_DOUBLE -> 128;
            default -> 16;
        };

        if (ud.getCount() > 1)
            return align(base, 16) * ud.getCount();

        return base;
    }

    // Attribute Creation \\

    public static UniformAttribute<?> createUniformAttribute(UniformData ud) {

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
            default -> throw new IllegalArgumentException("Unsupported uniform type: " + ud.getUniformType());
        };
    }

    // Parse — Vector2 \\

    public static Vector2 parseVector2(JsonArray array) {
        if (array.size() != 2)
            throw new IllegalArgumentException("Vector2 requires exactly 2 values, got " + array.size());
        return new Vector2(array.get(0).getAsFloat(), array.get(1).getAsFloat());
    }

    public static Vector2Double parseVector2Double(JsonArray array) {
        if (array.size() != 2)
            throw new IllegalArgumentException("Vector2Double requires exactly 2 values, got " + array.size());
        return new Vector2Double(array.get(0).getAsDouble(), array.get(1).getAsDouble());
    }

    public static Vector2Int parseVector2Int(JsonArray array) {
        if (array.size() != 2)
            throw new IllegalArgumentException("Vector2Int requires exactly 2 values, got " + array.size());
        return new Vector2Int(array.get(0).getAsInt(), array.get(1).getAsInt());
    }

    public static Vector2Boolean parseVector2Boolean(JsonArray array) {
        if (array.size() != 2)
            throw new IllegalArgumentException("Vector2Boolean requires exactly 2 values, got " + array.size());
        return new Vector2Boolean(array.get(0).getAsBoolean(), array.get(1).getAsBoolean());
    }

    // Parse — Vector3 \\

    public static Vector3 parseVector3(JsonArray array) {
        if (array.size() != 3)
            throw new IllegalArgumentException("Vector3 requires exactly 3 values, got " + array.size());
        return new Vector3(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    public static Vector3Double parseVector3Double(JsonArray array) {
        if (array.size() != 3)
            throw new IllegalArgumentException("Vector3Double requires exactly 3 values, got " + array.size());
        return new Vector3Double(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
    }

    public static Vector3Int parseVector3Int(JsonArray array) {
        if (array.size() != 3)
            throw new IllegalArgumentException("Vector3Int requires exactly 3 values, got " + array.size());
        return new Vector3Int(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt());
    }

    public static Vector3Boolean parseVector3Boolean(JsonArray array) {
        if (array.size() != 3)
            throw new IllegalArgumentException("Vector3Boolean requires exactly 3 values, got " + array.size());
        return new Vector3Boolean(
                array.get(0).getAsBoolean(),
                array.get(1).getAsBoolean(),
                array.get(2).getAsBoolean());
    }

    // Parse — Vector4 \\

    public static Vector4 parseVector4(JsonArray array) {
        if (array.size() != 4)
            throw new IllegalArgumentException("Vector4 requires exactly 4 values, got " + array.size());
        return new Vector4(
                array.get(0).getAsFloat(), array.get(1).getAsFloat(),
                array.get(2).getAsFloat(), array.get(3).getAsFloat());
    }

    public static Vector4Double parseVector4Double(JsonArray array) {
        if (array.size() != 4)
            throw new IllegalArgumentException("Vector4Double requires exactly 4 values, got " + array.size());
        return new Vector4Double(
                array.get(0).getAsDouble(), array.get(1).getAsDouble(),
                array.get(2).getAsDouble(), array.get(3).getAsDouble());
    }

    public static Vector4Int parseVector4Int(JsonArray array) {
        if (array.size() != 4)
            throw new IllegalArgumentException("Vector4Int requires exactly 4 values, got " + array.size());
        return new Vector4Int(
                array.get(0).getAsInt(), array.get(1).getAsInt(),
                array.get(2).getAsInt(), array.get(3).getAsInt());
    }

    public static Vector4Boolean parseVector4Boolean(JsonArray array) {
        if (array.size() != 4)
            throw new IllegalArgumentException("Vector4Boolean requires exactly 4 values, got " + array.size());
        return new Vector4Boolean(
                array.get(0).getAsBoolean(), array.get(1).getAsBoolean(),
                array.get(2).getAsBoolean(), array.get(3).getAsBoolean());
    }

    // Parse — Matrix \\

    public static Matrix2 parseMatrix2(JsonArray array) {
        if (array.size() != 4)
            throw new IllegalArgumentException("Matrix2 requires exactly 4 values, got " + array.size());
        float[] v = new float[4];
        for (int i = 0; i < 4; i++)
            v[i] = array.get(i).getAsFloat();
        return new Matrix2(v);
    }

    public static Matrix3 parseMatrix3(JsonArray array) {
        if (array.size() != 9)
            throw new IllegalArgumentException("Matrix3 requires exactly 9 values, got " + array.size());
        float[] v = new float[9];
        for (int i = 0; i < 9; i++)
            v[i] = array.get(i).getAsFloat();
        return new Matrix3(v);
    }

    public static Matrix4 parseMatrix4(JsonArray array) {
        if (array.size() != 16)
            throw new IllegalArgumentException("Matrix4 requires exactly 16 values, got " + array.size());
        float[] v = new float[16];
        for (int i = 0; i < 16; i++)
            v[i] = array.get(i).getAsFloat();
        return new Matrix4(v);
    }

    public static Matrix2Double parseMatrix2Double(JsonArray array) {
        if (array.size() != 4)
            throw new IllegalArgumentException("Matrix2Double requires exactly 4 values, got " + array.size());
        double[] v = new double[4];
        for (int i = 0; i < 4; i++)
            v[i] = array.get(i).getAsDouble();
        return new Matrix2Double(v);
    }

    public static Matrix3Double parseMatrix3Double(JsonArray array) {
        if (array.size() != 9)
            throw new IllegalArgumentException("Matrix3Double requires exactly 9 values, got " + array.size());
        double[] v = new double[9];
        for (int i = 0; i < 9; i++)
            v[i] = array.get(i).getAsDouble();
        return new Matrix3Double(v);
    }

    public static Matrix4Double parseMatrix4Double(JsonArray array) {
        if (array.size() != 16)
            throw new IllegalArgumentException("Matrix4Double requires exactly 16 values, got " + array.size());
        double[] v = new double[16];
        for (int i = 0; i < 16; i++)
            v[i] = array.get(i).getAsDouble();
        return new Matrix4Double(v);
    }
}