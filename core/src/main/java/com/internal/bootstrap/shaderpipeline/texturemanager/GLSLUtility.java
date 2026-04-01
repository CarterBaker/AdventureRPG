package com.internal.bootstrap.shaderpipeline.texturemanager;

import com.internal.platform.PlatformRuntime;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import com.internal.platform.graphics.GL20;
import com.internal.platform.graphics.GL30;
import com.internal.core.util.image.Pixmap;
import com.internal.core.engine.UtilityPackage;
import com.internal.core.util.PixmapUtility;

/*
 * GL30 wrapper for texture array operations. Handles upload and deletion only.
 * Pixel conversion is delegated to PixmapUtility. Atlas images are already
 * flipped at composite time — no flip applied here.
 */
class GLSLUtility extends UtilityPackage {

    // GPU Upload \\

    static int pushTextureArray(BufferedImage[] layers) {

        int depth = layers.length;

        if (depth == 0)
            throwException("No layers defined in texture array");

        BufferedImage first = layers[0];
        int width = first.getWidth();
        int height = first.getHeight();

        for (int i = 1; i < depth; i++) {
            BufferedImage b = layers[i];
            if (b.getWidth() != width || b.getHeight() != height)
                throwException("All texture array layers must have identical dimensions");
        }

        if (!(PlatformRuntime.gl instanceof GL30))
            throwException("GL30 required for texture arrays");

        GL30 gl30 = (GL30) PlatformRuntime.gl;
        int handle = PlatformRuntime.gl.glGenTexture();

        if (handle == 0)
            throwException("GPU handle could not be generated for texture array");

        PlatformRuntime.gl.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, handle);

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

        PlatformRuntime.gl.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_NEAREST);
        PlatformRuntime.gl.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_NEAREST);
        PlatformRuntime.gl.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
        PlatformRuntime.gl.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);

        PlatformRuntime.gl.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);

        return handle;
    }

    // GPU Disposal \\

    static void deleteTextureArray(int handle) {

        if (handle == 0)
            return;

        PlatformRuntime.gl.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
        PlatformRuntime.gl.glDeleteTexture(handle);
    }
}
