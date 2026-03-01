package com.internal.bootstrap.shaderpipeline.texturemanager;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.internal.core.engine.UtilityPackage;
import com.internal.core.util.PixmapUtility;

/*
 * Handles all GL30 texture-array operations: uploading atlas layer stacks to
 * the GPU and releasing handles on disposal. Pixel conversion is delegated to
 * PixmapUtility. Flip is not applied here — atlas images are already flipped
 * at draw time inside InternalBuildSystem.
 */
class GLSLUtility extends UtilityPackage {

    // Texture Management \\

    static int pushTextureArray(BufferedImage[] layers) {

        int depth = layers.length;

        if (depth == 0)
            throwException("There are no defined layers in the texture array");

        BufferedImage first = layers[0];
        int width = first.getWidth();
        int height = first.getHeight();

        for (int i = 1; i < depth; i++) {
            BufferedImage b = layers[i];
            if (b.getWidth() != width || b.getHeight() != height)
                throwException("All images must be the same width and height");
        }

        if (!(Gdx.gl instanceof GL30))
            throwException("GL30 required for texture arrays");

        GL30 gl30 = (GL30) Gdx.gl;

        int handle = Gdx.gl.glGenTexture();

        if (handle == 0)
            throwException("The GPU handle for the texture array could not be generated");

        Gdx.gl.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, handle);

        gl30.glTexImage3D(
                GL30.GL_TEXTURE_2D_ARRAY,
                0,
                GL30.GL_RGBA8,
                width, height, depth,
                0,
                GL30.GL_RGBA,
                GL30.GL_UNSIGNED_BYTE,
                (ByteBuffer) null);

        for (int layer = 0; layer < depth; layer++) {
            Pixmap pix = PixmapUtility.fromBufferedImage(layers[layer], false);
            ByteBuffer pixels = pix.getPixels();
            pixels.position(0);
            gl30.glTexSubImage3D(
                    GL30.GL_TEXTURE_2D_ARRAY,
                    0,
                    0, 0, layer,
                    width, height, 1,
                    GL30.GL_RGBA,
                    GL30.GL_UNSIGNED_BYTE,
                    pixels);
            pix.dispose();
        }

        Gdx.gl.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_NEAREST);
        Gdx.gl.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_NEAREST);
        Gdx.gl.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
        Gdx.gl.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);

        Gdx.gl.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);

        return handle;
    }

    static void deleteTextureArray(int handle) {
        if (handle == 0)
            return;
        Gdx.gl.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
        Gdx.gl.glDeleteTexture(handle);
    }
}