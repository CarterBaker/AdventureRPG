package com.internal.bootstrap.geometrypipeline.vbomanager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;
import com.internal.bootstrap.geometrypipeline.vao.VAOStruct;
import com.internal.bootstrap.geometrypipeline.vbo.VBOHandle;
import com.internal.bootstrap.geometrypipeline.vbo.VBOInstance;
import com.internal.bootstrap.geometrypipeline.vbo.VBOStruct;

class GLSLUtility {

        // Upload \\

        static VBOHandle uploadVertexData(VAOInstance vaoInstance, VBOHandle vboHandle, float[] vertices) {
                VBOStruct vboStruct = upload(vaoInstance, vertices);
                vboHandle.constructor(vboStruct);
                return vboHandle;
        }

        static VBOInstance uploadVertexData(VAOInstance vaoInstance, VBOInstance vboInstance, float[] vertices) {
                VBOStruct vboStruct = upload(vaoInstance, vertices);
                vboInstance.constructor(vboStruct);
                return vboInstance;
        }

        private static VBOStruct upload(VAOInstance vaoInstance, float[] vertices) {

                GL30 gl30 = Gdx.gl30;
                GL20 gl20 = Gdx.gl20;

                VAOStruct vaoStruct = vaoInstance.getVAOStruct();

                gl30.glBindVertexArray(vaoStruct.attributeHandle);

                int vbo = gl20.glGenBuffer();
                gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);

                FloatBuffer buffer = ByteBuffer
                                .allocateDirect(vertices.length * 4)
                                .order(ByteOrder.nativeOrder())
                                .asFloatBuffer();

                buffer.put(vertices).flip();
                gl20.glBufferData(GL20.GL_ARRAY_BUFFER, vertices.length * 4, buffer, GL20.GL_STATIC_DRAW);

                int strideBytes = vaoStruct.vertStride * 4;
                int[] attrSizes = vaoStruct.attrSizes;
                int byteOffset = 0;

                for (int i = 0; i < attrSizes.length; i++) {
                        gl20.glEnableVertexAttribArray(i);
                        gl20.glVertexAttribPointer(i, attrSizes[i], GL20.GL_FLOAT, false, strideBytes, byteOffset);
                        byteOffset += attrSizes[i] * 4;
                }

                gl30.glBindVertexArray(0);
                gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);

                return new VBOStruct(vbo, vertices.length / vaoStruct.vertStride);
        }

        // Removal \\

        static void removeVertexData(VBOStruct vboStruct) {
                Gdx.gl20.glDeleteBuffer(vboStruct.vertexHandle);
        }
}