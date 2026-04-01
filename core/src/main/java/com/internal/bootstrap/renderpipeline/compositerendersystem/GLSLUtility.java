package com.internal.bootstrap.renderpipeline.compositerendersystem;

import com.internal.core.app.CoreContext;
import com.internal.core.util.graphics.gl.GL20;
import com.internal.core.util.graphics.gl.GL30;
import com.internal.core.engine.UtilityPackage;
import com.internal.core.settings.EngineSetting;

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
        CoreContext.gl.glEnable(GL20.GL_DEPTH_TEST);
        CoreContext.gl.glDepthFunc(GL20.GL_LEQUAL);
        CoreContext.gl.glDepthMask(true);
    }

    // Upload \\

    static void updateInstanceVBO(int vbo, FloatBuffer data, int floatCount) {
        CoreContext.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        CoreContext.gl20.glBufferSubData(GL20.GL_ARRAY_BUFFER, 0, floatCount * Float.BYTES, data);
        CoreContext.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
    }

    static int createDynamicInstanceVBO(int maxInstances, int floatsPerInstance) {
        int vbo = CoreContext.gl20.glGenBuffer();
        CoreContext.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        CoreContext.gl20.glBufferData(
                GL20.GL_ARRAY_BUFFER,
                maxInstances * floatsPerInstance * Float.BYTES,
                null,
                GL20.GL_DYNAMIC_DRAW);
        CoreContext.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
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
        CoreContext.gl30.glGenVertexArrays(1, idBuffer);
        int vao = idBuffer.get(0);

        CoreContext.gl30.glBindVertexArray(vao);
        CoreContext.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, meshVBOHandle);

        int meshStride = 0;
        for (int i = 0; i < meshAttrSizes.length; i++)
            meshStride += meshAttrSizes[i];
        int meshStrideBytes = meshStride * Float.BYTES;

        int meshOffsetBytes = 0;
        for (int i = 0; i < meshAttrSizes.length; i++) {
            CoreContext.gl20.glEnableVertexAttribArray(i);
            CoreContext.gl20.glVertexAttribPointer(i, meshAttrSizes[i], GL20.GL_FLOAT, false, meshStrideBytes, meshOffsetBytes);
            meshOffsetBytes += meshAttrSizes[i] * Float.BYTES;
        }

        int instanceStride = 0;
        for (int i = 0; i < instanceAttrSizes.length; i++)
            instanceStride += instanceAttrSizes[i];
        int instanceStrideBytes = instanceStride * Float.BYTES;

        CoreContext.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, instanceVBOHandle);

        int instanceOffsetBytes = 0;
        for (int i = 0; i < instanceAttrSizes.length; i++) {
            int location = meshAttrSizes.length + i;
            CoreContext.gl20.glEnableVertexAttribArray(location);
            CoreContext.gl20.glVertexAttribPointer(
                    location,
                    instanceAttrSizes[i],
                    GL20.GL_FLOAT,
                    false,
                    instanceStrideBytes,
                    instanceOffsetBytes);
            CoreContext.gl30.glVertexAttribDivisor(location, 1);
            instanceOffsetBytes += instanceAttrSizes[i] * Float.BYTES;
        }

        CoreContext.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, meshIBOHandle);
        CoreContext.gl30.glBindVertexArray(0);
        CoreContext.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);

        return vao;
    }

    // Draw \\

    static void drawElementsInstanced(int vao, int indexCount, int instanceCount) {
        CoreContext.gl30.glBindVertexArray(vao);
        CoreContext.gl30.glDrawElementsInstanced(
                GL20.GL_TRIANGLES, indexCount, GL20.GL_UNSIGNED_SHORT, 0, instanceCount);
        CoreContext.gl30.glBindVertexArray(0);
    }

    // Shader \\

    static void useShader(int shaderHandle) {
        CoreContext.gl.glUseProgram(shaderHandle);
    }

    // UBO \\

    static void bindUniformBlock(int shaderProgram, String blockName, int bindingPoint) {
        int blockIndex = CoreContext.gl30.glGetUniformBlockIndex(shaderProgram, blockName);
        if (blockIndex != EngineSetting.GL_INVALID_INDEX)
            CoreContext.gl30.glUniformBlockBinding(shaderProgram, blockIndex, bindingPoint);
    }

    static void bindUniformBuffer(int bindingPoint, int gpuHandle) {
        CoreContext.gl30.glBindBufferBase(GL30.GL_UNIFORM_BUFFER, bindingPoint, gpuHandle);
    }

    static void deleteBuffer(int handle) {
        if (handle != 0)
            CoreContext.gl20.glDeleteBuffer(handle);
    }

    static void deleteVAO(int handle) {
        if (handle == 0)
            return;
        IntBuffer idBuffer = ByteBuffer.allocateDirect(Integer.BYTES)
                .order(ByteOrder.nativeOrder()).asIntBuffer();
        idBuffer.put(handle).flip();
        CoreContext.gl30.glDeleteVertexArrays(1, idBuffer);
    }
}
