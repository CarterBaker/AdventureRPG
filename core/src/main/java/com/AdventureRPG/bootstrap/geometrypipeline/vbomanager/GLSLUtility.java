package com.AdventureRPG.bootstrap.geometrypipeline.vbomanager;

import com.AdventureRPG.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

class GLSLUtility {

        static VBOHandle uploadVertexData(VAOHandle vaoHandle, VBOHandle vboHandle, float[] vertices) {

                GL30 gl30 = Gdx.gl30;
                GL20 gl20 = Gdx.gl20;

                // Bind the VAO FIRST
                gl30.glBindVertexArray(vaoHandle.getAttributeHandle());

                // Create VBO
                int vbo = gl20.glGenBuffer();
                gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);

                // Upload data
                FloatBuffer vertexBuffer = ByteBuffer
                                .allocateDirect(vertices.length * 4)
                                .order(ByteOrder.nativeOrder())
                                .asFloatBuffer();
                vertexBuffer.put(vertices).flip();

                gl20.glBufferData(GL20.GL_ARRAY_BUFFER, vertices.length * 4,
                                vertexBuffer, GL20.GL_STATIC_DRAW);

                // Configure vertex attributes
                int stride = vaoHandle.getVertStride() * 4; // floats to bytes

                gl20.glVertexAttribPointer(0, 3, GL20.GL_FLOAT, false, stride, 0);
                gl20.glEnableVertexAttribArray(0);

                gl20.glVertexAttribPointer(1, 2, GL20.GL_FLOAT, false, stride, 3 * 4);
                gl20.glEnableVertexAttribArray(1);

                gl20.glVertexAttribPointer(2, 4, GL20.GL_FLOAT, false, stride, 5 * 4);
                gl20.glEnableVertexAttribArray(2);

                // Unbind
                gl30.glBindVertexArray(0);
                gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);

                int vertexCount = vertices.length / vaoHandle.getVertStride();
                vboHandle.constructor(vbo, vertexCount);

                return vboHandle;
        }
}
