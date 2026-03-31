package com.internal.bootstrap.geometrypipeline.vaomanager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import com.internal.platform.Gdx;
import com.internal.platform.graphics.GL20;
import com.internal.platform.graphics.GL30;
import com.internal.bootstrap.geometrypipeline.vao.VAOData;
import com.internal.bootstrap.geometrypipeline.vao.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;

class GLSLUtility {

        /*
         * GL creation and deletion operations for VAOManager.
         * Package-private — only VAOManager may call these.
         */

        // Instance Creation \\

        static VAOInstance createVAOInstance(VAOInstance vaoInstance, VAOHandle template) {

                VAOData vaoData = createData(template.getVAOData().getAttrSizes());
                vaoInstance.constructor(vaoData);

                return vaoInstance;
        }

        static int cloneVAO(int[] attrSizes, int vertexHandle, int indexHandle) {

                GL30 gl30 = Gdx.gl30;
                GL20 gl20 = Gdx.gl20;

                IntBuffer id = ByteBuffer
                                .allocateDirect(Integer.BYTES)
                                .order(ByteOrder.nativeOrder())
                                .asIntBuffer();

                gl30.glGenVertexArrays(1, id);
                int vao = id.get(0);

                gl30.glBindVertexArray(vao);
                gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vertexHandle);

                int strideBytes = 0;
                for (int size : attrSizes)
                        strideBytes += size * Float.BYTES;

                int byteOffset = 0;
                for (int i = 0; i < attrSizes.length; i++) {
                        gl20.glEnableVertexAttribArray(i);
                        gl20.glVertexAttribPointer(i, attrSizes[i], GL20.GL_FLOAT, false, strideBytes, byteOffset);
                        byteOffset += attrSizes[i] * Float.BYTES;
                }

                gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, indexHandle);
                gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
                gl30.glBindVertexArray(0);

                return vao;
        }

        private static VAOData createData(int[] attrSizes) {

                GL30 gl = Gdx.gl30;

                IntBuffer id = ByteBuffer
                                .allocateDirect(Integer.BYTES)
                                .order(ByteOrder.nativeOrder())
                                .asIntBuffer();

                gl.glGenVertexArrays(1, id);
                int vao = id.get(0);

                gl.glBindVertexArray(vao);

                for (int i = 0; i < attrSizes.length; i++)
                        gl.glEnableVertexAttribArray(i);

                gl.glBindVertexArray(0);

                return new VAOData(vao, attrSizes);
        }

        // Removal \\

        static void removeVAOData(VAOData vaoData) {

                if (vaoData.getAttributeHandle() == 0)
                        return;

                IntBuffer buffer = ByteBuffer
                                .allocateDirect(Integer.BYTES)
                                .order(ByteOrder.nativeOrder())
                                .asIntBuffer();
                buffer.put(vaoData.getAttributeHandle()).flip();

                Gdx.gl30.glDeleteVertexArrays(1, buffer);
        }

        static void removeVAOInstance(VAOInstance vaoInstance) {

                int vao = vaoInstance.getVAOData().getAttributeHandle();

                if (vao == 0)
                        return;

                IntBuffer buffer = ByteBuffer
                                .allocateDirect(Integer.BYTES)
                                .order(ByteOrder.nativeOrder())
                                .asIntBuffer();
                buffer.put(vao).flip();

                Gdx.gl30.glDeleteVertexArrays(1, buffer);
        }

        static void removeVAOHandle(int vao) {

                if (vao == 0)
                        return;

                IntBuffer buffer = ByteBuffer
                                .allocateDirect(Integer.BYTES)
                                .order(ByteOrder.nativeOrder())
                                .asIntBuffer();
                buffer.put(vao).flip();

                Gdx.gl30.glDeleteVertexArrays(1, buffer);
        }
}