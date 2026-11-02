package application.bootstrap.geometrypipeline.skinnedbuffermanager;

import java.nio.FloatBuffer;

import engine.graphics.gl.GL20;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.memory.BufferUtility;

class SkinnedBufferGLSLUtility extends EngineUtility {

    /*
     * GL creation, upload, and disposal operations for SkinnedBufferManager.
     * Package-private — only SkinnedBufferManager may call these. The
     * palette is an RGBA32F 2D texture, one row per instance and as wide as
     * the buffer's row: its bones first (3 texels encode one mat4x3 skinning
     * matrix, dropping the implicit [0,0,0,1] last row), then the appearance
     * texels and the morph weights. Sampled with texelFetch — never filtered,
     * since this is data, not an image. Uploads reuse one growable direct
     * buffer each for instances and palette rows.
     */

    // Scratch
    private static FloatBuffer instanceScratch;
    private static FloatBuffer paletteScratch;

    // Instance VBO \\

    static int createDynamicInstanceVBO(int maxInstances) {

        GL20 gl20 = EngineContext.gl20;
        int size = maxInstances * EngineSetting.SKINNED_INSTANCE_FLOATS * Float.BYTES;

        int vbo = gl20.glGenBuffer();
        gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, vbo);
        gl20.glBufferData(EngineSetting.GL_ARRAY_BUFFER, size, null, EngineSetting.GL_DYNAMIC_DRAW);
        gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, 0);

        return vbo;
    }

    static void uploadInstanceVBO(int vbo, float[] instanceData, int floatCount) {

        GL20 gl20 = EngineContext.gl20;

        instanceScratch = BufferUtility.ensureCapacity(instanceScratch, floatCount);
        instanceScratch.put(instanceData, 0, floatCount).flip();

        gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, vbo);
        gl20.glBufferSubData(EngineSetting.GL_ARRAY_BUFFER, 0, floatCount * Float.BYTES, instanceScratch);
        gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, 0);
    }

    // Palette Texture \\

    static int createPaletteTexture(int width, int maxInstances) {

        GL20 gl20 = EngineContext.gl20;

        int texture = gl20.glGenTexture();
        gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, texture);
        gl20.glTexImage2D(
                EngineSetting.GL_TEXTURE_2D, 0, EngineSetting.GL_RGBA32F,
                width, maxInstances, 0,
                EngineSetting.GL_RGBA, EngineSetting.GL_FLOAT, (FloatBuffer) null);
        gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_MIN_FILTER,
                EngineSetting.GL_NEAREST);
        gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_MAG_FILTER,
                EngineSetting.GL_NEAREST);
        gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_WRAP_S,
                EngineSetting.GL_CLAMP_TO_EDGE);
        gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_WRAP_T,
                EngineSetting.GL_CLAMP_TO_EDGE);
        gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_HANDLE_NONE);

        return texture;
    }

    static void uploadPalette(int texture, float[] paletteData, int width, int instanceCount) {

        GL20 gl20 = EngineContext.gl20;
        int floatCount = width * instanceCount * EngineSetting.COLOR_CHANNEL_COUNT;

        paletteScratch = BufferUtility.ensureCapacity(paletteScratch, floatCount);
        paletteScratch.put(paletteData, 0, floatCount).flip();

        gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, texture);
        gl20.glTexSubImage2D(
                EngineSetting.GL_TEXTURE_2D, 0,
                0, 0, width, instanceCount,
                EngineSetting.GL_RGBA, EngineSetting.GL_FLOAT, paletteScratch);
        gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_HANDLE_NONE);
    }

    // Disposal \\

    static void deleteBuffer(int handle) {
        if (handle != 0)
            EngineContext.gl20.glDeleteBuffer(handle);
    }

    static void deleteTexture(int handle) {
        if (handle != 0)
            EngineContext.gl20.glDeleteTexture(handle);
    }
}