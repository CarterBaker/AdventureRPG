package com.internal.bootstrap.geometrypipeline.ibomanager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.core.engine.UtilityPackage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

class GLSLUtility extends UtilityPackage {

        // Uploads index data to a GPU buffer and binds it to the given VAO.
        static IBOHandle uploadIndexData(VAOHandle vaoHandle, IBOHandle iboHandle, short[] indices) {

                GL30 gl30 = Gdx.gl30;
                GL20 gl20 = Gdx.gl20;

                // Bind the VAO FIRST
                gl30.glBindVertexArray(vaoHandle.getAttributeHandle());

                // Create IBO
                int ibo = gl20.glGenBuffer();
                gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, ibo);

                // Upload data
                ShortBuffer indexBuffer = ByteBuffer
                                .allocateDirect(indices.length * 2)
                                .order(ByteOrder.nativeOrder())
                                .asShortBuffer();
                indexBuffer.put(indices).flip();

                gl20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, indices.length * 2,
                                indexBuffer, GL20.GL_STATIC_DRAW);

                // The IBO binding is stored in the VAO state
                gl30.glBindVertexArray(0);

                int indexCount = indices.length;
                iboHandle.constructor(ibo, indexCount);

                return iboHandle;
        }

        static void removeIndexData(IBOHandle iboHandle) {
                GL20 gl20 = Gdx.gl20;
                gl20.glDeleteBuffer(iboHandle.getIndexHandle());
        }
}
