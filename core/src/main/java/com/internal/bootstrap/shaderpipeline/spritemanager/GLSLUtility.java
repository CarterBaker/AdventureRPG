package com.internal.bootstrap.shaderpipeline.spritemanager;

import java.awt.image.BufferedImage;

import com.internal.platform.Gdx;
import com.internal.platform.graphics.GL20;
import com.internal.platform.graphics.Pixmap;
import com.internal.core.engine.UtilityPackage;
import com.internal.core.util.PixmapUtility;

/*
 * GL20 wrapper for individual sprite texture operations. Handles upload
 * and deletion only — format conversion is delegated to PixmapUtility.
 */
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

        Pixmap pixmap = PixmapUtility.fromBufferedImage(image, true);

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
}