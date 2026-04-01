package program.bootstrap.shaderpipeline.spritemanager;

import program.core.app.CoreContext;
import java.awt.image.BufferedImage;

import program.core.util.graphics.gl.GL20;
import program.core.util.image.Pixmap;
import program.core.engine.UtilityPackage;
import program.core.util.PixmapUtility;

/*
 * GL20 wrapper for individual sprite texture operations. Handles upload
 * and deletion only — format conversion is delegated to PixmapUtility.
 */
class GLSLUtility extends UtilityPackage {

    // GPU Upload \\

    static int pushSprite(BufferedImage image) {

        int handle = CoreContext.gl.glGenTexture();

        if (handle == 0)
            throwException("GPU handle could not be generated for sprite");

        CoreContext.gl.glBindTexture(GL20.GL_TEXTURE_2D, handle);
        CoreContext.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
        CoreContext.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);
        CoreContext.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_LINEAR);
        CoreContext.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_LINEAR);

        Pixmap pixmap = PixmapUtility.fromBufferedImage(image, true);

        CoreContext.gl.glTexImage2D(
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
        CoreContext.gl.glBindTexture(GL20.GL_TEXTURE_2D, 0);

        return handle;
    }

    // GPU Disposal \\

    static void deleteSprite(int handle) {

        if (handle == 0)
            return;

        CoreContext.gl.glBindTexture(GL20.GL_TEXTURE_2D, 0);
        CoreContext.gl.glDeleteTexture(handle);
    }
}
