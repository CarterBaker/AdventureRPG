package application.bootstrap.shaderpipeline.texturemanager;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import engine.assets.image.Pixmap;
import engine.assets.image.PixmapUtility;
import engine.graphics.gl.GL20;
import engine.graphics.gl.GL30;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

/*
 * GL30 wrapper for texture array operations. Handles upload and deletion only.
 * Pixel conversion is delegated to PixmapUtility. Atlas images are already
 * flipped at composite time — no flip applied here.
 */
class TextureGLSLUtility extends EngineUtility {

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

        if (!(EngineContext.gl20 instanceof GL30))
            throwException("GL30 required for texture arrays");

        GL30 gl30 = (GL30) EngineContext.gl20;
        int handle = EngineContext.gl20.glGenTexture();

        if (handle == 0)
            throwException("GPU handle could not be generated for texture array");

        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D_ARRAY, handle);

        gl30.glTexImage3D(
                EngineSetting.GL_TEXTURE_2D_ARRAY,
                0,
                EngineSetting.GL_RGBA8,
                width, height, depth,
                0,
                EngineSetting.GL_RGBA,
                EngineSetting.GL_UNSIGNED_BYTE,
                (ByteBuffer) null);

        for (int layer = 0; layer < depth; layer++) {
            Pixmap pix = PixmapUtility.fromBufferedImage(layers[layer], false);
            ByteBuffer pixels = pix.getPixels();
            pixels.position(0);
            gl30.glTexSubImage3D(
                    EngineSetting.GL_TEXTURE_2D_ARRAY,
                    0,
                    0, 0, layer,
                    width, height, 1,
                    EngineSetting.GL_RGBA,
                    EngineSetting.GL_UNSIGNED_BYTE,
                    pixels);
            pix.dispose();
        }

        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D_ARRAY, EngineSetting.GL_TEXTURE_MIN_FILTER,
                EngineSetting.GL_NEAREST);
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D_ARRAY, EngineSetting.GL_TEXTURE_MAG_FILTER,
                EngineSetting.GL_NEAREST);
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D_ARRAY, EngineSetting.GL_TEXTURE_WRAP_S,
                EngineSetting.GL_CLAMP_TO_EDGE);
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D_ARRAY, EngineSetting.GL_TEXTURE_WRAP_T,
                EngineSetting.GL_CLAMP_TO_EDGE);

        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D_ARRAY, EngineSetting.GL_HANDLE_NONE);

        return handle;
    }

    static int createFloatTexture2D(float[] pixels, int width, int height, int wrapMode, int filterMode) {
        java.nio.FloatBuffer buffer = engine.util.memory.BufferUtils.newFloatBuffer(pixels.length);
        buffer.put(pixels).flip();

        int handle = EngineContext.gl20.glGenTexture();
        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, handle);
        EngineContext.gl20.glTexImage2D(
                EngineSetting.GL_TEXTURE_2D, 0, EngineSetting.GL_RGB32F,
                width, height, 0, EngineSetting.GL_RGB, EngineSetting.GL_FLOAT, buffer);
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_WRAP_S, wrapMode);
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_WRAP_T, wrapMode);
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_MIN_FILTER,
                filterMode);
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_MAG_FILTER,
                filterMode);
        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_HANDLE_NONE);

        return handle;
    }

    // GPU Disposal \\

    static void deleteTextureArray(int handle) {

        if (handle == 0)
            return;

        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D_ARRAY, 0);
        EngineContext.gl20.glDeleteTexture(handle);
    }
}