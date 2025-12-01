package com.AdventureRPG.Core.RenderPipeline.ShaderManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.IntBuffer;

import com.AdventureRPG.Core.Util.FileUtility;
import com.AdventureRPG.Core.Util.Exceptions.FileException;
import com.AdventureRPG.Core.Util.Exceptions.GraphicException;
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

        catch (Exception e) {
            throw new FileException.FileReadException("Failed to read file: " + file, e);
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

    static ObjectArrayList<String> extractBracketBlock(File file, String startingLine) {

        ObjectArrayList<String> lines = new ObjectArrayList<>(FileUtility.readAllLines(file));
        ObjectArrayList<String> result = new ObjectArrayList<>();

        int startIndex = -1;

        // 1. Locate header line
        for (int i = 0; i < lines.size(); i++)
            if (lines.get(i).trim().equals(startingLine.trim())) {
                startIndex = i;
                break;
            }

        if (startIndex == -1)
            return result;

        // 2. Find first '{' after starting line
        int openBraceLine = -1;

        for (int i = startIndex; i < lines.size(); i++) {
            String ln = lines.get(i);
            if (ln.indexOf('{') != -1) {
                openBraceLine = i;
                break;
            }
        }

        if (openBraceLine == -1)
            return result;

        // 3. Parse block contents
        int depth = 0;
        boolean inside = false;

        for (int i = openBraceLine; i < lines.size(); i++) {

            String raw = lines.get(i);
            String clean = stripSingleLineComments(raw);

            int len = clean.length();
            for (int c = 0; c < len; c++) {
                char ch = clean.charAt(c);

                if (ch == '{') {
                    depth++;
                    if (!inside) {
                        inside = true;
                        continue;
                    }
                } else if (ch == '}') {
                    depth--;
                    if (depth == 0)
                        return result;
                }
            }

            // Only include internal block lines
            if (inside && depth >= 1 && i > openBraceLine)
                result.add(raw);
        }

        return result;
    }

    private static String stripSingleLineComments(String line) {
        int idx = line.indexOf("//");
        return idx >= 0 ? line.substring(0, idx) : line;
    }

    // Shader Program Construction
    static int createShaderProgram(ShaderDefinitionInstance shaderDef) {

        ShaderDataInstance vert = shaderDef.vert;
        ShaderDataInstance frag = shaderDef.frag;

        int vertShader = compileShader(vert);
        int fragShader = compileShader(frag);

        int program = Gdx.gl.glCreateProgram();
        if (program == 0)
            throw new GraphicException.ShaderProgramException(
                    "Failed to return a valid gpu handle for shader: " + shaderDef.shaderName);

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
            throw new GraphicException.ShaderProgramException("Failed to link shader program: " + log);
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
            throw new GraphicException.ShaderProgramException(
                    "Shader: " + shader.shaderName() + ", Contains an invalid shader ID");

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
            throw new GraphicException.ShaderProgramException(
                    "Failed to compile shader " + shader.shaderName() + ": " + log);
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
            throw new GraphicException.ShaderProgramException("Failed to read shader source: " + shader.shaderFile(),
                    e);
        }
    }

    static int getUniformHandle(int programHandle, String uniformName) {

        int handle = Gdx.gl.glGetUniformLocation(programHandle, uniformName);

        if (handle == -1) // TODO: Make my own error
            throw new GraphicException.ShaderProgramException("Uniform not found in shader program: " + uniformName);

        return handle;
    }
}
