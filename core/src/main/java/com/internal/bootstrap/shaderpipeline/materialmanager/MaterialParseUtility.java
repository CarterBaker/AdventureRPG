package com.internal.bootstrap.shaderpipeline.materialmanager;

import com.google.gson.JsonArray;
import com.internal.core.engine.UtilityPackage;
import com.internal.core.util.mathematics.matrices.*;
import com.internal.core.util.mathematics.vectors.*;

/*
 * Parses JSON arrays into typed math objects for uniform value assignment during
 * material bootstrap. All methods are stateless and size-validated before construction.
 */
class MaterialParseUtility extends UtilityPackage {

    // Vector2 \\

    static Vector2 parseVector2(JsonArray array) {
        if (array.size() != 2)
            throwException("Vector2 requires exactly 2 values, got " + array.size());
        return new Vector2(array.get(0).getAsFloat(), array.get(1).getAsFloat());
    }

    static Vector2Double parseVector2Double(JsonArray array) {
        if (array.size() != 2)
            throwException("Vector2Double requires exactly 2 values, got " + array.size());
        return new Vector2Double(array.get(0).getAsDouble(), array.get(1).getAsDouble());
    }

    static Vector2Int parseVector2Int(JsonArray array) {
        if (array.size() != 2)
            throwException("Vector2Int requires exactly 2 values, got " + array.size());
        return new Vector2Int(array.get(0).getAsInt(), array.get(1).getAsInt());
    }

    static Vector2Boolean parseVector2Boolean(JsonArray array) {
        if (array.size() != 2)
            throwException("Vector2Boolean requires exactly 2 values, got " + array.size());
        return new Vector2Boolean(array.get(0).getAsBoolean(), array.get(1).getAsBoolean());
    }

    // Vector3 \\

    static Vector3 parseVector3(JsonArray array) {
        if (array.size() != 3)
            throwException("Vector3 requires exactly 3 values, got " + array.size());
        return new Vector3(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    static Vector3Double parseVector3Double(JsonArray array) {
        if (array.size() != 3)
            throwException("Vector3Double requires exactly 3 values, got " + array.size());
        return new Vector3Double(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
    }

    static Vector3Int parseVector3Int(JsonArray array) {
        if (array.size() != 3)
            throwException("Vector3Int requires exactly 3 values, got " + array.size());
        return new Vector3Int(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt());
    }

    static Vector3Boolean parseVector3Boolean(JsonArray array) {
        if (array.size() != 3)
            throwException("Vector3Boolean requires exactly 3 values, got " + array.size());
        return new Vector3Boolean(array.get(0).getAsBoolean(), array.get(1).getAsBoolean(),
                array.get(2).getAsBoolean());
    }

    // Vector4 \\

    static Vector4 parseVector4(JsonArray array) {
        if (array.size() != 4)
            throwException("Vector4 requires exactly 4 values, got " + array.size());
        return new Vector4(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat(),
                array.get(3).getAsFloat());
    }

    static Vector4Double parseVector4Double(JsonArray array) {
        if (array.size() != 4)
            throwException("Vector4Double requires exactly 4 values, got " + array.size());
        return new Vector4Double(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble(),
                array.get(3).getAsDouble());
    }

    static Vector4Int parseVector4Int(JsonArray array) {
        if (array.size() != 4)
            throwException("Vector4Int requires exactly 4 values, got " + array.size());
        return new Vector4Int(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt(),
                array.get(3).getAsInt());
    }

    static Vector4Boolean parseVector4Boolean(JsonArray array) {
        if (array.size() != 4)
            throwException("Vector4Boolean requires exactly 4 values, got " + array.size());
        return new Vector4Boolean(array.get(0).getAsBoolean(), array.get(1).getAsBoolean(), array.get(2).getAsBoolean(),
                array.get(3).getAsBoolean());
    }

    // Matrix \\

    static Matrix2 parseMatrix2(JsonArray array) {
        if (array.size() != 4)
            throwException("Matrix2 requires exactly 4 values, got " + array.size());
        float[] v = new float[4];
        for (int i = 0; i < 4; i++)
            v[i] = array.get(i).getAsFloat();
        return new Matrix2(v);
    }

    static Matrix3 parseMatrix3(JsonArray array) {
        if (array.size() != 9)
            throwException("Matrix3 requires exactly 9 values, got " + array.size());
        float[] v = new float[9];
        for (int i = 0; i < 9; i++)
            v[i] = array.get(i).getAsFloat();
        return new Matrix3(v);
    }

    static Matrix4 parseMatrix4(JsonArray array) {
        if (array.size() != 16)
            throwException("Matrix4 requires exactly 16 values, got " + array.size());
        float[] v = new float[16];
        for (int i = 0; i < 16; i++)
            v[i] = array.get(i).getAsFloat();
        return new Matrix4(v);
    }

    static Matrix2Double parseMatrix2Double(JsonArray array) {
        if (array.size() != 4)
            throwException("Matrix2Double requires exactly 4 values, got " + array.size());
        double[] v = new double[4];
        for (int i = 0; i < 4; i++)
            v[i] = array.get(i).getAsDouble();
        return new Matrix2Double(v);
    }

    static Matrix3Double parseMatrix3Double(JsonArray array) {
        if (array.size() != 9)
            throwException("Matrix3Double requires exactly 9 values, got " + array.size());
        double[] v = new double[9];
        for (int i = 0; i < 9; i++)
            v[i] = array.get(i).getAsDouble();
        return new Matrix3Double(v);
    }

    static Matrix4Double parseMatrix4Double(JsonArray array) {
        if (array.size() != 16)
            throwException("Matrix4Double requires exactly 16 values, got " + array.size());
        double[] v = new double[16];
        for (int i = 0; i < 16; i++)
            v[i] = array.get(i).getAsDouble();
        return new Matrix4Double(v);
    }
}