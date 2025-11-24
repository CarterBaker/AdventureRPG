package com.AdventureRPG.Core.RenderPipeline.Util;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.AdventureRPG.Core.Bootstrap.EngineConstant;
import com.AdventureRPG.Core.RenderPipeline.RenderableInstance.MeshData;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.BufferUtils;

public final class GPUCall {

    // Buffer \\

    public static void enableDepth() {

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        Gdx.gl.glDepthMask(true);
    }

    public static void disableDepth() {

        Gdx.gl.glDepthMask(false);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
    }

    public static void clearBuffer() {

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT);
    }

    // Texture Management \\

    public static int pushTextureArray(BufferedImage[] layers) {

        int depth = layers.length;

        if (depth == 0) // TODO: Add my own error
            throw new IllegalStateException("Texture array contains no layers.");

        // Ensure all layers are the same size
        BufferedImage first = layers[0];
        int width = first.getWidth();
        int height = first.getHeight();

        for (int i = 1; i < depth; i++) {

            BufferedImage b = layers[i];

            if (b.getWidth() != width || b.getHeight() != height) // TODO: Add my own error
                throw new IllegalStateException("All layers must be same dimensions.");
        }

        // ensure GL30 available
        if (!(Gdx.gl instanceof GL30)) // TODO: Add my own error
            throw new IllegalStateException("GL30 required for texture arrays.");

        GL30 gl30 = (GL30) Gdx.gl;

        // generate texture handle
        int handle = Gdx.gl.glGenTexture();

        if (handle == 0) // TODO: Add my own error
            throw new IllegalStateException("glGenTextures returned 0");

        // bind and allocate empty storage
        Gdx.gl.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, handle);

        // Allocate immutable-like storage. Use GL_RGBA8 internal format.
        gl30.glTexImage3D(
                GL30.GL_TEXTURE_2D_ARRAY,
                0, // level
                GL30.GL_RGBA8, // internal format
                width,
                height,
                depth, // number of layers
                0, // border
                GL30.GL_RGBA, // format (for texsub)
                GL30.GL_UNSIGNED_BYTE,
                (ByteBuffer) null // no data yet
        );

        // Upload each layer via TexSubImage3D
        for (int layer = 0; layer < depth; layer++) {

            BufferedImage img = layers[layer];
            Pixmap pix = convertBufferedImageToPixmapRGBA(img);

            // Reset position to 0 to be safe
            ByteBuffer pixels = pix.getPixels();
            pixels.position(0);

            // Upload slice
            gl30.glTexSubImage3D(
                    GL30.GL_TEXTURE_2D_ARRAY,
                    0, // level
                    0, 0, layer, // xoffset, yoffset, zoffset
                    width,
                    height,
                    1, // depth = 1 for single slice
                    GL30.GL_RGBA,
                    GL30.GL_UNSIGNED_BYTE,
                    pixels);

            // dispose temporary pixmap
            pix.dispose();
        }

        // Set texture parameters
        Gdx.gl.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_NEAREST);
        Gdx.gl.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_NEAREST);
        Gdx.gl.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
        Gdx.gl.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);

        // Unbind
        Gdx.gl.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);

        return handle;
    }

    private static Pixmap convertBufferedImageToPixmapRGBA(BufferedImage img) {

        final int w = img.getWidth();
        final int h = img.getHeight();

        // Allocate the pixmap
        Pixmap pix = new Pixmap(w, h, Pixmap.Format.RGBA8888);

        // Read all ARGB pixels from the BufferedImage
        int[] argb = new int[w * h];
        img.getRGB(0, 0, w, h, argb, 0, w);

        // Convert each pixel to RGBA where R is highest byte (LibGDX standard)
        int idx = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                int c = argb[idx++];

                int a = (c >> 24) & 0xFF;
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = (c) & 0xFF;

                // Pack into LibGDX RGBA8888 (R highest byte, A lowest)
                int rgba = (r << 24) |
                        (g << 16) |
                        (b << 8) |
                        a;

                pix.drawPixel(x, y, rgba);
            }
        }

        return pix;
    }

    // Push Data \\

    public static GPUHandle pushData(MeshData data) {

        if (data == null)
            return null;

        float[] verts = data.getVerticesArray();
        short[] inds = data.getIndicesArray();

        int vbo = uploadVerticesToGPU(verts);
        int ibo = uploadIndicesToGPU(inds);

        return new GPUHandle(vbo, ibo);
    }

    private static int uploadVerticesToGPU(float[] vertices) {

        if (vertices == null || vertices.length == 0)
            return 0;

        int vbo = glGenBuffer();

        Gdx.gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        FloatBuffer buffer = BufferUtils.newFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        Gdx.gl.glBufferData(GL20.GL_ARRAY_BUFFER, buffer.limit() * 4, buffer, GL20.GL_STATIC_DRAW);
        Gdx.gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);

        return vbo;
    }

    private static int uploadIndicesToGPU(short[] indices) {

        if (indices == null || indices.length == 0)
            return 0;

        int ibo = glGenBuffer();

        Gdx.gl.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, ibo);
        ShortBuffer buffer = BufferUtils.newShortBuffer(indices.length);
        buffer.put(indices).flip();
        Gdx.gl.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, buffer.limit() * 2, buffer, GL20.GL_STATIC_DRAW);
        Gdx.gl.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);

        return ibo;
    }

    private static int glGenBuffer() {

        IntBuffer tmp = BufferUtils.newIntBuffer(1);
        Gdx.gl.glGenBuffers(1, tmp);

        return tmp.get(0);
    }

    // Remove Data \\

    public static void removeData(GPUHandle handle) {

        if (handle == null)
            return;

        freeBuffer(handle.vbo);
        freeBuffer(handle.ibo);
    }

    private static void freeBuffer(int bufferHandle) {

        if (bufferHandle == 0)
            return;

        IntBuffer tmp = BufferUtils.newIntBuffer(1);
        tmp.put(bufferHandle).flip();
        Gdx.gl.glDeleteBuffers(1, tmp);
    }

    // Render \\

    public static void bind(GPUHandle handle) {
        if (handle == null)
            return;

        // Bind buffers
        Gdx.gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, handle.vbo);
        Gdx.gl.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, handle.ibo);

        int stride = EngineConstant.VERT_STRIDE * 4; // bytes

        // Position (location 0)
        Gdx.gl.glEnableVertexAttribArray(0);
        Gdx.gl.glVertexAttribPointer(0, 3, GL20.GL_FLOAT, false, stride, 0);

        // Normal (location 1)
        Gdx.gl.glEnableVertexAttribArray(1);
        Gdx.gl.glVertexAttribPointer(1, 3, GL20.GL_FLOAT, false, stride, 3 * 4);

        // Color (location 2)
        Gdx.gl.glEnableVertexAttribArray(2);
        Gdx.gl.glVertexAttribPointer(2, 3, GL20.GL_FLOAT, false, stride, 6 * 4);

        // UV (location 3) – if you have u,v stored as last two floats
        Gdx.gl.glEnableVertexAttribArray(3);
        Gdx.gl.glVertexAttribPointer(3, 2, GL20.GL_FLOAT, false, stride, 9 * 4);
    }

    public static void unbind(GPUHandle handle) {
        if (handle == null)
            return;

        Gdx.gl.glDisableVertexAttribArray(0);
        Gdx.gl.glDisableVertexAttribArray(1);
        Gdx.gl.glDisableVertexAttribArray(2);
        Gdx.gl.glDisableVertexAttribArray(3);

        Gdx.gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
        Gdx.gl.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public static void draw(GPUHandle handle, int vertexCount) {
        if (handle == null || vertexCount <= 0)
            return;

        Gdx.gl.glDrawElements(GL20.GL_TRIANGLES, vertexCount, GL20.GL_UNSIGNED_SHORT, 0);
    }

}
