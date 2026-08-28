package application.bootstrap.screencapturepipeline.screencapturemanager;

import java.nio.ByteBuffer;

import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

class ScreenCaptureGLUtility extends EngineUtility {

    /*
     * Stateless OpenGL pixel-readback helper shared by ScreenshotSystem and
     * VideoRecordingSystem. Reads are always taken from the front buffer so
     * capture never depends on where in the frame it happens relative to
     * RenderManager's own draw/swap sequence, and are always requested as
     * BGRA so every row lands on a four-byte boundary with no client-side
     * packing padding to account for downstream.
     */

    static void readFrontBuffer(int width, int height, ByteBuffer destination) {
        EngineContext.gl20.glReadBuffer(EngineSetting.GL_FRONT);
        EngineContext.gl20.glReadPixels(
                0, 0, width, height,
                EngineSetting.GL_BGRA, EngineSetting.GL_UNSIGNED_BYTE,
                destination);
    }
}