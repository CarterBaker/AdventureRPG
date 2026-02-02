package com.internal.bootstrap.renderpipeline.rendersystem;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;

class GLSLUtility {

    // Buffer \\

    static void clearBuffer() {
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT);
    }

    static void setViewport(int width, int height) {
        Gdx.gl.glViewport(0, 0, width, height);
    }

    // Depth \\

    static void enableDepth() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        Gdx.gl.glDepthMask(true);
    }

    static void disableDepth() {
        Gdx.gl.glDepthMask(false);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
    }

    static void clearDepthBuffer() {
        Gdx.gl.glClear(GL30.GL_DEPTH_BUFFER_BIT);
    }

    // Blending \\

    static void enableBlending() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    static void disableBlending() {
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // Culling \\

    static void enableCulling() {
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        Gdx.gl.glCullFace(GL20.GL_BACK);
        Gdx.gl.glFrontFace(GL20.GL_CCW);
    }

    static void disableCulling() {
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
    }

    // Shader \\

    static void useShader(int shaderHandle) {
        Gdx.gl.glUseProgram(shaderHandle);
    }

    // VAO \\

    static void bindVAO(int vaoHandle) {
        Gdx.gl30.glBindVertexArray(vaoHandle);
    }

    static void unbindVAO() {
        Gdx.gl30.glBindVertexArray(0);
    }

    // Drawing \\

    static void drawElements(int indexCount) {
        Gdx.gl.glDrawElements(GL20.GL_TRIANGLES, indexCount, GL20.GL_UNSIGNED_SHORT, 0);
    }

    // UBO \\

    static void bindUniformBuffer(int bindingPoint, int gpuHandle) {
        Gdx.gl30.glBindBufferBase(GL30.GL_UNIFORM_BUFFER, bindingPoint, gpuHandle);
    }

    static void bindUniformBlockToProgram(int shaderProgram, String blockName, int bindingPoint) {
        int blockIndex = Gdx.gl30.glGetUniformBlockIndex(shaderProgram, blockName);
        if (blockIndex != GL30.GL_INVALID_INDEX)
            Gdx.gl30.glUniformBlockBinding(shaderProgram, blockIndex, bindingPoint);
    }

    static void updateUniformBuffer(int gpuHandle, int offset, java.nio.ByteBuffer data) {
        Gdx.gl30.glBindBuffer(GL30.GL_UNIFORM_BUFFER, gpuHandle);
        Gdx.gl30.glBufferSubData(GL30.GL_UNIFORM_BUFFER, offset, data.remaining(), data);
        Gdx.gl30.glBindBuffer(GL30.GL_UNIFORM_BUFFER, 0);
    }
}