package application.bootstrap.shaderpipeline.shadermanager;

import java.nio.IntBuffer;

import application.bootstrap.shaderpipeline.shader.ShaderSourceStruct;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.graphics.gl.GL20;
import engine.util.graphics.gl.GL30;
import engine.util.memory.BufferUtils;
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

        int program = EngineContext.gl20.glCreateProgram();

        if (program == 0)
            throwException("Failed to create shader program: " + assembly.getShaderName());

        EngineContext.gl20.glAttachShader(program, vertShader);
        EngineContext.gl20.glAttachShader(program, fragShader);
        EngineContext.gl20.glLinkProgram(program);

        IntBuffer statusBuf = BufferUtils.newIntBuffer(1);
        EngineContext.gl20.glGetProgramiv(program, GL20.GL_LINK_STATUS, statusBuf);
        statusBuf.rewind();

        if (statusBuf.get(0) == 0) {
            String log = EngineContext.gl20.glGetProgramInfoLog(program);
            EngineContext.gl20.glDeleteProgram(program);
            EngineContext.gl20.glDeleteShader(vertShader);
            EngineContext.gl20.glDeleteShader(fragShader);
            throwException("Failed to link shader " + assembly.getShaderName() + ": " + log);
        }

        EngineContext.gl20.glDetachShader(program, vertShader);
        EngineContext.gl20.glDetachShader(program, fragShader);
        EngineContext.gl20.glDeleteShader(vertShader);
        EngineContext.gl20.glDeleteShader(fragShader);

        return program;
    }

    // Shader Compilation \\

    private static int compileShaderFromSource(int type, String source, String shaderName) {

        int shaderID = EngineContext.gl20.glCreateShader(type);

        if (shaderID == 0)
            throwException("Invalid shader ID for: " + shaderName);

        EngineContext.gl20.glShaderSource(shaderID, source);
        EngineContext.gl20.glCompileShader(shaderID);

        IntBuffer compiled = BufferUtils.newIntBuffer(1);
        EngineContext.gl20.glGetShaderiv(shaderID, GL20.GL_COMPILE_STATUS, compiled);
        compiled.rewind();

        if (compiled.get(0) == 0) {
            String log = EngineContext.gl20.glGetShaderInfoLog(shaderID);
            EngineContext.gl20.glDeleteShader(shaderID);
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
        return EngineContext.gl20.glGetUniformLocation(programHandle, uniformName);
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
            EngineContext.gl20.glDeleteProgram(programHandle);
    }
}
