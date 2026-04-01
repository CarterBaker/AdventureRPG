package com.internal.bootstrap.renderpipeline.rendermanager;

import com.internal.platform.PlatformRuntime;
import com.internal.platform.graphics.GL20;
import com.internal.platform.graphics.GL30;
import com.internal.core.engine.UtilityPackage;

class GLSLUtility extends UtilityPackage {

    /*
     * Stateless OpenGL state management helpers for RenderManager. Covers buffer
     * clearing, depth and blend state, scissor, shader binding, VAO operations,
     * UBO binding, and draw calls. Package-private.
     */

    // Buffer \\

    static void clearBuffer() {
        PlatformRuntime.gl.glClearColor(0, 0, 0, 0);
        PlatformRuntime.gl.glClear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT);
    }

    static void clearDepthBuffer() {
        PlatformRuntime.gl.glClear(GL30.GL_DEPTH_BUFFER_BIT);
    }

    static void setViewport(int width, int height) {
        PlatformRuntime.gl.glViewport(0, 0, width, height);
    }

    // Depth \\

    static void enableDepth() {
        PlatformRuntime.gl.glEnable(GL20.GL_DEPTH_TEST);
        PlatformRuntime.gl.glDepthFunc(GL20.GL_LEQUAL);
        PlatformRuntime.gl.glDepthMask(true);
    }

    static void disableDepth() {
        PlatformRuntime.gl.glDepthMask(false);
        PlatformRuntime.gl.glDisable(GL20.GL_DEPTH_TEST);
    }

    // Blending \\

    static void enableBlending() {
        PlatformRuntime.gl.glEnable(GL20.GL_BLEND);
        PlatformRuntime.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    static void disableBlending() {
        PlatformRuntime.gl.glDisable(GL20.GL_BLEND);
    }

    // Culling \\

    static void enableCulling() {
        PlatformRuntime.gl.glEnable(GL20.GL_CULL_FACE);
        PlatformRuntime.gl.glCullFace(GL20.GL_BACK);
        PlatformRuntime.gl.glFrontFace(GL20.GL_CCW);
    }

    static void disableCulling() {
        PlatformRuntime.gl.glDisable(GL20.GL_CULL_FACE);
    }

    // Scissor \\

    static void enableScissor(int x, int y, int w, int h) {
        PlatformRuntime.gl.glEnable(GL20.GL_SCISSOR_TEST);
        PlatformRuntime.gl.glScissor(x, y, w, h);
    }

    static void disableScissor() {
        PlatformRuntime.gl.glDisable(GL20.GL_SCISSOR_TEST);
    }

    // Shader \\

    static void useShader(int shaderHandle) {
        PlatformRuntime.gl.glUseProgram(shaderHandle);
    }

    // VAO \\

    static void bindVAO(int vaoHandle) {
        PlatformRuntime.gl30.glBindVertexArray(vaoHandle);
    }

    static void unbindVAO() {
        PlatformRuntime.gl30.glBindVertexArray(0);
    }

    // Draw \\

    static void drawElements(int indexCount) {
        PlatformRuntime.gl.glDrawElements(GL20.GL_TRIANGLES, indexCount, GL20.GL_UNSIGNED_SHORT, 0);
    }

    // UBO \\

    static void bindUniformBuffer(int bindingPoint, int gpuHandle) {
        PlatformRuntime.gl30.glBindBufferBase(GL30.GL_UNIFORM_BUFFER, bindingPoint, gpuHandle);
    }

    static void bindUniformBlockToProgram(int shaderProgram, String blockName, int bindingPoint) {
        int blockIndex = PlatformRuntime.gl30.glGetUniformBlockIndex(shaderProgram, blockName);
        if (blockIndex != GL30.GL_INVALID_INDEX)
            PlatformRuntime.gl30.glUniformBlockBinding(shaderProgram, blockIndex, bindingPoint);
    }

    static void updateUniformBuffer(int gpuHandle, int offset, java.nio.ByteBuffer data) {
        PlatformRuntime.gl30.glBindBuffer(GL30.GL_UNIFORM_BUFFER, gpuHandle);
        PlatformRuntime.gl30.glBufferSubData(GL30.GL_UNIFORM_BUFFER, offset, data.remaining(), data);
        PlatformRuntime.gl30.glBindBuffer(GL30.GL_UNIFORM_BUFFER, 0);
    }
}
