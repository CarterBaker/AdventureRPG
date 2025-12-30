package com.AdventureRPG.core.shaderpipeline.shadermanager;

import java.nio.file.Files;
import java.nio.IntBuffer;

import com.AdventureRPG.core.engine.UtiityPackage;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.utils.BufferUtils;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class GLSLUtility extends UtiityPackage {

    // Shader Program Construction
    static int createShaderProgram(ShaderDefinitionData shaderDef) {

        // Preprocess shaders to replace #include directives with actual content
        String vertSource = preprocessShaderSource(shaderDef, shaderDef.vert);
        String fragSource = preprocessShaderSource(shaderDef, shaderDef.frag);

        // Compile preprocessed shaders
        int vertShader = compileShaderFromSource(GL20.GL_VERTEX_SHADER, vertSource, shaderDef.vert.shaderName());
        int fragShader = compileShaderFromSource(GL20.GL_FRAGMENT_SHADER, fragSource, shaderDef.frag.shaderName());

        int program = Gdx.gl.glCreateProgram();
        if (program == 0)
            throwException(
                    "Failed to return a valid gpu handle for shader: " + shaderDef.shaderName);

        Gdx.gl.glAttachShader(program, vertShader);
        Gdx.gl.glAttachShader(program, fragShader);
        Gdx.gl.glLinkProgram(program);

        // Check link result
        IntBuffer statusBuf = BufferUtils.newIntBuffer(1);
        Gdx.gl.glGetProgramiv(program, GL20.GL_LINK_STATUS, statusBuf);
        statusBuf.rewind();

        if (statusBuf.get(0) == 0) {
            String log = Gdx.gl.glGetProgramInfoLog(program);
            Gdx.gl.glDeleteProgram(program);
            Gdx.gl.glDeleteShader(vertShader);
            Gdx.gl.glDeleteShader(fragShader);
            throwException(
                    "Failed to link shader program " + shaderDef.shaderName + ": " + log);
        }

        // Cleanup - detach and delete shaders
        Gdx.gl.glDetachShader(program, vertShader);
        Gdx.gl.glDetachShader(program, fragShader);
        Gdx.gl.glDeleteShader(vertShader);
        Gdx.gl.glDeleteShader(fragShader);

        return program;
    }

    // Shader Compilation
    private static int compileShaderFromSource(int type, String source, String shaderName) {

        int shaderID = Gdx.gl.glCreateShader(type);
        if (shaderID == 0)
            throwException(
                    "Shader: " + shaderName + ", Contains an invalid shader ID");

        Gdx.gl.glShaderSource(shaderID, source);
        Gdx.gl.glCompileShader(shaderID);

        IntBuffer compiledBuf = BufferUtils.newIntBuffer(1);
        Gdx.gl.glGetShaderiv(shaderID, GL20.GL_COMPILE_STATUS, compiledBuf);
        compiledBuf.rewind();

        if (compiledBuf.get(0) == 0) {
            String log = Gdx.gl.glGetShaderInfoLog(shaderID);
            Gdx.gl.glDeleteShader(shaderID);
            throwException(
                    "Failed to compile shader " + shaderName + ": " + log);
        }

        return shaderID;
    }

    static String preprocessShaderSource(ShaderDefinitionData shaderDefinition, ShaderData shaderData) {
        StringBuilder result = new StringBuilder();

        // 1. Add version directive first (only from main shader, not includes)
        String version = shaderData.getVersion();
        if (version != null && !version.isEmpty()) {
            result.append(version).append("\n\n");
        }

        // 2. Add all includes' content (in the order they were collected)
        ObjectArrayList<ShaderData> includes = shaderDefinition.getIncludes();
        for (int i = 0; i < includes.size(); i++) {
            ShaderData include = includes.get(i);
            String includeSource = FileParserUtility.convertFileToRawText(include.shaderFile());

            // Remove #version and #include directives from included files
            includeSource = stripPreprocessorDirectives(includeSource);

            result.append("// -------- Include: ").append(include.shaderName()).append(" --------\n");
            result.append(includeSource).append("\n");
            result.append("// -------- End Include --------\n\n");
        }

        // 3. Add main shader source (without #version and #include directives)
        String mainSource = FileParserUtility.convertFileToRawText(shaderData.shaderFile());
        mainSource = stripPreprocessorDirectives(mainSource);

        result.append(mainSource);

        return result.toString();
    }

    private static String stripPreprocessorDirectives(String source) {
        // Remove #version directives
        source = source.replaceAll("(?m)^\\s*#version\\s+.*$", "");
        // Remove #include directives
        source = source.replaceAll("(?m)^\\s*#include\\s+.*$", "");
        // Clean up multiple blank lines
        source = source.replaceAll("\n{3,}", "\n\n");
        return source.trim();
    }

    static int getUniformHandle(int programHandle, String uniformName) {

        int handle = Gdx.gl.glGetUniformLocation(programHandle, uniformName);

        if (handle == -1) // TODO: Make my own error
            throwException("Uniform not found in shader program: " + uniformName);

        return handle;
    }

    // Create a uniform buffer object
    static int createUniformBuffer() {
        IntBuffer buffer = BufferUtils.newIntBuffer(1);
        Gdx.gl30.glGenBuffers(1, buffer);
        buffer.rewind();
        return buffer.get(0);
    }

    // Bind uniform buffer to a binding point
    static void bindUniformBuffer(int bufferHandle, int bindingPoint) {
        Gdx.gl30.glBindBufferBase(GL30.GL_UNIFORM_BUFFER, bindingPoint, bufferHandle);
    }

    static void allocateUniformBuffer(int bufferHandle, int sizeInBytes) {
        Gdx.gl30.glBindBuffer(GL30.GL_UNIFORM_BUFFER, bufferHandle);
        Gdx.gl30.glBufferData(GL30.GL_UNIFORM_BUFFER, sizeInBytes, null, GL30.GL_DYNAMIC_DRAW);
        Gdx.gl30.glBindBuffer(GL30.GL_UNIFORM_BUFFER, 0);
    }

    static void deleteShaderProgram(int programHandle) {
        if (programHandle != 0) {
            Gdx.gl.glDeleteProgram(programHandle);
        }
    }

    static void deleteUniformBuffer(int bufferHandle) {
        if (bufferHandle != 0) {
            IntBuffer buffer = BufferUtils.newIntBuffer(1);
            buffer.put(0, bufferHandle);
            Gdx.gl30.glDeleteBuffers(1, buffer);
        }
    }

    static void bindUniformBlock(
            int programHandle,
            String blockName,
            int bindingPoint) {
        int blockIndex = Gdx.gl30.glGetUniformBlockIndex(programHandle, blockName);

        if (blockIndex == -1)
            throwException(
                    "Uniform block not found in shader program: " + blockName);

        Gdx.gl30.glUniformBlockBinding(
                programHandle,
                blockIndex,
                bindingPoint);
    }
}