package program.bootstrap.menupipeline.fontmanager;

import program.core.app.CoreContext;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import program.core.util.graphics.gl.GL20;
import program.core.util.image.Pixmap;
import program.core.engine.UtilityPackage;
import program.core.util.PixmapUtility;

class GLSLUtility extends UtilityPackage {

    /*
     * GL20 2D texture operations for the font pipeline. Uploads a single RGBA
     * BufferedImage to a GPU texture and releases handles on disposal.
     * platform GL calls are isolated here — nothing above this class imports platform.
     * Package-private — only FontManager may call these.
     */

    // Texture Management \\

    static int pushTexture2D(BufferedImage image) {

        int width = image.getWidth();
        int height = image.getHeight();
        int handle = CoreContext.gl.glGenTexture();

        if (handle == 0)
            throwException("[FontGLSLUtility] Failed to generate GPU texture handle");

        CoreContext.gl.glBindTexture(GL20.GL_TEXTURE_2D, handle);

        Pixmap pix = PixmapUtility.fromBufferedImage(image, false);
        ByteBuffer pixels = pix.getPixels();
        pixels.position(0);

        CoreContext.gl.glTexImage2D(
                GL20.GL_TEXTURE_2D,
                0,
                GL20.GL_RGBA,
                width, height,
                0,
                GL20.GL_RGBA,
                GL20.GL_UNSIGNED_BYTE,
                pixels);

        pix.dispose();

        CoreContext.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_LINEAR);
        CoreContext.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_LINEAR);
        CoreContext.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
        CoreContext.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);

        CoreContext.gl.glBindTexture(GL20.GL_TEXTURE_2D, 0);

        return handle;
    }

    static void deleteTexture2D(int handle) {

        if (handle == 0)
            return;

        CoreContext.gl.glBindTexture(GL20.GL_TEXTURE_2D, 0);
        CoreContext.gl.glDeleteTexture(handle);
    }
}
