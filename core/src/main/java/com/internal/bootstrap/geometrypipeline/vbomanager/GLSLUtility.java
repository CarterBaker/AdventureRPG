package com.internal.bootstrap.geometrypipeline.vbomanager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

class GLSLUtility {

        static VBOHandle uploadVertexData(VAOHandle vaoHandle, VBOHandle vboHandle, float[] vertices) {

                GL30 gl30 = Gdx.gl30;
                GL20 gl20 = Gdx.gl20;

                gl30.glBindVertexArray(vaoHandle.getAttributeHandle());

                int vbo = gl20.glGenBuffer();
                gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);

                FloatBuffer vertexBuffer = ByteBuffer
                                .allocateDirect(vertices.length * 4)
                                .order(ByteOrder.nativeOrder())
                                .asFloatBuffer();
                vertexBuffer.put(vertices).flip();
                gl20.glBufferData(GL20.GL_ARRAY_BUFFER, vertices.length * 4,
                                vertexBuffer, GL20.GL_STATIC_DRAW);

                // VBO is now bound - apply attrib pointers dynamically from VAOHandle layout
                int strideBytes = vaoHandle.getVertStride() * 4;
                int[] attrSizes = vaoHandle.getAttrSizes();
                int byteOffset = 0;

                for (int i = 0; i < attrSizes.length; i++) {
                        gl20.glEnableVertexAttribArray(i);
                        gl20.glVertexAttribPointer(i, attrSizes[i], GL20.GL_FLOAT, false, strideBytes, byteOffset);
                        byteOffset += attrSizes[i] * 4;
                }

                gl30.glBindVertexArray(0);
                gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);

                int vertexCount = vertices.length / vaoHandle.getVertStride();
                vboHandle.constructor(vbo, vertexCount);

                return vboHandle;
        }

        static void removeVertexData(VBOHandle vboHandle) {
                Gdx.gl20.glDeleteBuffer(vboHandle.getVertexHandle());
        }
}