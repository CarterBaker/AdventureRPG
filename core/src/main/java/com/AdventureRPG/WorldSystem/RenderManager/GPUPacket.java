package com.AdventureRPG.WorldSystem.RenderManager;

import com.AdventureRPG.MaterialManager.MaterialManager;
import com.AdventureRPG.Util.GlobalConstant;
import com.AdventureRPG.WorldSystem.MegaChunk.MegaChunk;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;

import java.nio.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public final class GPUPacket {

    private final MaterialManager materialManager;
    private final MegaChunk megaChunk;
    private final RenderPacket renderPacket;

    private final Int2ObjectOpenHashMap<ObjectArrayList<DrawCall>> drawCalls = new Int2ObjectOpenHashMap<>();
    private final ObjectArrayList<RenderPacket.RenderKey> keys = new ObjectArrayList<>();

    private boolean uploaded = false;

    public GPUPacket(MaterialManager materialManager, MegaChunk megaChunk, RenderPacket renderPacket) {

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

            // Ensure key appears only once in keys list
            if (!keys.contains(key))
                keys.add(key);

            // Fetch or create the list of GPU batches for this key id
            ObjectArrayList<DrawCall> list = drawCalls.get(batch.keyId);

            if (list == null) {

                list = new ObjectArrayList<>();
                drawCalls.put(batch.keyId, list);
            }

            // Create and upload a new GpuBatch for every RenderBatch (no skipping)
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

            DrawCall gpu = new DrawCall();
            gpu.indexCount = batch.indexCount;

            gl.glGenVertexArrays(1, tmp);
            gpu.vao = tmp.get(0);
            gl.glBindVertexArray(gpu.vao);

            tmp.clear();
            gl.glGenBuffers(1, tmp);
            gpu.vbo = tmp.get(0);
            gl.glBindBuffer(GL30.GL_ARRAY_BUFFER, gpu.vbo);
            gl.glBufferData(GL30.GL_ARRAY_BUFFER, vBuf.capacity() * Float.BYTES, vBuf, GL30.GL_STATIC_DRAW);

            tmp.clear();
            gl.glGenBuffers(1, tmp);
            gpu.ibo = tmp.get(0);
            gl.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, gpu.ibo);
            gl.glBufferData(GL30.GL_ELEMENT_ARRAY_BUFFER, iBuf.capacity() * Short.BYTES, iBuf, GL30.GL_STATIC_DRAW);

            int stride = GlobalConstant.VERT_STRIDE * Float.BYTES;
            int offset = 0;

            key.shaderProgram.bind();

            // Position (x, y, z)
            gl.glEnableVertexAttribArray(0);
            gl.glVertexAttribPointer(0, 3, GL30.GL_FLOAT, false, stride, offset);
            offset += 3 * Float.BYTES;

            // Normal / direction (x, y, z)
            gl.glEnableVertexAttribArray(1);
            gl.glVertexAttribPointer(1, 3, GL30.GL_FLOAT, false, stride, offset);
            offset += 3 * Float.BYTES;

            // Color (packed as single float)
            gl.glEnableVertexAttribArray(2);
            gl.glVertexAttribPointer(2, 1, GL30.GL_FLOAT, false, stride, offset);
            offset += 1 * Float.BYTES;

            // UV (u, v)
            gl.glEnableVertexAttribArray(3);
            gl.glVertexAttribPointer(3, 2, GL30.GL_FLOAT, false, stride, offset);

            gl.glBindVertexArray(0);

            // store gpu batch into list for this keyId
            list.add(gpu);
        }

        uploaded = true;
    }

    public void render() {

        // Lazy initialization on the render thread
        if (!uploaded)
            uploadToGPU();

        GL30 gl = Gdx.gl30;

        for (RenderPacket.RenderKey key : keys) {

            ObjectArrayList<DrawCall> list = drawCalls.get(key.id);

            if (list == null || list.isEmpty())
                continue;

            key.shaderProgram.bind();
            key.textureArray.bind();

            materialManager.setUniform(
                    key.id,
                    "u_transform",
                    megaChunk.renderPosition(),
                    true);

            // draw all GPU batches for this material
            for (DrawCall gpu : list) {

                if (gpu == null)
                    continue;

                gl.glBindVertexArray(gpu.vao);
                gl.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, gpu.ibo);
                gl.glDrawElements(GL30.GL_TRIANGLES, gpu.indexCount, GL30.GL_UNSIGNED_SHORT, 0);
            }

            gl.glBindVertexArray(0);
        }

        gl.glUseProgram(0);
    }

    public void dispose() {

        if (!uploaded)
            return;

        GL30 gl = Gdx.gl30;
        IntBuffer tmp = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();

        // iterate over lists of GPU batches per material
        for (ObjectArrayList<DrawCall> list : drawCalls.values()) {

            if (list == null)
                continue;

            for (DrawCall gpu : list) {

                if (gpu == null)
                    continue;

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
        }

        drawCalls.clear();
        keys.clear();

        uploaded = false;
    }

    private static final class DrawCall {

        int vao, vbo, ibo;
        int indexCount;
    }
}
