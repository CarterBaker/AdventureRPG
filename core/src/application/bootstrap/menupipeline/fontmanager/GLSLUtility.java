package application.bootstrap.menupipeline.fontmanager;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import engine.assets.image.Pixmap;
import engine.assets.image.PixmapUtility;
import engine.graphics.gl.GL20;
import engine.graphics.gl.GL30;
import engine.root.EngineContext;
import engine.root.EngineUtility;

class GLSLUtility extends EngineUtility {

    /*
     * GL30 texture array upload for the font pipeline. Mirrors the texture
     * manager's upload path exactly — fonts are single-layer arrays and go
     * through the same GL path as everything else. GL_LINEAR instead of
     * GL_NEAREST because fonts are not pixel art.
     * Package-private — only FontManager internals may call these.
     */

    // GPU Upload \\

    static int pushTextureArray(BufferedImage[] layers) {

        int depth = layers.length;

        if (depth == 0)
            throwException("[GLSLUtility] No layers provided for font texture array");

        BufferedImage first = layers[0];
        int width = first.getWidth();
        int height = first.getHeight();

        if (!(EngineContext.gl20 instanceof GL30))
            throwException("[GLSLUtility] GL30 required for texture arrays");

        GL30 gl30 = (GL30) EngineContext.gl20;

        int handle = EngineContext.gl20.glGenTexture();

        if (handle == 0)
            throwException("[GLSLUtility] Failed to generate GPU handle for font texture array");

        EngineContext.gl20.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, handle);

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

        EngineContext.gl20.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_LINEAR);
        EngineContext.gl20.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_LINEAR);
        EngineContext.gl20.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
        EngineContext.gl20.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);

        EngineContext.gl20.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);

        return handle;
    }

    // GPU Disposal \\

    static void deleteTextureArray(int handle) {

        if (handle == 0)
            return;

        EngineContext.gl20.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
        EngineContext.gl20.glDeleteTexture(handle);
    }
}