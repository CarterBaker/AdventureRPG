package application.bootstrap.geometrypipeline.quadbuffermanager;

import java.nio.IntBuffer;

import engine.graphics.gl.GL20;
import engine.graphics.gl.GL30;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.memory.BufferUtility;

class QuadBufferGLSLUtility extends EngineUtility {

    /*
     * GL creation, upload and deletion for QuadBufferManager. Records reach
     * the GPU through one reusable direct scratch buffer, read straight from
     * the caller's backing array up to its live count. Storage is respecified
     * only when records outgrow it or shrink well below it, so most uploads
     * rewrite the buffer in place and the buffer texture viewing it stays
     * attached. Package-private.
     */

    // Scratch
    private static IntBuffer intScratch;

    // Creation \\

    static int createBuffer() {
        return EngineContext.gl20.glGenBuffer();
    }

    static int createTexture(int bufferHandle, int internalFormat) {

        GL30 gl30 = EngineContext.gl30;
        int texture = gl30.glGenTexture();

        gl30.glBindTexture(EngineSetting.GL_TEXTURE_BUFFER, texture);
        gl30.glTexBuffer(EngineSetting.GL_TEXTURE_BUFFER, internalFormat, bufferHandle);
        gl30.glBindTexture(EngineSetting.GL_TEXTURE_BUFFER, EngineSetting.GL_HANDLE_NONE);

        return texture;
    }

    // Upload \\

    // The storage the buffer holds afterwards, in ints
    static int upload(int bufferHandle, int capacityInts, int[] records, int intCount) {

        GL20 gl20 = EngineContext.gl20;
        int size = intCount * Integer.BYTES;

        intScratch = BufferUtility.ensureCapacity(intScratch, intCount);
        intScratch.put(records, 0, intCount).flip();

        gl20.glBindBuffer(EngineSetting.GL_TEXTURE_BUFFER, bufferHandle);

        if (intCount > capacityInts || intCount < capacityInts / EngineSetting.QUAD_BUFFER_SHRINK_DIVISOR) {
            gl20.glBufferData(EngineSetting.GL_TEXTURE_BUFFER, size, intScratch, EngineSetting.GL_STATIC_DRAW);
            capacityInts = intCount;
        } else
            gl20.glBufferSubData(EngineSetting.GL_TEXTURE_BUFFER, 0, size, intScratch);

        gl20.glBindBuffer(EngineSetting.GL_TEXTURE_BUFFER, EngineSetting.GL_HANDLE_NONE);

        return capacityInts;
    }

    // The buffer's storage given back while it waits for its next records
    static void empty(int bufferHandle) {

        GL20 gl20 = EngineContext.gl20;

        gl20.glBindBuffer(EngineSetting.GL_TEXTURE_BUFFER, bufferHandle);
        gl20.glBufferData(EngineSetting.GL_TEXTURE_BUFFER, 0, null, EngineSetting.GL_STATIC_DRAW);
        gl20.glBindBuffer(EngineSetting.GL_TEXTURE_BUFFER, EngineSetting.GL_HANDLE_NONE);
    }

    // Removal \\

    static void delete(int bufferHandle, int textureHandle) {
        EngineContext.gl20.glDeleteTexture(textureHandle);
        EngineContext.gl20.glDeleteBuffer(bufferHandle);
    }
}
