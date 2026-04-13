package program.bootstrap.renderpipeline.compositerendersystem;

import program.core.engine.EngineContext;
import program.core.util.graphics.gl.GL20;
import program.core.util.graphics.gl.GL30;
import program.core.engine.EngineUtility;
import program.core.settings.EngineSetting;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

class GLSLUtility extends EngineUtility {

    /*
     * Stateless OpenGL helpers for CompositeRenderSystem. Covers depth state,
     * instance VBO upload, instanced draw calls, shader binding, and UBO binding.
     * Package-private — only CompositeRenderSystem may call these.
     */

    // Depth \\

    static void enableDepth() {
        EngineContext.gl.glEnable(GL20.GL_DEPTH_TEST);
        EngineContext.gl.glDepthFunc(GL20.GL_LEQUAL);
        EngineContext.gl.glDepthMask(true);
    }

    // Upload \\

    static void updateInstanceVBO(int vbo, FloatBuffer data, int floatCount) {
        EngineContext.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        EngineContext.gl20.glBufferSubData(GL20.GL_ARRAY_BUFFER, 0, floatCount * Float.BYTES, data);
        EngineContext.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
    }

    static int createDynamicInstanceVBO(int maxInstances, int floatsPerInstance) {
        int vbo = EngineContext.gl20.glGenBuffer();
        EngineContext.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        EngineContext.gl20.glBufferData(
                GL20.GL_ARRAY_BUFFER,
                maxInstances * floatsPerInstance * Float.BYTES,
                null,
                GL20.GL_DYNAMIC_DRAW);
        EngineContext.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
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
        EngineContext.gl30.glGenVertexArrays(1, idBuffer);
        int vao = idBuffer.get(0);

        EngineContext.gl30.glBindVertexArray(vao);
        EngineContext.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, meshVBOHandle);

        int meshStride = 0;
        for (int i = 0; i < meshAttrSizes.length; i++)
            meshStride += meshAttrSizes[i];
        int meshStrideBytes = meshStride * Float.BYTES;

        int meshOffsetBytes = 0;
        for (int i = 0; i < meshAttrSizes.length; i++) {
            EngineContext.gl20.glEnableVertexAttribArray(i);
            EngineContext.gl20.glVertexAttribPointer(i, meshAttrSizes[i], GL20.GL_FLOAT, false, meshStrideBytes,
                    meshOffsetBytes);
            meshOffsetBytes += meshAttrSizes[i] * Float.BYTES;
        }

        int instanceStride = 0;
        for (int i = 0; i < instanceAttrSizes.length; i++)
            instanceStride += instanceAttrSizes[i];
        int instanceStrideBytes = instanceStride * Float.BYTES;

        EngineContext.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, instanceVBOHandle);

        int instanceOffsetBytes = 0;
        for (int i = 0; i < instanceAttrSizes.length; i++) {
            int location = meshAttrSizes.length + i;
            EngineContext.gl20.glEnableVertexAttribArray(location);
            EngineContext.gl20.glVertexAttribPointer(
                    location,
                    instanceAttrSizes[i],
                    GL20.GL_FLOAT,
                    false,
                    instanceStrideBytes,
                    instanceOffsetBytes);
            EngineContext.gl30.glVertexAttribDivisor(location, 1);
            instanceOffsetBytes += instanceAttrSizes[i] * Float.BYTES;
        }

        EngineContext.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, meshIBOHandle);
        EngineContext.gl30.glBindVertexArray(0);
        EngineContext.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);

        return vao;
    }

    // Draw \\

    static void drawElementsInstanced(int vao, int indexCount, int instanceCount) {
        EngineContext.gl30.glBindVertexArray(vao);
        EngineContext.gl30.glDrawElementsInstanced(
                GL20.GL_TRIANGLES, indexCount, GL20.GL_UNSIGNED_SHORT, 0, instanceCount);
        EngineContext.gl30.glBindVertexArray(0);
    }

    // Shader \\

    static void useShader(int shaderHandle) {
        EngineContext.gl.glUseProgram(shaderHandle);
    }

    // UBO \\

    static void bindUniformBlock(int shaderProgram, String blockName, int bindingPoint) {
        int blockIndex = EngineContext.gl30.glGetUniformBlockIndex(shaderProgram, blockName);
        if (blockIndex != EngineSetting.GL_INVALID_INDEX)
            EngineContext.gl30.glUniformBlockBinding(shaderProgram, blockIndex, bindingPoint);
    }

    static void bindUniformBuffer(int bindingPoint, int gpuHandle) {
        EngineContext.gl30.glBindBufferBase(GL30.GL_UNIFORM_BUFFER, bindingPoint, gpuHandle);
    }

    static void deleteBuffer(int handle) {
        if (handle != 0)
            EngineContext.gl20.glDeleteBuffer(handle);
    }

    static void deleteVAO(int handle) {
        if (handle == 0)
            return;
        IntBuffer idBuffer = ByteBuffer.allocateDirect(Integer.BYTES)
                .order(ByteOrder.nativeOrder()).asIntBuffer();
        idBuffer.put(handle).flip();
        EngineContext.gl30.glDeleteVertexArrays(1, idBuffer);
    }
}
