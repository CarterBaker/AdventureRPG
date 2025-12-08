package com.AdventureRPG.core.renderpipeline.materialmanager;

import com.AdventureRPG.core.util.Methematics.Matrices.*;
import com.AdventureRPG.core.util.Methematics.Vectors.*;
import com.google.gson.JsonArray;

class MaterialParseUtility {

    // ===== Vector2 Parsers =====

    public static Vector2 parseVector2(JsonArray array) {
        if (array.size() != 2) {
            throw new IllegalArgumentException(
                    "Vector2 requires exactly 2 values, got " + array.size());
        }
        return new Vector2(
                array.get(0).getAsFloat(),
                array.get(1).getAsFloat());
    }

    public static Vector2Double parseVector2Double(JsonArray array) {
        if (array.size() != 2) {
            throw new IllegalArgumentException(
                    "Vector2Double requires exactly 2 values, got " + array.size());
        }
        return new Vector2Double(
                array.get(0).getAsDouble(),
                array.get(1).getAsDouble());
    }

    public static Vector2Int parseVector2Int(JsonArray array) {
        if (array.size() != 2) {
            throw new IllegalArgumentException(
                    "Vector2Int requires exactly 2 values, got " + array.size());
        }
        return new Vector2Int(
                array.get(0).getAsInt(),
                array.get(1).getAsInt());
    }

    public static Vector2Boolean parseVector2Boolean(JsonArray array) {
        if (array.size() != 2) {
            throw new IllegalArgumentException(
                    "Vector2Boolean requires exactly 2 values, got " + array.size());
        }
        return new Vector2Boolean(
                array.get(0).getAsBoolean(),
                array.get(1).getAsBoolean());
    }

    // ===== Vector3 Parsers =====

    public static Vector3 parseVector3(JsonArray array) {
        if (array.size() != 3) {
            throw new IllegalArgumentException(
                    "Vector3 requires exactly 3 values, got " + array.size());
        }
        return new Vector3(
                array.get(0).getAsFloat(),
                array.get(1).getAsFloat(),
                array.get(2).getAsFloat());
    }

    public static Vector3Double parseVector3Double(JsonArray array) {
        if (array.size() != 3) {
            throw new IllegalArgumentException(
                    "Vector3Double requires exactly 3 values, got " + array.size());
        }
        return new Vector3Double(
                array.get(0).getAsDouble(),
                array.get(1).getAsDouble(),
                array.get(2).getAsDouble());
    }

    public static Vector3Int parseVector3Int(JsonArray array) {
        if (array.size() != 3) {
            throw new IllegalArgumentException(
                    "Vector3Int requires exactly 3 values, got " + array.size());
        }
        return new Vector3Int(
                array.get(0).getAsInt(),
                array.get(1).getAsInt(),
                array.get(2).getAsInt());
    }

    public static Vector3Boolean parseVector3Boolean(JsonArray array) {
        if (array.size() != 3) {
            throw new IllegalArgumentException(
                    "Vector3Boolean requires exactly 3 values, got " + array.size());
        }
        return new Vector3Boolean(
                array.get(0).getAsBoolean(),
                array.get(1).getAsBoolean(),
                array.get(2).getAsBoolean());
    }

    // ===== Vector4 Parsers =====

    public static Vector4 parseVector4(JsonArray array) {
        if (array.size() != 4) {
            throw new IllegalArgumentException(
                    "Vector4 requires exactly 4 values, got " + array.size());
        }
        return new Vector4(
                array.get(0).getAsFloat(),
                array.get(1).getAsFloat(),
                array.get(2).getAsFloat(),
                array.get(3).getAsFloat());
    }

    public static Vector4Double parseVector4Double(JsonArray array) {
        if (array.size() != 4) {
            throw new IllegalArgumentException(
                    "Vector4Double requires exactly 4 values, got " + array.size());
        }
        return new Vector4Double(
                array.get(0).getAsDouble(),
                array.get(1).getAsDouble(),
                array.get(2).getAsDouble(),
                array.get(3).getAsDouble());
    }

