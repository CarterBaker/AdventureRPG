package program.bootstrap.renderpipeline.rendermanager;

import program.core.app.CoreContext;
import program.core.util.graphics.gl.GL20;
import program.core.util.graphics.gl.GL30;
import program.core.engine.UtilityPackage;
import program.core.settings.EngineSetting;

class GLSLUtility extends UtilityPackage {

    /*
     * Stateless OpenGL state management helpers for RenderManager. Covers buffer
     * clearing, depth and blend state, scissor, shader binding, VAO operations,
     * UBO binding, and draw calls. Package-private.
     */

    // Buffer \\

    static void clearBuffer() {
        CoreContext.gl.glClearColor(0, 0, 0, 0);
        CoreContext.gl.glClear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT);
    }

    static void clearDepthBuffer() {
        CoreContext.gl.glClear(GL30.GL_DEPTH_BUFFER_BIT);
    }

    static void setViewport(int width, int height) {
        CoreContext.gl.glViewport(0, 0, width, height);
    }

    // Depth \\

    static void enableDepth() {
        CoreContext.gl.glEnable(GL20.GL_DEPTH_TEST);
        CoreContext.gl.glDepthFunc(GL20.GL_LEQUAL);
        CoreContext.gl.glDepthMask(true);
    }

    static void disableDepth() {
        CoreContext.gl.glDepthMask(false);
        CoreContext.gl.glDisable(GL20.GL_DEPTH_TEST);
    }

    // Blending \\

    static void enableBlending() {
        CoreContext.gl.glEnable(GL20.GL_BLEND);
        CoreContext.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    static void disableBlending() {
        CoreContext.gl.glDisable(GL20.GL_BLEND);
    }

    // Culling \\

    static void enableCulling() {
        CoreContext.gl.glEnable(GL20.GL_CULL_FACE);
        CoreContext.gl.glCullFace(GL20.GL_BACK);
        CoreContext.gl.glFrontFace(GL20.GL_CCW);
    }

    static void disableCulling() {
        CoreContext.gl.glDisable(GL20.GL_CULL_FACE);
    }

    // Scissor \\

    static void enableScissor(int x, int y, int w, int h) {
        CoreContext.gl.glEnable(GL20.GL_SCISSOR_TEST);
        CoreContext.gl.glScissor(x, y, w, h);
    }

    static void disableScissor() {
        CoreContext.gl.glDisable(GL20.GL_SCISSOR_TEST);
    }

    // Shader \\

    static void useShader(int shaderHandle) {
        CoreContext.gl.glUseProgram(shaderHandle);
    }

    // VAO \\

    static void bindVAO(int vaoHandle) {
        CoreContext.gl30.glBindVertexArray(vaoHandle);
    }

    static void unbindVAO() {
        CoreContext.gl30.glBindVertexArray(EngineSetting.GL_HANDLE_NONE);
    }

    // Draw \\

    static void drawElements(int indexCount) {
        CoreContext.gl.glDrawElements(GL20.GL_TRIANGLES, indexCount, GL20.GL_UNSIGNED_SHORT, EngineSetting.GL_HANDLE_NONE);
    }

    // UBO \\

    static void bindUniformBuffer(int bindingPoint, int gpuHandle) {
        CoreContext.gl30.glBindBufferBase(GL30.GL_UNIFORM_BUFFER, bindingPoint, gpuHandle);
    }

    static void bindUniformBlockToProgram(int shaderProgram, String blockName, int bindingPoint) {
        int blockIndex = CoreContext.gl30.glGetUniformBlockIndex(shaderProgram, blockName);
        if (blockIndex != EngineSetting.GL_INVALID_INDEX)
            CoreContext.gl30.glUniformBlockBinding(shaderProgram, blockIndex, bindingPoint);
    }

    static void updateUniformBuffer(int gpuHandle, int offset, java.nio.ByteBuffer data) {
        CoreContext.gl30.glBindBuffer(GL30.GL_UNIFORM_BUFFER, gpuHandle);
        CoreContext.gl30.glBufferSubData(GL30.GL_UNIFORM_BUFFER, offset, data.remaining(), data);
        CoreContext.gl30.glBindBuffer(GL30.GL_UNIFORM_BUFFER, EngineSetting.GL_HANDLE_NONE);
    }
}
