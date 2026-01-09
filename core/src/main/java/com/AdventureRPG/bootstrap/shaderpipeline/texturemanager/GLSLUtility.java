package com.AdventureRPG.bootstrap.shaderpipeline.texturemanager;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import com.AdventureRPG.core.engine.UtilityPackage;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;

class GLSLUtility extends UtilityPackage {

    // Texture Management \\

    static int pushTextureArray(BufferedImage[] layers) {

        int depth = layers.length;

        if (depth == 0)
            throwException("There are no defined layers in the texture array");

        // Ensure all layers are the same size
        BufferedImage first = layers[0];
        int width = first.getWidth();
        int height = first.getHeight();

        for (int i = 1; i < depth; i++) {

            BufferedImage b = layers[i];

            if (b.getWidth() != width || b.getHeight() != height)
                throwException("All images must be the same width and height");
        }

        // ensure GL30 available
        if (!(Gdx.gl instanceof GL30))
            throwException("GL30 required for texture arrays.");

        GL30 gl30 = (GL30) Gdx.gl;

        // generate texture handle
        int handle = Gdx.gl.glGenTexture();

        if (handle == 0) // TODO: Add my own error
            throwException("The GPU handle for the image could not be found");

        // bind and allocate empty storage
        Gdx.gl.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, handle);

        // Allocate immutable-like storage. Use GL_RGBA8 internal format.
        gl30.glTexImage3D(
                GL30.GL_TEXTURE_2D_ARRAY,
                0, // level
                GL30.GL_RGBA8, // internal format
                width,
                height,
                depth, // number of layers
                0, // border
                GL30.GL_RGBA, // format (for texsub)
                GL30.GL_UNSIGNED_BYTE,
                (ByteBuffer) null // no data yet
        );

        // Upload each layer via TexSubImage3D
        for (int layer = 0; layer < depth; layer++) {

            BufferedImage img = layers[layer];
            Pixmap pix = convertBufferedImageToPixmapRGBA(img);

            // Reset position to 0 to be safe
            ByteBuffer pixels = pix.getPixels();
            pixels.position(0);

            // Upload slice
            gl30.glTexSubImage3D(
                    GL30.GL_TEXTURE_2D_ARRAY,
                    0, // level
                    0, 0, layer, // xoffset, yoffset, zoffset
                    width,
                    height,
                    1, // depth = 1 for single slice
                    GL30.GL_RGBA,
                    GL30.GL_UNSIGNED_BYTE,
                    pixels);

            // dispose temporary pixmap
            pix.dispose();
        }

        // Set texture parameters
        Gdx.gl.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_NEAREST);
        Gdx.gl.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_NEAREST);
        Gdx.gl.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
        Gdx.gl.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);

        // Unbind
        Gdx.gl.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);

        return handle;
    }

    private static Pixmap convertBufferedImageToPixmapRGBA(BufferedImage img) {

        final int w = img.getWidth();
        final int h = img.getHeight();

        // Allocate the pixmap
        Pixmap pix = new Pixmap(w, h, Pixmap.Format.RGBA8888);

        // Read all ARGB pixels from the BufferedImage
        int[] argb = new int[w * h];
        img.getRGB(0, 0, w, h, argb, 0, w);

        // Convert each pixel to RGBA where R is highest byte (LibGDX standard)
        int idx = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                int c = argb[idx++];

                int a = (c >> 24) & 0xFF;
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = (c) & 0xFF;

                // Pack into LibGDX RGBA8888 (R highest byte, A lowest)
                int rgba = (r << 24) |
                        (g << 16) |
                        (b << 8) |
                        a;

                pix.drawPixel(x, y, rgba);
            }
        }

        return pix;
    }

    static void deleteTextureArray(int handle) {

        if (handle == 0)
            return;

        // Unbind first for safety
        Gdx.gl.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);

        // Delete GPU side
        Gdx.gl.glDeleteTexture(handle);
    }
}
