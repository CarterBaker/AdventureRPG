package com.internal.bootstrap.renderpipeline.rendersystem;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;

class GLSLUtility {

    // Buffer \\

    static void enableDepth() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        Gdx.gl.glDepthMask(true);
    }

    static void disableDepth() {
        Gdx.gl.glDepthMask(false);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
    }

    static void clearBuffer() {
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT);
    }

    static void setViewport(int width, int height) {
        Gdx.gl.glViewport(0, 0, width, height);
    }

    // Blending \\

    static void enableBlending() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    static void disableBlending() {
        Gdx.gl.glDisable(GL20.GL_BLEND);
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
}