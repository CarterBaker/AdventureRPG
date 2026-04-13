package program.bootstrap.shaderpipeline.shadermanager;

import program.core.engine.EngineContext;

import java.nio.IntBuffer;

import program.core.util.graphics.gl.GL20;
import program.core.util.graphics.gl.GL30;
import program.core.util.memory.BufferUtils;
import program.bootstrap.shaderpipeline.shader.ShaderSourceStruct;
import program.core.engine.EngineUtility;
import program.core.settings.EngineSetting;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * GL20/GL30 wrapper for shader program construction, source preprocessing,
 * uniform location queries, UBO block binding, and program deletion.
 * Stateless — all methods are package-private statics.
 */
class GLSLUtility extends EngineUtility {

    // Shader Program Construction \\

    static int createShaderProgram(ShaderSourceStruct assembly) {

        String vertSource = preprocessShaderSource(assembly, assembly.getVert());
        String fragSource = preprocessShaderSource(assembly, assembly.getFrag());

        int vertShader = compileShaderFromSource(
                GL20.GL_VERTEX_SHADER,
                vertSource,
                assembly.getVert().getShaderName());
        int fragShader = compileShaderFromSource(
                GL20.GL_FRAGMENT_SHADER,
                fragSource,
                assembly.getFrag().getShaderName());

        int program = EngineContext.gl.glCreateProgram();

        if (program == 0)
            throwException("Failed to create shader program: " + assembly.getShaderName());

        EngineContext.gl.glAttachShader(program, vertShader);
        EngineContext.gl.glAttachShader(program, fragShader);
        EngineContext.gl.glLinkProgram(program);

        IntBuffer statusBuf = BufferUtils.newIntBuffer(1);
        EngineContext.gl.glGetProgramiv(program, GL20.GL_LINK_STATUS, statusBuf);
        statusBuf.rewind();

        if (statusBuf.get(0) == 0) {
            String log = EngineContext.gl.glGetProgramInfoLog(program);
            EngineContext.gl.glDeleteProgram(program);
            EngineContext.gl.glDeleteShader(vertShader);
            EngineContext.gl.glDeleteShader(fragShader);
            throwException("Failed to link shader " + assembly.getShaderName() + ": " + log);
        }

        EngineContext.gl.glDetachShader(program, vertShader);
        EngineContext.gl.glDetachShader(program, fragShader);
        EngineContext.gl.glDeleteShader(vertShader);
        EngineContext.gl.glDeleteShader(fragShader);

        return program;
    }

    // Shader Compilation \\

    private static int compileShaderFromSource(int type, String source, String shaderName) {

        int shaderID = EngineContext.gl.glCreateShader(type);

        if (shaderID == 0)
            throwException("Invalid shader ID for: " + shaderName);

        EngineContext.gl.glShaderSource(shaderID, source);
        EngineContext.gl.glCompileShader(shaderID);

        IntBuffer compiled = BufferUtils.newIntBuffer(1);
        EngineContext.gl.glGetShaderiv(shaderID, GL20.GL_COMPILE_STATUS, compiled);
        compiled.rewind();

        if (compiled.get(0) == 0) {
            String log = EngineContext.gl.glGetShaderInfoLog(shaderID);
            EngineContext.gl.glDeleteShader(shaderID);
            throwException("Failed to compile shader " + shaderName + ": " + log);
        }

        return shaderID;
    }

    // Source Preprocessing \\

    static String preprocessShaderSource(ShaderSourceStruct assembly, ShaderSourceStruct source) {

        StringBuilder result = new StringBuilder();

        String version = source.getVersion();

        if (version != null && !version.isEmpty())
            result.append(version).append("\n\n");

        ObjectArrayList<ShaderSourceStruct> includes = assembly.getFlattenedIncludes();

        for (int i = 0; i < includes.size(); i++) {
            ShaderSourceStruct include = includes.get(i);
            String includeSource = stripDirectives(
                    FileParserUtility.convertFileToRawText(include.getShaderFile()));
            result.append("// -------- Include: ").append(include.getShaderName()).append(" --------\n");
            result.append(includeSource).append("\n");
            result.append("// -------- End Include --------\n\n");
        }

        result.append(stripDirectives(FileParserUtility.convertFileToRawText(source.getShaderFile())));

        return result.toString();
    }

    private static String stripDirectives(String source) {
        source = source.replaceAll("(?m)^\\s*#version\\s+.*$", "");
        source = source.replaceAll("(?m)^\\s*#include\\s+.*$", "");
        source = source.replaceAll("\n{3,}", "\n\n");
        return source.trim();
    }

    // Uniform Location \\

    /*
     * Returns -1 if the driver removed the uniform as unused — not an error.
     * Callers store -1 and no-op on upload when location is -1.
     */
    static int getUniformLocation(int programHandle, String uniformName) {
        return EngineContext.gl.glGetUniformLocation(programHandle, uniformName);
    }

    // UBO Block Binding \\

    static void bindUniformBlock(int programHandle, String blockName, int bindingPoint) {

        int blockIndex = EngineContext.gl30.glGetUniformBlockIndex(programHandle, blockName);

        if (blockIndex == EngineSetting.GL_INVALID_INDEX)
            throwException("Uniform block not found in shader program: " + blockName);

        EngineContext.gl30.glUniformBlockBinding(programHandle, blockIndex, bindingPoint);
    }

    // Shader Disposal \\

    static void deleteShaderProgram(int programHandle) {
        if (programHandle != 0)
            EngineContext.gl.glDeleteProgram(programHandle);
    }
}
