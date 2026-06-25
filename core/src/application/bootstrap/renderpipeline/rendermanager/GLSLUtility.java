package application.bootstrap.renderpipeline.rendermanager;

import org.lwjgl.glfw.GLFW;

import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

class GLSLUtility extends EngineUtility {

    /*
     * Stateless OpenGL state management helpers for RenderManager. Covers buffer
     * clearing, depth and blend state, scissor, shader binding, VAO operations,
     * UBO binding, and draw calls. Package-private.
     */

    // Buffer \\

    static void clearBuffer() {
        EngineContext.gl20.glClearColor(0, 0, 0, 1);
        EngineContext.gl20.glClear(EngineSetting.GL_COLOR_BUFFER_BIT | EngineSetting.GL_DEPTH_BUFFER_BIT);
    }

    static void clearBuffer(float r, float g, float b, float a) {
        EngineContext.gl20.glClearColor(r, g, b, a);
        EngineContext.gl20.glClear(EngineSetting.GL_COLOR_BUFFER_BIT | EngineSetting.GL_DEPTH_BUFFER_BIT);
    }

    static void clearDepthBuffer() {
        EngineContext.gl20.glClear(EngineSetting.GL_DEPTH_BUFFER_BIT);
    }

    static void setViewport(int width, int height) {
        EngineContext.gl20.glViewport(0, 0, width, height);
    }

    static void unbindFramebuffer() {
        EngineContext.gl30.glBindFramebuffer(EngineSetting.GL_FRAMEBUFFER, EngineSetting.GL_HANDLE_NONE);
    }

    // Depth \\

    static void enableDepth() {
        EngineContext.gl20.glEnable(EngineSetting.GL_DEPTH_TEST);
        EngineContext.gl20.glDepthFunc(EngineSetting.GL_LEQUAL);
        EngineContext.gl20.glDepthMask(true);
    }

    static void disableDepth() {
        EngineContext.gl20.glDepthMask(false);
        EngineContext.gl20.glDisable(EngineSetting.GL_DEPTH_TEST);
    }

    // Blending \\

    static void enableBlending() {
        EngineContext.gl20.glEnable(EngineSetting.GL_BLEND);
        EngineContext.gl20.glBlendFunc(EngineSetting.GL_SRC_ALPHA, EngineSetting.GL_ONE_MINUS_SRC_ALPHA);
    }

    static void disableBlending() {
        EngineContext.gl20.glDisable(EngineSetting.GL_BLEND);
    }

    // Culling \\

    static void enableCulling() {
        EngineContext.gl20.glEnable(EngineSetting.GL_CULL_FACE);
        EngineContext.gl20.glCullFace(EngineSetting.GL_BACK);
        EngineContext.gl20.glFrontFace(EngineSetting.GL_CCW);
    }

    static void disableCulling() {
        EngineContext.gl20.glDisable(EngineSetting.GL_CULL_FACE);
    }

    // Scissor \\

    static void enableScissor(int x, int y, int w, int h) {
        EngineContext.gl20.glEnable(EngineSetting.GL_SCISSOR_TEST);
        EngineContext.gl20.glScissor(x, y, w, h);
    }

    static void disableScissor() {
        EngineContext.gl20.glDisable(EngineSetting.GL_SCISSOR_TEST);
    }

    // Shader \\

    static void useShader(int shaderHandle) {
        EngineContext.gl20.glUseProgram(shaderHandle);
    }

    // VAO \\

    static void bindVAO(int vaoHandle) {
        EngineContext.gl30.glBindVertexArray(vaoHandle);
    }

    static void unbindVAO() {
        EngineContext.gl30.glBindVertexArray(EngineSetting.GL_HANDLE_NONE);
    }

    // Draw \\

    static void drawElements(int indexCount) {
        EngineContext.gl20.glDrawElements(EngineSetting.GL_TRIANGLES, indexCount, EngineSetting.GL_UNSIGNED_SHORT,
                EngineSetting.GL_HANDLE_NONE);
    }

    static void drawPatches(int vertexCount) {
        EngineContext.gl20.glDrawArrays(EngineSetting.GL_PATCHES, 0, vertexCount);
    }

    // UBO \\

    static void bindUniformBuffer(int bindingPoint, int gpuHandle) {
        EngineContext.gl30.glBindBufferBase(EngineSetting.GL_UNIFORM_BUFFER, bindingPoint, gpuHandle);
    }

    static void bindUniformBlockToProgram(int shaderProgram, String blockName, int bindingPoint) {
        int blockIndex = EngineContext.gl30.glGetUniformBlockIndex(shaderProgram, blockName);
        if (blockIndex != EngineSetting.GL_INVALID_INDEX)
            EngineContext.gl30.glUniformBlockBinding(shaderProgram, blockIndex, bindingPoint);
    }

    static void updateUniformBuffer(int gpuHandle, int offset, java.nio.ByteBuffer data) {
        EngineContext.gl30.glBindBuffer(EngineSetting.GL_UNIFORM_BUFFER, gpuHandle);
        EngineContext.gl30.glBufferSubData(EngineSetting.GL_UNIFORM_BUFFER, offset, data.remaining(), data);
        EngineContext.gl30.glBindBuffer(EngineSetting.GL_UNIFORM_BUFFER, EngineSetting.GL_HANDLE_NONE);
    }

    // Texture \\

    static void bindSamplerUniform(int location, int unit) {
        EngineContext.gl20.glUniform1i(location, unit);
    }

    // Presentation \\

    static void swapBuffers(long nativeHandle) {
        GLFW.glfwSwapBuffers(nativeHandle);
    }

    // Tessellation \\

    static void setPatchVertices(int count) {
        EngineContext.gl40.glPatchParameteri(EngineSetting.GL_PATCH_VERTICES, count);
    }
}
