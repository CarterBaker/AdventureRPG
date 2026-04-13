package application.bootstrap.shaderpipeline.texturemanager;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import engine.graphics.GL20;
import engine.graphics.GL30;
import engine.root.EngineContext;
import engine.root.EngineUtility;
import engine.settings.EngineSetting;
import engine.util.PixmapUtility;
import engine.util.image.Pixmap;

/*
 * GL30 wrapper for texture array operations. Handles upload and deletion only.
 * Pixel conversion is delegated to PixmapUtility. Atlas images are already
 * flipped at composite time — no flip applied here.
 */
class GLSLUtility extends EngineUtility {

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

        if (!(EngineContext.gl instanceof GL30))
            throwException("GL30 required for texture arrays");

        GL30 gl30 = (GL30) EngineContext.gl;
        int handle = EngineContext.gl.glGenTexture();

        if (handle == 0)
            throwException("GPU handle could not be generated for texture array");

        EngineContext.gl.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, handle);

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

        EngineContext.gl.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL20.GL_TEXTURE_MIN_FILTER,
                EngineSetting.GL_NEAREST);
        EngineContext.gl.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL20.GL_TEXTURE_MAG_FILTER,
                EngineSetting.GL_NEAREST);
        EngineContext.gl.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
        EngineContext.gl.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);

        EngineContext.gl.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);

        return handle;
    }

    // GPU Disposal \\

    static void deleteTextureArray(int handle) {

        if (handle == 0)
            return;

        EngineContext.gl.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
        EngineContext.gl.glDeleteTexture(handle);
    }
}