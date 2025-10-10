package com.AdventureRPG.WorldSystem.RenderManager;

import com.AdventureRPG.MaterialManager.MaterialData;
import com.AdventureRPG.Util.GlobalConstant;
import com.badlogic.gdx.graphics.TextureArray;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

// TODO: AI Created, needs scrutiny
public final class RenderPacket {

    public final Int2ObjectOpenHashMap<RenderKey> keys = new Int2ObjectOpenHashMap<>();
    public final ObjectArrayList<RenderBatch> batches = new ObjectArrayList<>();

    // Base \\

    public RenderKey getOrCreateKey(MaterialData mat) {

        RenderKey key = keys.get(mat.id);

        if (key == null) {

            key = new RenderKey(
                    mat.id,
                    mat.textureArray,
                    mat.shaderProgram);

            keys.put(mat.id, key);
        }

        return key;
    }

    public void addBatch(RenderBatch batch) {

        batches.add(batch);
    }

    public void clear() {

        keys.clear();
        batches.clear();
    }

    // Utility \\

    public static final class RenderKey {

        public final int id;
        public final TextureArray textureArray;
        public final ShaderProgram shaderProgram;

        public RenderKey(
                int id,
                TextureArray textureArray,
                ShaderProgram shaderProgram) {

            this.id = id;
            this.textureArray = textureArray;
            this.shaderProgram = shaderProgram;
        }
    }

    public static final class RenderBatch {

        public final int keyId;

        public final float[] vertices;
        public final short[] indices;

        public final int vertexCount;
        public final int indexCount;

        public RenderBatch(
                int keyId,
                float[] vertices,
                short[] indices) {

            this.keyId = keyId;
            this.vertices = vertices;
            this.indices = indices;
            this.vertexCount = vertices.length / GlobalConstant.VERT_STRIDE;
            this.indexCount = indices.length;
        }
    }
}
