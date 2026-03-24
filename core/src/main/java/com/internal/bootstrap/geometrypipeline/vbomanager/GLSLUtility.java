package com.internal.bootstrap.geometrypipeline.vbomanager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.internal.bootstrap.geometrypipeline.vao.VAOData;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;
import com.internal.bootstrap.geometrypipeline.vbo.VBOData;
import com.internal.bootstrap.geometrypipeline.vbo.VBOHandle;
import com.internal.bootstrap.geometrypipeline.vbo.VBOInstance;

class GLSLUtility {

        /*
         * GL upload and deletion operations for VBOManager.
         * Package-private — only VBOManager may call these.
         */

        // Upload \\

        static VBOHandle uploadVertexData(
                        VAOData vaoData,
                        VBOHandle vboHandle,
                        float[] vertices) {

                VBOData vboData = upload(vaoData, vertices);
                vboHandle.constructor(vboData);

                return vboHandle;
        }

        static VBOInstance uploadVertexData(
                        VAOData vaoData,
                        VBOInstance vboInstance,
                        float[] vertices) {

                VBOData vboData = upload(vaoData, vertices);
                vboInstance.constructor(vboData);

                return vboInstance;
        }

        private static VBOData upload(VAOData vaoData, float[] vertices) {

                GL30 gl30 = Gdx.gl30;
                GL20 gl20 = Gdx.gl20;
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
                Gdx.gl20.glDeleteBuffer(vboData.getVertexHandle());
        }
}