    public static Vector4Int parseVector4Int(JsonArray array) {
        if (array.size() != 4) {
            throw new IllegalArgumentException(
                    "Vector4Int requires exactly 4 values, got " + array.size());
        }
        return new Vector4Int(
                array.get(0).getAsInt(),
                array.get(1).getAsInt(),
                array.get(2).getAsInt(),
                array.get(3).getAsInt());
    }

    public static Vector4Boolean parseVector4Boolean(JsonArray array) {
        if (array.size() != 4) {
            throw new IllegalArgumentException(
                    "Vector4Boolean requires exactly 4 values, got " + array.size());
        }
        return new Vector4Boolean(
                array.get(0).getAsBoolean(),
                array.get(1).getAsBoolean(),
                array.get(2).getAsBoolean(),
                array.get(3).getAsBoolean());
    }

    // ===== Matrix Parsers (Float) =====

    public static Matrix2 parseMatrix2(JsonArray array) {
        if (array.size() != 4) {
            throw new IllegalArgumentException(
                    "Matrix2 requires exactly 4 values, got " + array.size());
        }
        float[] values = new float[4];
        for (int i = 0; i < 4; i++) {
            values[i] = array.get(i).getAsFloat();
        }
        return new Matrix2(values);
    }

    public static Matrix3 parseMatrix3(JsonArray array) {
        if (array.size() != 9) {
            throw new IllegalArgumentException(
                    "Matrix3 requires exactly 9 values, got " + array.size());
        }
        float[] values = new float[9];
        for (int i = 0; i < 9; i++) {
            values[i] = array.get(i).getAsFloat();
        }
        return new Matrix3(values);
    }

    public static Matrix4 parseMatrix4(JsonArray array) {
        if (array.size() != 16) {
            throw new IllegalArgumentException(
                    "Matrix4 requires exactly 16 values, got " + array.size());
        }
        float[] values = new float[16];
        for (int i = 0; i < 16; i++) {
            values[i] = array.get(i).getAsFloat();
        }
        return new Matrix4(values);
    }

    // ===== Matrix Parsers (Double) =====

    public static Matrix2Double parseMatrix2Double(JsonArray array) {
        if (array.size() != 4) {
            throw new IllegalArgumentException(
                    "Matrix2Double requires exactly 4 values, got " + array.size());
        }
        double[] values = new double[4];
        for (int i = 0; i < 4; i++) {
            values[i] = array.get(i).getAsDouble();
        }
        return new Matrix2Double(values);
    }

    public static Matrix3Double parseMatrix3Double(JsonArray array) {
        if (array.size() != 9) {
            throw new IllegalArgumentException(
                    "Matrix3Double requires exactly 9 values, got " + array.size());
        }
        double[] values = new double[9];
        for (int i = 0; i < 9; i++) {
            values[i] = array.get(i).getAsDouble();
        }
        return new Matrix3Double(values);
    }

    public static Matrix4Double parseMatrix4Double(JsonArray array) {
        if (array.size() != 16) {
            throw new IllegalArgumentException(
                    "Matrix4Double requires exactly 16 values, got " + array.size());
        }
        double[] values = new double[16];
        for (int i = 0; i < 16; i++) {
            values[i] = array.get(i).getAsDouble();
        }
        return new Matrix4Double(values);
    }

    // ===== Array Conversion Helpers =====

    public static float[] jsonArrayToFloatArray(JsonArray array) {
        float[] result = new float[array.size()];
        for (int i = 0; i < array.size(); i++) {
            result[i] = array.get(i).getAsFloat();
        }
        return result;
    }

    public static double[] jsonArrayToDoubleArray(JsonArray array) {
        double[] result = new double[array.size()];
        for (int i = 0; i < array.size(); i++) {
            result[i] = array.get(i).getAsDouble();
        }
        return result;
    }

    public static int[] jsonArrayToIntArray(JsonArray array) {
        int[] result = new int[array.size()];
        for (int i = 0; i < array.size(); i++) {
            result[i] = array.get(i).getAsInt();
        }
        return result;
    }

    public static boolean[] jsonArrayToBooleanArray(JsonArray array) {
        boolean[] result = new boolean[array.size()];
        for (int i = 0; i < array.size(); i++) {
            result[i] = array.get(i).getAsBoolean();
        }
        return result;
    }
}
