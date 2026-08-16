package application.bootstrap.geometrypipeline.vbomanager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import application.bootstrap.geometrypipeline.vao.VAOData;
import application.bootstrap.geometrypipeline.vao.VAOInstance;
import application.bootstrap.geometrypipeline.vbo.VBOData;
import application.bootstrap.geometrypipeline.vbo.VBOHandle;
import application.bootstrap.geometrypipeline.vbo.VBOInstance;
import engine.graphics.gl.GL20;
import engine.graphics.gl.GL30;
import engine.root.EngineContext;
import engine.root.EngineSetting;

class VBOGLSLUtility {

        /*
         * GL upload, in-place update, and deletion operations for VBOManager.
         * updateVertexData respecifies an EXISTING vertex buffer's data store via
         * the same GL handle rather than allocating a new one, so any VAO that
         * already references this handle — including per-window clones held by
         * VAOManager — stays valid with no extra work. Every upload and reupload
         * funnels its data through one reusable, growable direct buffer instead
         * of allocating a fresh native ByteBuffer per call — direct buffers are
         * off-heap and only freed once the GC gets around to running their
         * Cleaner, and a chunk-streaming workload allocates them far faster than
         * that can keep up, which is what was stalling the engine a few seconds
         * into any real session. Both upload() and reupload() pass the raw
         * vertices array and stride into VBOData so its bounds stay accurate
         * against whatever the buffer currently holds, even after an in-place
         * respecify. All GL work here runs on whichever thread currently holds
         * the GL context, never concurrently, so a static scratch buffer is
         * safe. Package-private — only VBOManager may call these.
         */

        // Scratch — reused across every upload/reupload call, grown by doubling
        private static FloatBuffer floatScratch;
        private static int floatScratchCapacity;

        private static FloatBuffer acquireFloatScratch(int floatCount) {

                if (floatScratch == null || floatScratchCapacity < floatCount) {
                        floatScratchCapacity = Math.max(floatCount, floatScratchCapacity * 2);
                        floatScratch = ByteBuffer
                                        .allocateDirect(floatScratchCapacity * Float.BYTES)
                                        .order(ByteOrder.nativeOrder())
                                        .asFloatBuffer();
                }

                floatScratch.clear();
                return floatScratch;
        }

        // Upload \\

        static VBOHandle uploadVertexData(
                        VAOInstance vaoInstance,
                        VBOHandle vboHandle,
                        float[] vertices) {

                VBOData vboData = upload(vaoInstance, vertices);
                vboHandle.constructor(vboData);

                return vboHandle;
        }

        static VBOInstance uploadVertexData(
                        VAOInstance vaoInstance,
                        VBOInstance vboInstance,
                        float[] vertices) {

                VBOData vboData = upload(vaoInstance, vertices);
                vboInstance.constructor(vboData);

                return vboInstance;
        }

        private static VBOData upload(VAOInstance vaoInstance, float[] vertices) {

                GL30 gl30 = EngineContext.gl30;
                GL20 gl20 = EngineContext.gl20;
                VAOData vaoData = vaoInstance.getVAOData();
                int size = vertices.length * Float.BYTES;

                gl30.glBindVertexArray(vaoData.getAttributeHandle());

                int vbo = gl20.glGenBuffer();
                gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, vbo);

                FloatBuffer buffer = acquireFloatScratch(vertices.length);
                buffer.put(vertices).flip();

                gl20.glBufferData(EngineSetting.GL_ARRAY_BUFFER, size, buffer, EngineSetting.GL_STATIC_DRAW);

                int strideBytes = vaoData.getVertStride() * Float.BYTES;
                int[] attrSizes = vaoData.getAttrSizes();
                int byteOffset = 0;

                for (int i = 0; i < attrSizes.length; i++) {
                        gl20.glEnableVertexAttribArray(i);
                        gl20.glVertexAttribPointer(i, attrSizes[i], EngineSetting.GL_FLOAT, false, strideBytes,
                                        byteOffset);
                        byteOffset += attrSizes[i] * Float.BYTES;
                }

                gl30.glBindVertexArray(0);
                gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, 0);

                return new VBOData(vbo, vertices, vaoData.getVertStride());
        }

        // Update \\

        static VBOInstance updateVertexData(VAOInstance vaoInstance, VBOInstance vboInstance, float[] vertices) {

                VBOData oldData = vboInstance.getVBOData();
                int vertStride = vaoInstance.getVAOData().getVertStride();
                VBOData newData = reupload(oldData.getVertexHandle(), vertices, vertStride);
                vboInstance.constructor(newData);

                return vboInstance;
        }

        private static VBOData reupload(int vbo, float[] vertices, int vertStride) {

                GL20 gl20 = EngineContext.gl20;
                int size = vertices.length * Float.BYTES;

                gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, vbo);

                FloatBuffer buffer = acquireFloatScratch(vertices.length);
                buffer.put(vertices).flip();

                gl20.glBufferData(EngineSetting.GL_ARRAY_BUFFER, size, buffer, EngineSetting.GL_DYNAMIC_DRAW);
                gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, 0);

                return new VBOData(vbo, vertices, vertStride);
        }

        // Removal \\

        static void removeVertexData(VBOData vboData) {
                EngineContext.gl20.glDeleteBuffer(vboData.getVertexHandle());
        }
}