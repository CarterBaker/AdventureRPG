package application.bootstrap.menupipeline.fontmanager;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import engine.graphics.GL20;
import engine.root.EngineContext;
import engine.root.EngineUtility;
import engine.util.PixmapUtility;
import engine.util.image.Pixmap;

class GLSLUtility extends EngineUtility {

    /*
     * GL20 2D texture operations for the font pipeline. Uploads a single RGBA
     * BufferedImage to a GPU texture and releases handles on disposal.
     * platform GL calls are isolated here — nothing above this class imports
     * platform.
     * Package-private — only FontManager may call these.
     */

    // Texture Management \\

    static int pushTexture2D(BufferedImage image) {

        int width = image.getWidth();
        int height = image.getHeight();
        int handle = EngineContext.gl.glGenTexture();

        if (handle == 0)
            throwException("[FontGLSLUtility] Failed to generate GPU texture handle");

        EngineContext.gl.glBindTexture(GL20.GL_TEXTURE_2D, handle);

        Pixmap pix = PixmapUtility.fromBufferedImage(image, false);
        ByteBuffer pixels = pix.getPixels();
        pixels.position(0);

        EngineContext.gl.glTexImage2D(
                GL20.GL_TEXTURE_2D,
                0,
                GL20.GL_RGBA,
                width, height,
                0,
                GL20.GL_RGBA,
                GL20.GL_UNSIGNED_BYTE,
                pixels);

        pix.dispose();

        EngineContext.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_LINEAR);
        EngineContext.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_LINEAR);
        EngineContext.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
        EngineContext.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);

        EngineContext.gl.glBindTexture(GL20.GL_TEXTURE_2D, 0);

        return handle;
    }

    static void deleteTexture2D(int handle) {

        if (handle == 0)
            return;

        EngineContext.gl.glBindTexture(GL20.GL_TEXTURE_2D, 0);
        EngineContext.gl.glDeleteTexture(handle);
    }
}
