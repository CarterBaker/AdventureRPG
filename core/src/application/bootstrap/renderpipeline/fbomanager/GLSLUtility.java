package application.bootstrap.renderpipeline.fbomanager;

import engine.graphics.gl.GL20;
import engine.graphics.gl.GL30;
import engine.root.EngineContext;
import engine.root.EngineUtility;

class GLSLUtility extends EngineUtility {

    static int genFramebuffer() {
        return EngineContext.gl30.glGenFramebuffer();
    }

    static void bindFramebuffer(int fbo) {
        EngineContext.gl30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
    }

    static void unbindFramebuffer() {
        EngineContext.gl30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    static void framebufferTexture2D(int texture) {
        EngineContext.gl30.glFramebufferTexture2D(
                GL30.GL_FRAMEBUFFER,
                GL30.GL_COLOR_ATTACHMENT0,
                GL20.GL_TEXTURE_2D,
                texture,
                0);
    }

    static int genRenderbuffer() {
        return EngineContext.gl30.glGenRenderbuffer();
    }

    static void bindRenderbuffer(int rbo) {
        EngineContext.gl30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, rbo);
    }

    static void unbindRenderbuffer() {
        EngineContext.gl30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);
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

    static int checkFramebufferStatus() {
        return EngineContext.gl30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
    }
}
