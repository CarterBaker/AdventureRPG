package application.bootstrap.geometrypipeline.meshmanager;

import java.nio.FloatBuffer;

import engine.graphics.gl.GL20;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.memory.BufferUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

class RiggedMeshGLSLUtility extends EngineUtility {

    /*
     * GL creation and disposal of a rigged mesh's morph texture for
     * MeshManager. The texture is RGBA32F, RIGGED_MESH_MORPH_TEXTURE_WIDTH
     * texels wide with as many rows as its entries need, one entry per texel:
     * a delta in rgb and the morph it belongs to in a. The skinned vertex
     * shader reads it with texelFetch — data, never filtered. A mesh with no
     * morph entries has no texture.
     */

    // Scratch
    private static FloatBuffer entryScratch;

    // Morph Texture \\

    static int createMorphTexture(FloatArrayList morphEntries) {

        int entryCount = morphEntries.size() / EngineSetting.RIGGED_MESH_MORPH_ENTRY_FLOATS;

        if (entryCount == 0)
            return EngineSetting.GL_HANDLE_NONE;

        GL20 gl20 = EngineContext.gl20;
        int width = EngineSetting.RIGGED_MESH_MORPH_TEXTURE_WIDTH;
        int height = (entryCount + width - 1) / width;
        int floatCount = width * height * EngineSetting.RIGGED_MESH_MORPH_ENTRY_FLOATS;

        entryScratch = BufferUtility.ensureCapacity(entryScratch, floatCount);
        entryScratch.put(morphEntries.elements(), 0, morphEntries.size());

        while (entryScratch.position() < floatCount)
            entryScratch.put(0f);

        entryScratch.flip();

        int texture = gl20.glGenTexture();
        gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, texture);
        gl20.glTexImage2D(
                EngineSetting.GL_TEXTURE_2D, 0, EngineSetting.GL_RGBA32F,
                width, height, 0,
                EngineSetting.GL_RGBA, EngineSetting.GL_FLOAT, entryScratch);
        gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_MIN_FILTER,
                EngineSetting.GL_NEAREST);
        gl20.glTexParameteri(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_TEXTURE_MAG_FILTER,
                EngineSetting.GL_NEAREST);
        gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, EngineSetting.GL_HANDLE_NONE);

        return texture;
    }

    // Disposal \\

    static void deleteTexture(int handle) {
        if (handle != EngineSetting.GL_HANDLE_NONE)
            EngineContext.gl20.glDeleteTexture(handle);
    }
}
