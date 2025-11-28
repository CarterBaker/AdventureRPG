package com.AdventureRPG.Core.RenderPipeline.ShaderManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.IntBuffer;

import com.AdventureRPG.Core.Util.Methematics.Vectors.*;
import com.AdventureRPG.Core.Util.Methematics.Matrices.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.BufferUtils;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class GLSLUtility {

    // Line Search Utilities
    static ObjectArrayList<String> findLinesStartingWith(File file, String prefix) {
        String text = readFileToString(file);
        return findLinesStartingWith(text, prefix);
    }

    static ObjectArrayList<String> findLinesContaining(File file, String needle) {
        String text = readFileToString(file);
        return findLinesContaining(text, needle);
    }

    static String readFileToString(File file) {

        try {
            return Files.readString(file.toPath());
        }

        catch (Exception e) { // TODO: Add my own error
            throw new IllegalStateException("Failed to read file: " + file, e);
        }
    }

    static ObjectArrayList<String> findLinesStartingWith(String text, String prefix) {

        ObjectArrayList<String> lines = new ObjectArrayList<>();
        int start = 0;
        int len = text.length();

        while (start < len) {

            int end = text.indexOf('\n', start);
            if (end == -1)
                end = len;

            String line = text.substring(start, end).trim();
            if (line.startsWith(prefix))
                lines.add(line);

            start = end + 1;
        }

        return lines;
    }

    static ObjectArrayList<String> findLinesContaining(String text, String needle) {

        ObjectArrayList<String> lines = new ObjectArrayList<>();
        int start = 0;
        int len = text.length();

        while (start < len) {

            int end = text.indexOf('\n', start);
            if (end == -1)
                end = len;

            String line = text.substring(start, end).trim();
            if (line.contains(needle))
                lines.add(line);

            start = end + 1;
        }

        return lines;
    }

    // Shader Program Construction
    static int createShaderProgram(ShaderDefinitionInstance shaderDef) {

        ShaderDataInstance vert = shaderDef.vert;
        ShaderDataInstance frag = shaderDef.frag;

        int vertShader = compileShader(vert);
        int fragShader = compileShader(frag);

        int program = Gdx.gl.glCreateProgram();
        if (program == 0) // TODO: Add my own error
            throw new IllegalStateException("glCreateProgram returned 0");

        Gdx.gl.glAttachShader(program, vertShader);
        Gdx.gl.glAttachShader(program, fragShader);

        // Attach included shader snippets
        for (ShaderDataInstance include : shaderDef.getIncludes()) {
            int includeShader = compileShader(include);
            Gdx.gl.glAttachShader(program, includeShader);
        }

        Gdx.gl.glLinkProgram(program);

        // Check link result
        IntBuffer statusBuf = BufferUtils.newIntBuffer(1);
        Gdx.gl.glGetProgramiv(program, GL20.GL_LINK_STATUS, statusBuf);
        statusBuf.rewind();

        if (statusBuf.get(0) == 0) {
            String log = Gdx.gl.glGetProgramInfoLog(program);
            Gdx.gl.glDeleteProgram(program);
            throw new IllegalStateException("Failed to link program: " + log);
        }

        // Cleanup
        Gdx.gl.glDetachShader(program, vertShader);
        Gdx.gl.glDetachShader(program, fragShader);

        return program;
    }

    // Shader Compilation
    private static int compileShader(ShaderDataInstance shader) {

        int type = switch (shader.shaderType()) {
            case VERT -> GL20.GL_VERTEX_SHADER;
            case FRAG -> GL20.GL_FRAGMENT_SHADER;
            case INCLUDE -> GL20.GL_VERTEX_SHADER;
        };

        int shaderID = Gdx.gl.glCreateShader(type);
        if (shaderID == 0)
            throw new IllegalStateException("glCreateShader returned 0 for " + shader.shaderName());

        String source = readShaderSource(shader);
        Gdx.gl.glShaderSource(shaderID, source);
        Gdx.gl.glCompileShader(shaderID);

        IntBuffer compiledBuf = BufferUtils.newIntBuffer(1);
        Gdx.gl.glGetShaderiv(shaderID, GL20.GL_COMPILE_STATUS, compiledBuf);
        compiledBuf.rewind();

        if (compiledBuf.get(0) == 0) {

            String log = Gdx.gl.glGetShaderInfoLog(shaderID);
            Gdx.gl.glDeleteShader(shaderID);

            // TODO: Add my own error
            throw new IllegalStateException("Failed to compile shader " + shader.shaderName() + ": " + log);
        }

        return shaderID;
    }

    // Shader Source Loading
    private static String readShaderSource(ShaderDataInstance shader) {

        // Assumes the shader file already contains merged includes
        try {
            return Files.readString(shader.shaderFile().toPath());
        }

        catch (Exception e) { // TODO: Add my own error
            throw new IllegalStateException("Failed to read shader source: " + shader.shaderFile(), e);
        }
    }

    static int getUniformHandle(int programHandle, String uniformName) {

        int handle = Gdx.gl.glGetUniformLocation(programHandle, uniformName);

        if (handle == -1) // TODO: Make my own error
            throw new IllegalStateException("Uniform not found in shader program: " + uniformName);

        return handle;
    }

    // Utilities for parsing uniform values
    private static String extractValue(String line) {
        int equalsIndex = line.indexOf('=');
        if (equalsIndex == -1)
            throw new IllegalArgumentException("Uniform line does not contain '=': " + line);
        String value = line.substring(equalsIndex + 1).trim();
        // Remove trailing semicolon
        if (value.endsWith(";"))
            value = value.substring(0, value.length() - 1).trim();
        return value;
    }

    private static String[] splitComponents(String value) {
        value = value.replaceAll("[()]", ""); // remove parentheses
        return value.split("\\s*,\\s*"); // split by commas
    }

    // --- Scalar Parsers ---
    static Float parseFloatUniform(String input) {
        String val = extractValue(input);
        return Float.parseFloat(val);
    }

    static Double parseDoubleUniform(String input) {
        String val = extractValue(input);
        return Double.parseDouble(val);
    }

    static Integer parseIntegerUniform(String input) {
        String val = extractValue(input);
        return Integer.parseInt(val);
    }

    static Boolean parseBooleanUniform(String input) {
        String val = extractValue(input);
        return val.equals("true") || val.equals("1");
    }

    // --- Vector Parsers ---
    static Vector2 parseVector2Uniform(String input) {
        String[] comps = splitComponents(extractValue(input));
        if (comps.length != 2)
            throw new IllegalArgumentException("Expected 2 components: " + input);
        return new Vector2(Float.parseFloat(comps[0]), Float.parseFloat(comps[1]));
    }

    static Vector3 parseVector3Uniform(String input) {
        String[] comps = splitComponents(extractValue(input));
        if (comps.length != 3)
            throw new IllegalArgumentException("Expected 3 components: " + input);
        return new Vector3(Float.parseFloat(comps[0]), Float.parseFloat(comps[1]), Float.parseFloat(comps[2]));
    }

    static Vector4 parseVector4Uniform(String input) {
        String[] comps = splitComponents(extractValue(input));
        if (comps.length != 4)
            throw new IllegalArgumentException("Expected 4 components: " + input);
        return new Vector4(Float.parseFloat(comps[0]), Float.parseFloat(comps[1]), Float.parseFloat(comps[2]),
                Float.parseFloat(comps[3]));
    }

    // Double precision vectors
    static Vector2Double parseVector2DoubleUniform(String input) {
        String[] comps = splitComponents(extractValue(input));
        if (comps.length != 2)
            throw new IllegalArgumentException("Expected 2 components: " + input);
        return new Vector2Double(Double.parseDouble(comps[0]), Double.parseDouble(comps[1]));
    }

    static Vector3Double parseVector3DoubleUniform(String input) {
        String[] comps = splitComponents(extractValue(input));
        if (comps.length != 3)
            throw new IllegalArgumentException("Expected 3 components: " + input);
        return new Vector3Double(Double.parseDouble(comps[0]), Double.parseDouble(comps[1]),
                Double.parseDouble(comps[2]));
    }

    static Vector4Double parseVector4DoubleUniform(String input) {
        String[] comps = splitComponents(extractValue(input));
        if (comps.length != 4)
            throw new IllegalArgumentException("Expected 4 components: " + input);
        return new Vector4Double(Double.parseDouble(comps[0]), Double.parseDouble(comps[1]),
                Double.parseDouble(comps[2]), Double.parseDouble(comps[3]));
    }

    // Integer vectors
    static Vector2Int parseVector2IntUniform(String input) {
        String[] comps = splitComponents(extractValue(input));
        if (comps.length != 2)
            throw new IllegalArgumentException("Expected 2 components: " + input);
        return new Vector2Int(Integer.parseInt(comps[0]), Integer.parseInt(comps[1]));
    }

    static Vector3Int parseVector3IntUniform(String input) {
        String[] comps = splitComponents(extractValue(input));
        if (comps.length != 3)
            throw new IllegalArgumentException("Expected 3 components: " + input);
        return new Vector3Int(Integer.parseInt(comps[0]), Integer.parseInt(comps[1]), Integer.parseInt(comps[2]));
    }

    static Vector4Int parseVector4IntUniform(String input) {
        String[] comps = splitComponents(extractValue(input));
        if (comps.length != 4)
            throw new IllegalArgumentException("Expected 4 components: " + input);
        return new Vector4Int(Integer.parseInt(comps[0]), Integer.parseInt(comps[1]), Integer.parseInt(comps[2]),
                Integer.parseInt(comps[3]));
    }

    // Boolean vectors
    static Vector2Boolean parseVector2BooleanUniform(String input) {
        String[] comps = splitComponents(extractValue(input));
        if (comps.length != 2)
            throw new IllegalArgumentException("Expected 2 components: " + input);
        return new Vector2Boolean(Boolean.parseBoolean(comps[0]), Boolean.parseBoolean(comps[1]));
    }

    static Vector3Boolean parseVector3BooleanUniform(String input) {
        String[] comps = splitComponents(extractValue(input));
        if (comps.length != 3)
            throw new IllegalArgumentException("Expected 3 components: " + input);
        return new Vector3Boolean(Boolean.parseBoolean(comps[0]), Boolean.parseBoolean(comps[1]),
                Boolean.parseBoolean(comps[2]));
    }

    static Vector4Boolean parseVector4BooleanUniform(String input) {
        String[] comps = splitComponents(extractValue(input));
        if (comps.length != 4)
            throw new IllegalArgumentException("Expected 4 components: " + input);
        return new Vector4Boolean(Boolean.parseBoolean(comps[0]), Boolean.parseBoolean(comps[1]),
                Boolean.parseBoolean(comps[2]), Boolean.parseBoolean(comps[3]));
    }

    // --- Matrix Parsers ---
    private static float[] parseFloatArray(String input, int expectedLength) {
        String[] comps = splitComponents(extractValue(input));
        if (comps.length != expectedLength)
            throw new IllegalArgumentException("Expected " + expectedLength + " components: " + input);
        float[] arr = new float[expectedLength];
        for (int i = 0; i < expectedLength; i++)
            arr[i] = Float.parseFloat(comps[i]);
        return arr;
    }

    static Matrix2 parseMatrix2Uniform(String input) {
        float[] arr = parseFloatArray(input, 4);
        return new Matrix2(arr);
    }

    static Matrix3 parseMatrix3Uniform(String input) {
        float[] arr = parseFloatArray(input, 9);
        return new Matrix3(arr);
    }

    static Matrix4 parseMatrix4Uniform(String input) {
        float[] arr = parseFloatArray(input, 16);
        return new Matrix4(arr);
    }

    // Double matrices
    private static double[] parseDoubleArray(String input, int expectedLength) {
        String[] comps = splitComponents(extractValue(input));
        if (comps.length != expectedLength)
            throw new IllegalArgumentException("Expected " + expectedLength + " components: " + input);
        double[] arr = new double[expectedLength];
        for (int i = 0; i < expectedLength; i++)
            arr[i] = Double.parseDouble(comps[i]);
        return arr;
    }

    static Matrix2Double parseMatrix2DoubleUniform(String input) {
        double[] arr = parseDoubleArray(input, 4);
        return new Matrix2Double(arr);
    }

    static Matrix3Double parseMatrix3DoubleUniform(String input) {
        double[] arr = parseDoubleArray(input, 9);
        return new Matrix3Double(arr);
    }

    static Matrix4Double parseMatrix4DoubleUniform(String input) {
        double[] arr = parseDoubleArray(input, 16);
        return new Matrix4Double(arr);
    }

    // --- Sampler / Image Parsers ---
    static String parseSampleImage2DUniform(String input) {
        String val = extractValue(input);
        return val.replaceAll("[\"']", "");
    }

    static String parseSampleImage2DArrayUniform(String input) {
        String val = extractValue(input);
        return val.replaceAll("[\"']", "");
    }

}
