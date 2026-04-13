package application.bootstrap.geometrypipeline.vbomanager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import application.bootstrap.geometrypipeline.vao.VAOData;
import application.bootstrap.geometrypipeline.vao.VAOInstance;
import application.bootstrap.geometrypipeline.vbo.VBOData;
import application.bootstrap.geometrypipeline.vbo.VBOHandle;
import application.bootstrap.geometrypipeline.vbo.VBOInstance;
import application.core.engine.EngineContext;
import application.core.util.graphics.gl.GL20;
import application.core.util.graphics.gl.GL30;

class GLSLUtility {

        /*
         * GL upload and deletion operations for VBOManager.
         * Package-private — only VBOManager may call these.
         */

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
                gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);

                FloatBuffer buffer = ByteBuffer
                                .allocateDirect(size)
                                .order(ByteOrder.nativeOrder())
                                .asFloatBuffer();
                buffer.put(vertices).flip();

                gl20.glBufferData(GL20.GL_ARRAY_BUFFER, size, buffer, GL20.GL_STATIC_DRAW);

                int strideBytes = vaoData.getVertStride() * Float.BYTES;
                int[] attrSizes = vaoData.getAttrSizes();
                int byteOffset = 0;

                for (int i = 0; i < attrSizes.length; i++) {
                        gl20.glEnableVertexAttribArray(i);
                        gl20.glVertexAttribPointer(i, attrSizes[i], GL20.GL_FLOAT, false, strideBytes, byteOffset);
                        byteOffset += attrSizes[i] * Float.BYTES;
                }

                gl30.glBindVertexArray(0);
                gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);

                return new VBOData(vbo, vertices.length / vaoData.getVertStride());
        }

        // Removal \\

        static void removeVertexData(VBOData vboData) {
                EngineContext.gl20.glDeleteBuffer(vboData.getVertexHandle());
        }
}
