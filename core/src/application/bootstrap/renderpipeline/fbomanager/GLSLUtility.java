package application.bootstrap.renderpipeline.fbomanager;

import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

class GLSLUtility extends EngineUtility {

    /*
     * Stateless OpenGL helpers for the FBO package. Covers framebuffer and
     * renderbuffer lifecycle, texture allocation and parametrization, viewport
     * state, and resize operations. Package-private — only FboManager and
     * InternalBuilder may call these.
     */

    // Framebuffer \\

    static int genFramebuffer() {
        return EngineContext.gl30.glGenFramebuffer();
    }

    static void bindFramebuffer(int fbo) {
        EngineContext.gl30.glBindFramebuffer(EngineSetting.GL_FRAMEBUFFER, fbo);
    }

    static void unbindFramebuffer() {
        EngineContext.gl30.glBindFramebuffer(EngineSetting.GL_FRAMEBUFFER, EngineSetting.GL_HANDLE_NONE);
    }

    static void framebufferTexture2D(int texture) {
        EngineContext.gl30.glFramebufferTexture2D(
                EngineSetting.GL_FRAMEBUFFER,
                EngineSetting.GL_COLOR_ATTACHMENT0,
                EngineSetting.GL_TEXTURE_2D,
                texture,
                0);
    }

    static int checkFramebufferStatus() {
        return EngineContext.gl30.glCheckFramebufferStatus(EngineSetting.GL_FRAMEBUFFER);
    }

    // Renderbuffer \\

    static int genRenderbuffer() {
        return EngineContext.gl30.glGenRenderbuffer();
    }

    static void bindRenderbuffer(int rbo) {
        EngineContext.gl30.glBindRenderbuffer(EngineSetting.GL_RENDERBUFFER, rbo);
    }

    static void unbindRenderbuffer() {
        EngineContext.gl30.glBindRenderbuffer(EngineSetting.GL_RENDERBUFFER, EngineSetting.GL_HANDLE_NONE);
    }

    static void renderbufferStorage(int internalFormat, int width, int height) {
        EngineContext.gl30.glRenderbufferStorage(EngineSetting.GL_RENDERBUFFER, internalFormat, width, height);
    }

    static void framebufferRenderbuffer(int rbo) {
        EngineContext.gl30.glFramebufferRenderbuffer(
                EngineSetting.GL_FRAMEBUFFER,
                EngineSetting.GL_DEPTH_ATTACHMENT,
                EngineSetting.GL_RENDERBUFFER,
                rbo);
    }

    // Texture \\

    static int genTexture() {
        return EngineContext.gl20.glGenTexture();
    }

    static void bindTexture(int texture) {
        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, texture);
    }

    static void unbindTexture() {
        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_HANDLE_NONE);
    }

    static void texImage2D(int internalFormat, int width, int height) {
        EngineContext.gl20.glTexImage2D(
                EngineSetting.GL_TEXTURE_2D, 0, internalFormat, width, height, 0,
                EngineSetting.GL_RGBA, EngineSetting.GL_UNSIGNED_BYTE, null);
    }

    static void texParameterLinear() {
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_MIN_FILTER,
                EngineSetting.GL_LINEAR);
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_MAG_FILTER,
                EngineSetting.GL_LINEAR);
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_WRAP_S,
                EngineSetting.GL_CLAMP_TO_EDGE);
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_WRAP_T,
                EngineSetting.GL_CLAMP_TO_EDGE);
    }

    // Texture — Color \\

    static void texImage2DColor(int internalFormat, int width, int height) {
        int baseFormat = resolveColorBaseFormat(internalFormat);
        int dataType = resolveColorDataType(internalFormat);
        EngineContext.gl20.glTexImage2D(
                EngineSetting.GL_TEXTURE_2D, 0, internalFormat,
                width, height, 0, baseFormat, dataType, null);
    }

    static void texImage2DDepth(int width, int height) {
        EngineContext.gl20.glTexImage2D(
                EngineSetting.GL_TEXTURE_2D, 0, EngineSetting.GL_DEPTH_COMPONENT32F,
                width, height, 0, EngineSetting.GL_DEPTH_COMPONENT, EngineSetting.GL_FLOAT, null);
    }

    static void texParameterNearest() {
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_MIN_FILTER,
                EngineSetting.GL_NEAREST);
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_MAG_FILTER,
                EngineSetting.GL_NEAREST);
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_WRAP_S,
                EngineSetting.GL_CLAMP_TO_EDGE);
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_WRAP_T,
                EngineSetting.GL_CLAMP_TO_EDGE);
    }

    // Texture Unit \\

    static void bindTextureToUnit(int texture, int unit) {
        EngineContext.gl20.glActiveTexture(EngineSetting.GL_TEXTURE0 + unit);
        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, texture);
    }

    // Framebuffer — Attachments \\

    static void framebufferTexture2DColor(int texture, int colorIndex) {
        EngineContext.gl30.glFramebufferTexture2D(
                EngineSetting.GL_FRAMEBUFFER,
                EngineSetting.GL_COLOR_ATTACHMENT0 + colorIndex,
                EngineSetting.GL_TEXTURE_2D, texture, 0);
    }

    static void framebufferTexture2DDepth(int texture) {
        EngineContext.gl30.glFramebufferTexture2D(
                EngineSetting.GL_FRAMEBUFFER,
                EngineSetting.GL_DEPTH_ATTACHMENT,
                EngineSetting.GL_TEXTURE_2D, texture, 0);
    }

    static void drawBuffers(int colorCount) {
        int[] buffers = new int[colorCount];
        for (int i = 0; i < colorCount; i++)
            buffers[i] = EngineSetting.GL_COLOR_ATTACHMENT0 + i;
        EngineContext.gl30.glDrawBuffers(buffers);
    }

    // Viewport \\

    static void setViewport(int width, int height) {
        EngineContext.gl20.glViewport(0, 0, width, height);
    }

    // Resize \\

    static void resizeColorTexture(int texture, int internalFormat, int width, int height) {
        int baseFormat = resolveColorBaseFormat(internalFormat);
        int dataType = resolveColorDataType(internalFormat);
        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, texture);
        EngineContext.gl20.glTexImage2D(
                EngineSetting.GL_TEXTURE_2D, 0, internalFormat,
                width, height, 0, baseFormat, dataType, null);
        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_HANDLE_NONE);
    }

    static void resizeDepthTexture(int texture, int width, int height) {

        EngineContext.gl20.glBindTexture(
                EngineSetting.GL_TEXTURE_2D,
                texture);
        EngineContext.gl20.glTexImage2D(
                EngineSetting.GL_TEXTURE_2D,
                0,
                EngineSetting.GL_DEPTH_COMPONENT32F,
                width, height,
                0,
                EngineSetting.GL_DEPTH_COMPONENT,
                EngineSetting.GL_FLOAT,
                null);
        EngineContext.gl20.glBindTexture(
                EngineSetting.GL_TEXTURE_2D,
                EngineSetting.GL_HANDLE_NONE);
    }

    // Internal \\

    private static int resolveColorBaseFormat(int internalFormat) {
        if (internalFormat == EngineSetting.GL_RGB8 || internalFormat == EngineSetting.GL_RGB16F)
            return EngineSetting.GL_RGB;
        return EngineSetting.GL_RGBA;
    }

    private static int resolveColorDataType(int internalFormat) {
        if (internalFormat == EngineSetting.GL_RGBA16F || internalFormat == EngineSetting.GL_RGB16F)
            return EngineSetting.GL_FLOAT;
        return EngineSetting.GL_UNSIGNED_BYTE;
    }
}