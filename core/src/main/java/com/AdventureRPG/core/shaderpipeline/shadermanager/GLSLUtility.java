package com.AdventureRPG.core.shaderpipeline.shadermanager;

import java.nio.file.Files;
import java.nio.IntBuffer;

import com.AdventureRPG.core.util.Exceptions.GraphicException;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.utils.BufferUtils;

import it.unimi.dsi.fastutil.ints.IntArrayList;

class GLSLUtility {

    // Shader Program Construction
    static int createShaderProgram(ShaderDefinitionInstance shaderDef) {

        ShaderData vert = shaderDef.vert;
        ShaderData frag = shaderDef.frag;

        int vertShader = compileShader(vert);
        int fragShader = compileShader(frag);

        int program = Gdx.gl.glCreateProgram();
        if (program == 0)
            throw new GraphicException.ShaderProgramException(
                    "Failed to return a valid gpu handle for shader: " + shaderDef.shaderName);

        Gdx.gl.glAttachShader(program, vertShader);
        Gdx.gl.glAttachShader(program, fragShader);

        // Track include shaders for cleanup
        IntArrayList includeShaders = new IntArrayList();

        // Attach included shader snippets
        for (ShaderData include : shaderDef.getIncludes()) {
            int includeShader = compileShader(include);
            Gdx.gl.glAttachShader(program, includeShader);
            includeShaders.add(includeShader);
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

        // Cleanup - detach and delete all shaders
        Gdx.gl.glDetachShader(program, vertShader);
        Gdx.gl.glDetachShader(program, fragShader);
        Gdx.gl.glDeleteShader(vertShader);
        Gdx.gl.glDeleteShader(fragShader);

        // Cleanup includes
        for (int i = 0; i < includeShaders.size(); i++) {
            int includeShader = includeShaders.getInt(i);
            Gdx.gl.glDetachShader(program, includeShader);
            Gdx.gl.glDeleteShader(includeShader);
        }

        return program;
    }

    // Shader Compilation
    private static int compileShader(ShaderData shader) {

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
    private static String readShaderSource(ShaderData shader) {

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
}
