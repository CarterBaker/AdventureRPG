package com.internal.bootstrap.renderpipeline.compositerendersystem;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;

import java.nio.FloatBuffer;

/*
 * GL operations for CompositeRenderSystem — upload, draw, shader and UBO binding.
 * Package-private — only CompositeRenderSystem may call these.
 */
class GLSLUtility {

    // Depth \\

    static void enableDepth() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        Gdx.gl.glDepthMask(true);
    }

    // Upload \\

    static void updateInstanceVBO(int vbo, FloatBuffer data, int floatCount) {
        GL20 gl = Gdx.gl20;
        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        gl.glBufferSubData(GL20.GL_ARRAY_BUFFER, 0, floatCount * 4, data);
        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
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