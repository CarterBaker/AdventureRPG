package application.bootstrap.renderpipeline.rendermanager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.glfw.GLFW;

import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

class RenderGLSLUtility extends EngineUtility {

    /*
     * Stateless OpenGL state management helpers for RenderManager. Covers buffer
     * clearing, depth and blend state, scissor, shader binding, VAO operations,
     * UBO binding, draw calls, and the instanced-VAO/instance-buffer helpers
     * shared by the skinned and generic-instanced (cloud) render paths.
     * Package-private.
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

    /*
     * Toggles depth writes only, leaving the depth test/func untouched.
     * Used to bracket translucent, depth-test-but-don't-write passes (e.g.
     * weather/cloud instances) that must still sit behind terrain and
     * characters but must not depth-occlude each other based on draw order
     * within their own pass — see RenderSystem.drawToMappedTargets().
     */
    static void setDepthMask(boolean enabled) {
        EngineContext.gl20.glDepthMask(enabled);
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

    // Instanced VAO \\

    /*
     * Builds a VAO combining a mesh's own vertex attributes (as laid out by
     * meshAttrSizes) with a per-instance attribute set sourced from
     * instanceVBOHandle, one attribute location per entry in
     * instanceAttrSizes, each with a vertex attrib divisor of 1. Shared by
     * the skinned character path and the generic world-space instanced
     * (cloud) path.
     */
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
        EngineContext.gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, meshVBOHandle);

        int meshStride = 0;
        for (int i = 0; i < meshAttrSizes.length; i++)
            meshStride += meshAttrSizes[i];
        int meshStrideBytes = meshStride * Float.BYTES;

        int meshOffsetBytes = 0;
        for (int i = 0; i < meshAttrSizes.length; i++) {
            EngineContext.gl20.glEnableVertexAttribArray(i);
            EngineContext.gl20.glVertexAttribPointer(
                    i, meshAttrSizes[i], EngineSetting.GL_FLOAT, false, meshStrideBytes, meshOffsetBytes);
            meshOffsetBytes += meshAttrSizes[i] * Float.BYTES;
        }

        int instanceStride = 0;
        for (int i = 0; i < instanceAttrSizes.length; i++)
            instanceStride += instanceAttrSizes[i];
        int instanceStrideBytes = instanceStride * Float.BYTES;

        EngineContext.gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, instanceVBOHandle);

        int instanceOffsetBytes = 0;
        for (int i = 0; i < instanceAttrSizes.length; i++) {
            int location = meshAttrSizes.length + i;
            EngineContext.gl20.glEnableVertexAttribArray(location);
            EngineContext.gl20.glVertexAttribPointer(
                    location, instanceAttrSizes[i], EngineSetting.GL_FLOAT, false,
                    instanceStrideBytes, instanceOffsetBytes);
            EngineContext.gl30.glVertexAttribDivisor(location, 1);
            instanceOffsetBytes += instanceAttrSizes[i] * Float.BYTES;
        }

        EngineContext.gl20.glBindBuffer(EngineSetting.GL_ELEMENT_ARRAY_BUFFER, meshIBOHandle);
        EngineContext.gl30.glBindVertexArray(0);
        EngineContext.gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, 0);

        return vao;
    }

    static void deleteVAO(int handle) {

        if (handle == 0)
            return;

        IntBuffer idBuffer = ByteBuffer.allocateDirect(Integer.BYTES)
                .order(ByteOrder.nativeOrder()).asIntBuffer();
        idBuffer.put(handle).flip();

        EngineContext.gl30.glDeleteVertexArrays(1, idBuffer);
    }

    static void drawElementsInstanced(int indexCount, int instanceCount) {
        EngineContext.gl30.glDrawElementsInstanced(
                EngineSetting.GL_TRIANGLES, indexCount, EngineSetting.GL_UNSIGNED_SHORT,
                EngineSetting.GL_HANDLE_NONE, instanceCount);
    }

    // Instance Buffers \\

    /*
     * Creates a dynamic instance VBO sized for maxInstances rows of
     * floatsPerInstance floats each. Shared by the skinned model-matrix
     * buffer and the generic per-instance float buffer
     * (CompositeBufferInstance) used for world-space instanced draws such
     * as physical weather clouds.
     */
    static int createDynamicInstanceVBO(int maxInstances, int floatsPerInstance) {

        int size = maxInstances * floatsPerInstance * Float.BYTES;

        int vbo = EngineContext.gl20.glGenBuffer();
        EngineContext.gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, vbo);
        EngineContext.gl20.glBufferData(EngineSetting.GL_ARRAY_BUFFER, size, null, EngineSetting.GL_DYNAMIC_DRAW);
        EngineContext.gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, 0);

        return vbo;
    }

    static void updateInstanceVBO(int vbo, FloatBuffer data, int floatCount) {
        EngineContext.gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, vbo);
        EngineContext.gl20.glBufferSubData(EngineSetting.GL_ARRAY_BUFFER, 0, floatCount * Float.BYTES, data);
        EngineContext.gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, 0);
    }

    static void deleteBuffer(int handle) {
        if (handle != 0)
            EngineContext.gl20.glDeleteBuffer(handle);
    }
}