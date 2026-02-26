package com.internal.bootstrap.shaderpipeline.spritemanager;

import java.awt.image.BufferedImage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.internal.core.engine.UtilityPackage;

class GLSLUtility extends UtilityPackage {

    // GPU Upload \\

    static int pushSprite(BufferedImage image) {

        int handle = Gdx.gl.glGenTexture();

        if (handle == 0)
            throwException("GPU handle could not be generated for sprite");

        Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, handle);

        Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
        Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);
        Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_LINEAR);
        Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_LINEAR);

        Pixmap pixmap = convertToPixmap(image);

        Gdx.gl.glTexImage2D(
                GL20.GL_TEXTURE_2D,
                0,
                GL20.GL_RGBA,
                pixmap.getWidth(),
                pixmap.getHeight(),
                0,
                GL20.GL_RGBA,
                GL20.GL_UNSIGNED_BYTE,
                pixmap.getPixels());

        pixmap.dispose();

        Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, 0);

        return handle;
    }

    // GPU Disposal \\

    static void deleteSprite(int handle) {

        if (handle == 0)
            return;

        Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, 0);
        Gdx.gl.glDeleteTexture(handle);
    }

    // Conversion \\

    private static Pixmap convertToPixmap(BufferedImage image) {

        int width = image.getWidth();
        int height = image.getHeight();

        int[] argb = new int[width * height];
        image.getRGB(0, 0, width, height, argb, 0, width);

        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);

        int idx = 0;

        // Flip vertically — BufferedImage origin is top-left, OpenGL is bottom-left
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {

                int c = argb[y * width + x];

                int a = (c >> 24) & 0xFF;
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = (c) & 0xFF;

                // LibGDX RGBA8888: R highest byte, A lowest
                int rgba = (r << 24) | (g << 16) | (b << 8) | a;

                pixmap.drawPixel(x, idx / width, rgba);
                idx++;
            }
        }

        return pixmap;
    }
}