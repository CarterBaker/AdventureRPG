package application.bootstrap.shaderpipeline.texturemanager;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import engine.graphics.gl.GL40;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.memory.BufferUtility;

class TextureGLSLUtility extends EngineUtility {

    /*
     * GL wrapper for the engine's textures: plain 2D textures, the
     * block-compressed tile pools with their full mip chains, the integer
     * tile lookup, the unpack buffers tile data is staged through, and the
     * capability queries the pools are checked against. Upload and deletion
     * only; layers arrive rows bottom first, as the GPU reads them.
     */

    // Texture 2D \\

    static int createFloatTexture2D(float[] pixels, int width, int height, int wrapMode, int filterMode) {
        FloatBuffer buffer = BufferUtility.newFloatBuffer(pixels.length);
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

    static int createTexture2D(int width, int height, int wrapMode, int filterMode) {

        int handle = EngineContext.gl20.glGenTexture();

        if (handle == 0)
            throwException("GPU handle could not be generated for a " + width + "x" + height + " texture");

        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, handle);
        EngineContext.gl20.glTexImage2D(
                EngineSetting.GL_TEXTURE_2D, 0, EngineSetting.GL_RGBA8,
                width, height, 0, EngineSetting.GL_RGBA, EngineSetting.GL_UNSIGNED_BYTE,
                BufferUtility.newByteBuffer(width * height * EngineSetting.COLOR_CHANNEL_COUNT));
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_WRAP_S, wrapMode);
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_WRAP_T, wrapMode);
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_MIN_FILTER,
                filterMode);
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_MAG_FILTER,
                filterMode);
        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_HANDLE_NONE);

        return handle;
    }

    // RGBA8 rows are always four-byte aligned, so any region uploads under the default unpack alignment
    static void updateTexture2D(int handle, int x, int y, int width, int height, ByteBuffer pixels) {

        pixels.position(0);

        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, handle);
        EngineContext.gl20.glTexSubImage2D(
                EngineSetting.GL_TEXTURE_2D, 0, x, y, width, height,
                EngineSetting.GL_RGBA, EngineSetting.GL_UNSIGNED_BYTE, pixels);
        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_HANDLE_NONE);
    }

    // Tile Pools \\

    static int generateTexture() {

        int handle = EngineContext.gl20.glGenTexture();

        if (handle == EngineSetting.GL_HANDLE_NONE)
            throwException("GPU handle could not be generated for a texture");

        return handle;
    }

    // Gives a compressed array storage for every mip of the given square size, filtered trilinearly and repeating
    static void allocateCompressedArray(int handle, int format, int size, int layers, float anisotropy) {

        GL40 gl = EngineContext.gl40;
        int levels = Integer.numberOfTrailingZeros(size) + 1;

        gl.glBindTexture(EngineSetting.GL_TEXTURE_2D_ARRAY, handle);

        for (int level = 0, edge = size; level < levels; level++, edge >>= 1)
            gl.glCompressedTexImage3D(EngineSetting.GL_TEXTURE_2D_ARRAY, level, format, edge, edge, layers, 0,
                    TextureCompressionUtility.getLevelBytes(edge, edge) * layers, 0L);

        gl.glTexParameteri(EngineSetting.GL_TEXTURE_2D_ARRAY, EngineSetting.GL_TEXTURE_BASE_LEVEL, 0);
        gl.glTexParameteri(EngineSetting.GL_TEXTURE_2D_ARRAY, EngineSetting.GL_TEXTURE_MAX_LEVEL, levels - 1);
        gl.glTexParameteri(EngineSetting.GL_TEXTURE_2D_ARRAY, EngineSetting.GL_TEXTURE_MIN_FILTER,
                EngineSetting.GL_LINEAR_MIPMAP_LINEAR);
        gl.glTexParameteri(EngineSetting.GL_TEXTURE_2D_ARRAY, EngineSetting.GL_TEXTURE_MAG_FILTER,
                EngineSetting.GL_LINEAR);
        gl.glTexParameteri(EngineSetting.GL_TEXTURE_2D_ARRAY, EngineSetting.GL_TEXTURE_WRAP_S,
                EngineSetting.GL_REPEAT);
        gl.glTexParameteri(EngineSetting.GL_TEXTURE_2D_ARRAY, EngineSetting.GL_TEXTURE_WRAP_T,
                EngineSetting.GL_REPEAT);
        applyAnisotropy(gl, anisotropy);
        gl.glBindTexture(EngineSetting.GL_TEXTURE_2D_ARRAY, EngineSetting.GL_HANDLE_NONE);
    }

    static void setAnisotropy(int handle, float anisotropy) {

        GL40 gl = EngineContext.gl40;

        gl.glBindTexture(EngineSetting.GL_TEXTURE_2D_ARRAY, handle);
        applyAnisotropy(gl, anisotropy);
        gl.glBindTexture(EngineSetting.GL_TEXTURE_2D_ARRAY, EngineSetting.GL_HANDLE_NONE);
    }

    // Anisotropy one means none, so a driver without the extension is never asked for it
    private static void applyAnisotropy(GL40 gl, float anisotropy) {
        if (anisotropy > 1f)
            gl.glTexParameterf(EngineSetting.GL_TEXTURE_2D_ARRAY, EngineSetting.GL_TEXTURE_MAX_ANISOTROPY,
                    anisotropy);
    }

    // One layer's whole chain from the bound unpack buffer, its levels back to back from the given offset
    static void uploadCompressedLayer(int handle, int format, int size, int layer, long offset) {

        GL40 gl = EngineContext.gl40;
        long position = offset;

        gl.glBindTexture(EngineSetting.GL_TEXTURE_2D_ARRAY, handle);

        for (int level = 0, edge = size; edge >= 1; level++, edge >>= 1) {

            int bytes = TextureCompressionUtility.getLevelBytes(edge, edge);
            gl.glCompressedTexSubImage3D(EngineSetting.GL_TEXTURE_2D_ARRAY, level, 0, 0, layer, edge, edge, 1,
                    format, bytes, position);
            position += bytes;
        }

        gl.glBindTexture(EngineSetting.GL_TEXTURE_2D_ARRAY, EngineSetting.GL_HANDLE_NONE);
    }

    // The tile lookup: two unsigned words per texel, read with texelFetch only
    static int createLookupTexture(int width, int height, IntBuffer words) {

        int handle = generateTexture();

        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, handle);
        EngineContext.gl20.glTexImage2D(EngineSetting.GL_TEXTURE_2D, 0, EngineSetting.GL_RG32UI, width, height, 0,
                EngineSetting.GL_RG_INTEGER, EngineSetting.GL_UNSIGNED_INT, words);
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_MIN_FILTER,
                EngineSetting.GL_NEAREST);
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_MAG_FILTER,
                EngineSetting.GL_NEAREST);
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_WRAP_S,
                EngineSetting.GL_CLAMP_TO_EDGE);
        EngineContext.gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_WRAP_T,
                EngineSetting.GL_CLAMP_TO_EDGE);
        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_HANDLE_NONE);

        return handle;
    }

    // Rows of the lookup replaced, the words positioned at the first row's first texel
    static void updateLookupRows(int handle, int firstRow, int rows, int width, IntBuffer words) {

        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, handle);
        EngineContext.gl20.glTexSubImage2D(EngineSetting.GL_TEXTURE_2D, 0, 0, firstRow, width, rows,
                EngineSetting.GL_RG_INTEGER, EngineSetting.GL_UNSIGNED_INT, words);
        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_HANDLE_NONE);
    }

    // Staging \\

    static int generateStagingBuffer() {
        return EngineContext.gl20.glGenBuffer();
    }

    // Orphans the buffer's old storage, fills it from the staged bytes and leaves it bound for the uploads that read it
    static void bindStaged(int buffer, int capacity, ByteBuffer staged) {
        EngineContext.gl20.glBindBuffer(EngineSetting.GL_PIXEL_UNPACK_BUFFER, buffer);
        EngineContext.gl20.glBufferData(EngineSetting.GL_PIXEL_UNPACK_BUFFER, capacity, null,
                EngineSetting.GL_STREAM_DRAW);
        EngineContext.gl20.glBufferSubData(EngineSetting.GL_PIXEL_UNPACK_BUFFER, 0, staged.remaining(), staged);
    }

    static void unbindStaged() {
        EngineContext.gl20.glBindBuffer(EngineSetting.GL_PIXEL_UNPACK_BUFFER, EngineSetting.GL_HANDLE_NONE);
    }

    static void deleteStagingBuffer(int buffer) {
        if (buffer != EngineSetting.GL_HANDLE_NONE)
            EngineContext.gl20.glDeleteBuffer(buffer);
    }

    // Capabilities \\

    static int getInteger(int name) {
        return EngineContext.gl40.glGetInteger(name);
    }

    static float getFloat(int name) {
        return EngineContext.gl40.glGetFloat(name);
    }

    static boolean hasExtension(String extension) {

        int count = EngineContext.gl40.glGetInteger(EngineSetting.GL_NUM_EXTENSIONS);

        for (int i = 0; i < count; i++)
            if (extension.equals(EngineContext.gl40.glGetStringi(EngineSetting.GL_EXTENSIONS, i)))
                return true;

        return false;
    }

    // GPU Disposal \\

    static void deleteTexture2D(int handle) {

        if (handle == 0)
            return;

        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_HANDLE_NONE);
        EngineContext.gl20.glDeleteTexture(handle);
    }

    static void deleteTextureArray(int handle) {

        if (handle == 0)
            return;

        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D_ARRAY, 0);
        EngineContext.gl20.glDeleteTexture(handle);
    }
}