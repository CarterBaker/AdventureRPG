package application.bootstrap.renderpipeline.fbomanager;

import engine.graphics.gl.GL20;
import engine.graphics.gl.GL30;
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
        EngineContext.gl30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
    }

    static void unbindFramebuffer() {
        EngineContext.gl30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, EngineSetting.GL_HANDLE_NONE);
    }

    static void framebufferTexture2D(int texture) {
        EngineContext.gl30.glFramebufferTexture2D(
                GL30.GL_FRAMEBUFFER,
                GL30.GL_COLOR_ATTACHMENT0,
                GL20.GL_TEXTURE_2D,
                texture,
                0);
    }

    static int checkFramebufferStatus() {
        return EngineContext.gl30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
    }

    // Renderbuffer \\

    static int genRenderbuffer() {
        return EngineContext.gl30.glGenRenderbuffer();
    }

    static void bindRenderbuffer(int rbo) {
        EngineContext.gl30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, rbo);
    }

    static void unbindRenderbuffer() {
        EngineContext.gl30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, EngineSetting.GL_HANDLE_NONE);
    }

    static void renderbufferStorage(int internalFormat, int width, int height) {
        EngineContext.gl30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, internalFormat, width, height);
    }

    static void framebufferRenderbuffer(int rbo) {
        EngineContext.gl30.glFramebufferRenderbuffer(
                GL30.GL_FRAMEBUFFER,
                GL30.GL_DEPTH_ATTACHMENT,
                GL30.GL_RENDERBUFFER,
                rbo);
    }

    // Texture \\

    static int genTexture() {
        return EngineContext.gl20.glGenTexture();
    }

    static void bindTexture(int texture) {
        EngineContext.gl20.glBindTexture(GL20.GL_TEXTURE_2D, texture);
    }

    static void unbindTexture() {
        EngineContext.gl20.glBindTexture(GL20.GL_TEXTURE_2D, EngineSetting.GL_HANDLE_NONE);
    }

    static void texImage2D(int internalFormat, int width, int height) {
        EngineContext.gl20.glTexImage2D(
                GL20.GL_TEXTURE_2D, 0, internalFormat, width, height, 0,
                GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, null);
    }

    static void texParameterLinear() {
        EngineContext.gl20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_LINEAR);
        EngineContext.gl20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_LINEAR);
        EngineContext.gl20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
        EngineContext.gl20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);
    }

    static void resizeTexture(int texture, int internalFormat, int width, int height) {
        bindTexture(texture);
        EngineContext.gl20.glTexImage2D(
                GL20.GL_TEXTURE_2D, 0, internalFormat, width, height, 0,
                GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, null);
        unbindTexture();
    }

    // Viewport \\

    static void setViewport(int width, int height) {
        EngineContext.gl20.glViewport(0, 0, width, height);
    }
}