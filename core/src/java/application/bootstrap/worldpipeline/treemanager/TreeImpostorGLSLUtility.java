package application.bootstrap.worldpipeline.treemanager;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;

import application.bootstrap.shaderpipeline.texturemanager.TextureCompressionUtility;
import engine.graphics.gl.GL40;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

final class TreeImpostorGLSLUtility extends EngineUtility {

    /*
     * The GPU side of every species' impostor: one BC7 array texture per
     * map, a layer to each species by its tree ID, its whole chain from the
     * atlas down to one texel, filtered trilinearly and clamped at its edge so
     * no frame bleeds into the atlas's far side. A layer is written from a
     * direct buffer holding its chain back to back, read straight from client
     * memory.
     */

    // Arrays \\

    static int createArray(int layers) {

        GL40 gl = EngineContext.gl40;
        int handle = EngineContext.gl20.glGenTexture();
        int size = EngineSetting.TREE_IMPOSTOR_ATLAS_TEXELS;
        int levels = Integer.numberOfTrailingZeros(size) + 1;

        if (handle == EngineSetting.GL_HANDLE_NONE)
            throwException("GPU handle could not be generated for the tree impostor array");

        gl.glBindTexture(EngineSetting.GL_TEXTURE_2D_ARRAY, handle);

        for (int level = 0, edge = size; level < levels; level++, edge >>= 1)
            gl.glCompressedTexImage3D(EngineSetting.GL_TEXTURE_2D_ARRAY, level,
                    EngineSetting.GL_COMPRESSED_RGBA_BPTC_UNORM, edge, edge, layers, 0,
                    TextureCompressionUtility.getLevelBytes(edge, edge) * layers, 0L);

        gl.glTexParameteri(EngineSetting.GL_TEXTURE_2D_ARRAY, EngineSetting.GL_TEXTURE_BASE_LEVEL, 0);
        gl.glTexParameteri(EngineSetting.GL_TEXTURE_2D_ARRAY, EngineSetting.GL_TEXTURE_MAX_LEVEL, levels - 1);
        gl.glTexParameteri(EngineSetting.GL_TEXTURE_2D_ARRAY, EngineSetting.GL_TEXTURE_MIN_FILTER,
                EngineSetting.GL_LINEAR_MIPMAP_LINEAR);
        gl.glTexParameteri(EngineSetting.GL_TEXTURE_2D_ARRAY, EngineSetting.GL_TEXTURE_MAG_FILTER,
                EngineSetting.GL_LINEAR);
        gl.glTexParameteri(EngineSetting.GL_TEXTURE_2D_ARRAY, EngineSetting.GL_TEXTURE_WRAP_S,
                EngineSetting.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(EngineSetting.GL_TEXTURE_2D_ARRAY, EngineSetting.GL_TEXTURE_WRAP_T,
                EngineSetting.GL_CLAMP_TO_EDGE);
        gl.glBindTexture(EngineSetting.GL_TEXTURE_2D_ARRAY, EngineSetting.GL_HANDLE_NONE);

        return handle;
    }

    // One species' chain written into its layer from a direct buffer holding it from position zero
    static void uploadLayer(int handle, int layer, ByteBuffer chain) {

        GL40 gl = EngineContext.gl40;
        long address = MemoryUtil.memAddress0(chain);

        gl.glBindTexture(EngineSetting.GL_TEXTURE_2D_ARRAY, handle);

        for (int level = 0, edge = EngineSetting.TREE_IMPOSTOR_ATLAS_TEXELS; edge >= 1; level++, edge >>= 1) {

            int bytes = TextureCompressionUtility.getLevelBytes(edge, edge);

            gl.glCompressedTexSubImage3D(EngineSetting.GL_TEXTURE_2D_ARRAY, level, 0, 0, layer, edge, edge, 1,
                    EngineSetting.GL_COMPRESSED_RGBA_BPTC_UNORM, bytes, address);
            address += bytes;
        }

        gl.glBindTexture(EngineSetting.GL_TEXTURE_2D_ARRAY, EngineSetting.GL_HANDLE_NONE);
    }

    static void deleteArray(int handle) {
        EngineContext.gl20.glDeleteTexture(handle);
    }
}
