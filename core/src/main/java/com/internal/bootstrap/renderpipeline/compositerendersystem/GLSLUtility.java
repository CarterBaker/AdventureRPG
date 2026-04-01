package com.internal.bootstrap.renderpipeline.compositerendersystem;

import com.internal.platform.PlatformRuntime;
import com.internal.platform.graphics.GL20;
import com.internal.platform.graphics.GL30;
import com.internal.core.engine.UtilityPackage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

class GLSLUtility extends UtilityPackage {

    /*
     * Stateless OpenGL helpers for CompositeRenderSystem. Covers depth state,
     * instance VBO upload, instanced draw calls, shader binding, and UBO binding.
     * Package-private — only CompositeRenderSystem may call these.
     */

    // Depth \\

    static void enableDepth() {
        PlatformRuntime.gl.glEnable(GL20.GL_DEPTH_TEST);
        PlatformRuntime.gl.glDepthFunc(GL20.GL_LEQUAL);
        PlatformRuntime.gl.glDepthMask(true);
    }

    // Upload \\

    static void updateInstanceVBO(int vbo, FloatBuffer data, int floatCount) {
        PlatformRuntime.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        PlatformRuntime.gl20.glBufferSubData(GL20.GL_ARRAY_BUFFER, 0, floatCount * Float.BYTES, data);
        PlatformRuntime.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
    }

    static int createDynamicInstanceVBO(int maxInstances, int floatsPerInstance) {
        int vbo = PlatformRuntime.gl20.glGenBuffer();
        PlatformRuntime.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        PlatformRuntime.gl20.glBufferData(
                GL20.GL_ARRAY_BUFFER,
                maxInstances * floatsPerInstance * Float.BYTES,
                null,
                GL20.GL_DYNAMIC_DRAW);
        PlatformRuntime.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
        return vbo;
    }

    static int createInstancedVAO(
            int meshVBOHandle,
            int[] meshAttrSizes,
            int meshIBOHandle,
            int instanceVBOHandle,
            int[] instanceAttrSizes) {

        IntBuffer idBuffer = ByteBuffer.allocateDirect(Integer.BYTES)
                .order(ByteOrder.nativeOrder()).asIntBuffer();
        PlatformRuntime.gl30.glGenVertexArrays(1, idBuffer);
        int vao = idBuffer.get(0);

        PlatformRuntime.gl30.glBindVertexArray(vao);
        PlatformRuntime.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, meshVBOHandle);

        int meshStride = 0;
        for (int i = 0; i < meshAttrSizes.length; i++)
            meshStride += meshAttrSizes[i];
        int meshStrideBytes = meshStride * Float.BYTES;

        int meshOffsetBytes = 0;
        for (int i = 0; i < meshAttrSizes.length; i++) {
            PlatformRuntime.gl20.glEnableVertexAttribArray(i);
            PlatformRuntime.gl20.glVertexAttribPointer(i, meshAttrSizes[i], GL20.GL_FLOAT, false, meshStrideBytes, meshOffsetBytes);
            meshOffsetBytes += meshAttrSizes[i] * Float.BYTES;
        }

        int instanceStride = 0;
        for (int i = 0; i < instanceAttrSizes.length; i++)
            instanceStride += instanceAttrSizes[i];
        int instanceStrideBytes = instanceStride * Float.BYTES;

        PlatformRuntime.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, instanceVBOHandle);

        int instanceOffsetBytes = 0;
        for (int i = 0; i < instanceAttrSizes.length; i++) {
            int location = meshAttrSizes.length + i;
            PlatformRuntime.gl20.glEnableVertexAttribArray(location);
            PlatformRuntime.gl20.glVertexAttribPointer(
                    location,
                    instanceAttrSizes[i],
                    GL20.GL_FLOAT,
                    false,
                    instanceStrideBytes,
                    instanceOffsetBytes);
            PlatformRuntime.gl30.glVertexAttribDivisor(location, 1);
            instanceOffsetBytes += instanceAttrSizes[i] * Float.BYTES;
        }

        PlatformRuntime.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, meshIBOHandle);
        PlatformRuntime.gl30.glBindVertexArray(0);
        PlatformRuntime.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);

        return vao;
    }

    // Draw \\

    static void drawElementsInstanced(int vao, int indexCount, int instanceCount) {
        PlatformRuntime.gl30.glBindVertexArray(vao);
        PlatformRuntime.gl30.glDrawElementsInstanced(
                GL20.GL_TRIANGLES, indexCount, GL20.GL_UNSIGNED_SHORT, 0, instanceCount);
        PlatformRuntime.gl30.glBindVertexArray(0);
    }

    // Shader \\

    static void useShader(int shaderHandle) {
        PlatformRuntime.gl.glUseProgram(shaderHandle);
    }

    // UBO \\

    static void bindUniformBlock(int shaderProgram, String blockName, int bindingPoint) {
        int blockIndex = PlatformRuntime.gl30.glGetUniformBlockIndex(shaderProgram, blockName);
        if (blockIndex != GL30.GL_INVALID_INDEX)
            PlatformRuntime.gl30.glUniformBlockBinding(shaderProgram, blockIndex, bindingPoint);
    }

    static void bindUniformBuffer(int bindingPoint, int gpuHandle) {
        PlatformRuntime.gl30.glBindBufferBase(GL30.GL_UNIFORM_BUFFER, bindingPoint, gpuHandle);
    }

    static void deleteBuffer(int handle) {
        if (handle != 0)
            PlatformRuntime.gl20.glDeleteBuffer(handle);
    }

    static void deleteVAO(int handle) {
        if (handle == 0)
            return;
        IntBuffer idBuffer = ByteBuffer.allocateDirect(Integer.BYTES)
                .order(ByteOrder.nativeOrder()).asIntBuffer();
        idBuffer.put(handle).flip();
        PlatformRuntime.gl30.glDeleteVertexArrays(1, idBuffer);
    }
}
