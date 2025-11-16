package com.AdventureRPG.Core.RenderPipeline.Util;

import com.AdventureRPG.Core.RenderPipeline.RenderableInstance.MeshData;
import com.AdventureRPG.Core.Util.GlobalConstant;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

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

        int stride = GlobalConstant.VERT_STRIDE * 4; // bytes

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
