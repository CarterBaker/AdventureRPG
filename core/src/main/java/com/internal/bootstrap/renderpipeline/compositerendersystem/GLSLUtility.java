package com.internal.bootstrap.renderpipeline.compositerendersystem;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.internal.core.engine.UtilityPackage;
import java.nio.FloatBuffer;

class GLSLUtility extends UtilityPackage {

    /*
     * Stateless OpenGL helpers for CompositeRenderSystem. Covers depth state,
     * instance VBO upload, instanced draw calls, shader binding, and UBO binding.
     * Package-private — only CompositeRenderSystem may call these.
     */

    // Depth \\

    static void enableDepth() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        Gdx.gl.glDepthMask(true);
    }

    // Upload \\

    static void updateInstanceVBO(int vbo, FloatBuffer data, int floatCount) {
        Gdx.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        Gdx.gl20.glBufferSubData(GL20.GL_ARRAY_BUFFER, 0, floatCount * Float.BYTES, data);
        Gdx.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
    }

    // Draw \\

    static void drawElementsInstanced(int vao, int indexCount, int instanceCount) {
        Gdx.gl30.glBindVertexArray(vao);
        Gdx.gl30.glDrawElementsInstanced(
                GL20.GL_TRIANGLES, indexCount, GL20.GL_UNSIGNED_SHORT, 0, instanceCount);
        Gdx.gl30.glBindVertexArray(0);
    }

    // Shader \\

    static void useShader(int shaderHandle) {
        Gdx.gl.glUseProgram(shaderHandle);
    }

    // UBO \\

    static void bindUniformBlock(int shaderProgram, String blockName, int bindingPoint) {
        int blockIndex = Gdx.gl30.glGetUniformBlockIndex(shaderProgram, blockName);
        if (blockIndex != GL30.GL_INVALID_INDEX)
            Gdx.gl30.glUniformBlockBinding(shaderProgram, blockIndex, bindingPoint);
    }

    static void bindUniformBuffer(int bindingPoint, int gpuHandle) {
        Gdx.gl30.glBindBufferBase(GL30.GL_UNIFORM_BUFFER, bindingPoint, gpuHandle);
    }
}