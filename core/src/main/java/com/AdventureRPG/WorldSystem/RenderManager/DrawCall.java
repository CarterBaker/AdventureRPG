package com.AdventureRPG.WorldSystem.RenderManager;

import com.AdventureRPG.MaterialManager.MaterialManager;
import com.AdventureRPG.Util.GlobalConstant;
import com.AdventureRPG.WorldSystem.MegaChunk.MegaChunk;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;

import java.nio.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public final class DrawCall {

    private final MaterialManager materialManager;
    private final MegaChunk megaChunk;
    private final RenderPacket renderPacket;

    private final Int2ObjectOpenHashMap<GpuBatch> gpuBatches = new Int2ObjectOpenHashMap<>();
    private final ObjectArrayList<RenderPacket.RenderKey> keys = new ObjectArrayList<>();

    private boolean uploaded = false; // <-- lazy upload marker

    public DrawCall(MaterialManager materialManager, MegaChunk megaChunk, RenderPacket renderPacket) {
        this.materialManager = materialManager;
        this.megaChunk = megaChunk;
        this.renderPacket = renderPacket;
    }

    private void uploadToGPU() {

        if (renderPacket == null)
            return;

        GL30 gl = Gdx.gl30;

        for (RenderPacket.RenderBatch batch : renderPacket.batches) {
            RenderPacket.RenderKey key = renderPacket.keys.get(batch.keyId);
            keys.add(key);

            if (gpuBatches.containsKey(batch.keyId))
                continue;

            FloatBuffer vBuf = ByteBuffer
                    .allocateDirect(batch.vertices.length * Float.BYTES)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(batch.vertices);
            vBuf.flip();

            ShortBuffer iBuf = ByteBuffer
                    .allocateDirect(batch.indices.length * Short.BYTES)
                    .order(ByteOrder.nativeOrder())
                    .asShortBuffer()
                    .put(batch.indices);
            iBuf.flip();

            IntBuffer tmp = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();

            GpuBatch gpu = new GpuBatch();
            gpu.indexCount = batch.indexCount;

            gl.glGenVertexArrays(1, tmp);
            gpu.vao = tmp.get(0);
            gl.glBindVertexArray(gpu.vao);

            tmp.clear();
            gl.glGenBuffers(1, tmp);
            gpu.vbo = tmp.get(0);
            gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, gpu.vbo);
            gl.glBufferData(GL20.GL_ARRAY_BUFFER, vBuf.capacity() * Float.BYTES, vBuf, GL20.GL_STATIC_DRAW);

            tmp.clear();
            gl.glGenBuffers(1, tmp);
            gpu.ibo = tmp.get(0);
            gl.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, gpu.ibo);
            gl.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, iBuf.capacity() * Short.BYTES, iBuf, GL20.GL_STATIC_DRAW);

            int stride = GlobalConstant.VERT_STRIDE * Float.BYTES;
            int offset = 0;
            gl.glEnableVertexAttribArray(0);
            gl.glVertexAttribPointer(0, 3, GL20.GL_FLOAT, false, stride, offset);
            offset += 3 * Float.BYTES;

            gl.glEnableVertexAttribArray(1);
            gl.glVertexAttribPointer(1, 3, GL20.GL_FLOAT, false, stride, offset);
            offset += 3 * Float.BYTES;

            gl.glEnableVertexAttribArray(2);
            gl.glVertexAttribPointer(2, 2, GL20.GL_FLOAT, false, stride, offset);

            gl.glBindVertexArray(0);
            gpuBatches.put(batch.keyId, gpu);
        }

        uploaded = true;
    }

    public void render() {
        // Lazy initialization on the render thread
        if (!uploaded) {
            uploadToGPU();
        }

        GL30 gl = Gdx.gl30;

        for (RenderPacket.RenderKey key : keys) {
            GpuBatch gpu = gpuBatches.get(key.id);
            if (gpu == null)
                continue;

            materialManager.setUniform(
                    key.id,
                    "u_transform",
                    megaChunk.renderPosition(),
                    true);

            key.shaderProgram.bind();
            key.textureArray.bind();

            gl.glBindVertexArray(gpu.vao);
            gl.glDrawElements(GL20.GL_TRIANGLES, gpu.indexCount, GL20.GL_UNSIGNED_SHORT, 0);
            gl.glBindVertexArray(0);
        }

        gl.glUseProgram(0);
    }

    public void dispose() {
        if (!uploaded)
            return; // nothing to delete yet

        GL30 gl = Gdx.gl30;
        IntBuffer tmp = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();

        for (GpuBatch gpu : gpuBatches.values()) {
            if (gpu.vbo != 0) {
                tmp.put(0, gpu.vbo);
                gl.glDeleteBuffers(1, tmp);
            }
            if (gpu.ibo != 0) {
                tmp.put(0, gpu.ibo);
                gl.glDeleteBuffers(1, tmp);
            }
            if (gpu.vao != 0) {
                tmp.put(0, gpu.vao);
                gl.glDeleteVertexArrays(1, tmp);
            }
        }

        gpuBatches.clear();
        keys.clear();
        uploaded = false;
    }

    private static final class GpuBatch {
        int vao, vbo, ibo;
        int indexCount;
    }
}
