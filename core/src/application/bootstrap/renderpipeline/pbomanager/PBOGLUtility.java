package application.bootstrap.renderpipeline.pbomanager;

import java.nio.ByteBuffer;

import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

class PBOGLUtility extends EngineUtility {

    /*
     * Stateless OpenGL helpers for the PBO package. Covers pixel-pack
     * buffer generation, binding, storage allocation, the asynchronous
     * front-buffer readback that starts a transfer without blocking, and
     * the buffer-range mapping used to retrieve a transfer once the GPU
     * has finished it. Package-private — only PboManager may call these.
     */

    static int genPackBuffer() {
        return EngineContext.gl20.glGenBuffer();
    }

    static void bindPackBuffer(int pbo) {
        EngineContext.gl20.glBindBuffer(EngineSetting.GL_PIXEL_PACK_BUFFER, pbo);
    }

    static void unbindPackBuffer() {
        EngineContext.gl20.glBindBuffer(EngineSetting.GL_PIXEL_PACK_BUFFER, EngineSetting.GL_HANDLE_NONE);
    }

    static void allocatePackBuffer(int byteCount) {
        EngineContext.gl20.glBufferData(
                EngineSetting.GL_PIXEL_PACK_BUFFER, byteCount, null, EngineSetting.GL_STREAM_READ);
    }

    static void queueRead(int width, int height) {
        EngineContext.gl20.glReadBuffer(EngineSetting.GL_FRONT);
        EngineContext.gl20.glReadPixels(
                0, 0, width, height,
                EngineSetting.GL_BGRA, EngineSetting.GL_UNSIGNED_BYTE, 0L);
    }

    static void retrieve(int byteCount, ByteBuffer destination) {

        ByteBuffer mapped = EngineContext.gl20.glMapBufferRange(
                EngineSetting.GL_PIXEL_PACK_BUFFER, 0L, byteCount, EngineSetting.GL_MAP_READ_BIT);

        destination.clear();
        destination.put(mapped);
        destination.flip();

        if (!EngineContext.gl20.glUnmapBuffer(EngineSetting.GL_PIXEL_PACK_BUFFER))
            throwException("PBO readback buffer was invalidated before it could be read — GPU state was lost.");
    }
}