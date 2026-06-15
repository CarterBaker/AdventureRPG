package application.bootstrap.shaderpipeline.spritemanager;

import java.awt.image.BufferedImage;

import engine.assets.image.Pixmap;
import engine.assets.image.PixmapUtility;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

/*
 * GL20 wrapper for individual sprite texture operations. Handles upload
 * and deletion only — format conversion is delegated to PixmapUtility.
 */
class GLSLUtility extends EngineUtility {

    // GPU Upload \\

    static int pushSprite(BufferedImage image) {

        int handle = EngineContext.gl20.glGenTexture();

        if (handle == 0)
            throwException("GPU handle could not be generated for sprite");

        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, handle);
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_WRAP_S,
                EngineSetting.GL_CLAMP_TO_EDGE);
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_WRAP_T,
                EngineSetting.GL_CLAMP_TO_EDGE);
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_MIN_FILTER,
                EngineSetting.GL_LINEAR);
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_MAG_FILTER,
                EngineSetting.GL_LINEAR);

        Pixmap pixmap = PixmapUtility.fromBufferedImage(image, true);

        EngineContext.gl20.glTexImage2D(
                EngineSetting.GL_TEXTURE_2D,
                0,
                EngineSetting.GL_RGBA,
                pixmap.getWidth(),
                pixmap.getHeight(),
                0,
                EngineSetting.GL_RGBA,
                EngineSetting.GL_UNSIGNED_BYTE,
                pixmap.getPixels());

        pixmap.dispose();
        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, 0);

        return handle;
    }

    // GPU Disposal \\

    static void deleteSprite(int handle) {

        if (handle == 0)
            return;

        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, 0);
        EngineContext.gl20.glDeleteTexture(handle);
    }
}
