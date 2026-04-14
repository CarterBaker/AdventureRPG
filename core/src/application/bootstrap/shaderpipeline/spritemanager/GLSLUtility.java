package application.bootstrap.shaderpipeline.spritemanager;

import java.awt.image.BufferedImage;

import engine.root.EngineContext;
import engine.root.EngineUtility;
import engine.util.assets.image.Pixmap;
import engine.util.assets.image.PixmapUtility;
import engine.util.graphics.gl.GL20;

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

        EngineContext.gl20.glBindTexture(GL20.GL_TEXTURE_2D, handle);
        EngineContext.gl20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
        EngineContext.gl20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);
        EngineContext.gl20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_LINEAR);
        EngineContext.gl20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_LINEAR);

        Pixmap pixmap = PixmapUtility.fromBufferedImage(image, true);

        EngineContext.gl20.glTexImage2D(
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
        EngineContext.gl20.glBindTexture(GL20.GL_TEXTURE_2D, 0);

        return handle;
    }

    // GPU Disposal \\

    static void deleteSprite(int handle) {

        if (handle == 0)
            return;

        EngineContext.gl20.glBindTexture(GL20.GL_TEXTURE_2D, 0);
        EngineContext.gl20.glDeleteTexture(handle);
    }
}
