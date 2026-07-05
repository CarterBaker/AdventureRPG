package application.bootstrap.geometrypipeline.skinnedbuffermanager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import engine.graphics.gl.GL20;
import engine.root.EngineContext;
import engine.root.EngineSetting;

class SkinnedBufferGLSLUtility {

        /*
         * GL creation, upload, and disposal operations for SkinnedBufferManager.
         * Package-private — only SkinnedBufferManager may call these. The bone
         * palette is an RGBA32F 2D texture — width is boneCapacity *
         * SKINNED_BONE_TEXELS_PER_BONE texels (3 texels encode one mat4x3
         * skinning matrix, dropping the implicit [0,0,0,1] last row), height is
         * one row per instance. Sampled with texelFetch in the vertex shader —
         * never filtered, since this is data, not an image.
         */

        // Instance VBO \\

        static int createDynamicInstanceVBO(int maxInstances) {

                GL20 gl20 = EngineContext.gl20;
                int size = maxInstances * EngineSetting.SKINNED_INSTANCE_MODEL_FLOATS * Float.BYTES;

                int vbo = gl20.glGenBuffer();
                gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, vbo);
                gl20.glBufferData(EngineSetting.GL_ARRAY_BUFFER, size, null, EngineSetting.GL_DYNAMIC_DRAW);
                gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, 0);

                return vbo;
        }

        static void uploadInstanceVBO(int vbo, float[] instanceModelData, int floatCount) {

                GL20 gl20 = EngineContext.gl20;

                FloatBuffer buffer = ByteBuffer
                                .allocateDirect(floatCount * Float.BYTES)
                                .order(ByteOrder.nativeOrder())
                                .asFloatBuffer();
                buffer.put(instanceModelData, 0, floatCount).flip();

                gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, vbo);
                gl20.glBufferSubData(EngineSetting.GL_ARRAY_BUFFER, 0, floatCount * Float.BYTES, buffer);
                gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, 0);
        }

        // Bone Palette Texture \\

        static int createBonePaletteTexture(int boneCapacity, int maxInstances) {

                GL20 gl20 = EngineContext.gl20;
                int width = boneCapacity * EngineSetting.SKINNED_BONE_TEXELS_PER_BONE;

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

        static void uploadBonePalette(int texture, float[] boneMatrixData, int boneCapacity, int instanceCount) {

                GL20 gl20 = EngineContext.gl20;
                int width = boneCapacity * EngineSetting.SKINNED_BONE_TEXELS_PER_BONE;
                int floatCount = width * instanceCount * 4;

                FloatBuffer buffer = ByteBuffer
                                .allocateDirect(floatCount * Float.BYTES)
                                .order(ByteOrder.nativeOrder())
                                .asFloatBuffer();
                buffer.put(boneMatrixData, 0, floatCount).flip();

                gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, texture);
                gl20.glTexSubImage2D(
                                EngineSetting.GL_TEXTURE_2D, 0,
                                0, 0, width, instanceCount,
                                EngineSetting.GL_RGBA, EngineSetting.GL_FLOAT, buffer);
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