package com.internal.bootstrap.geometrypipeline.ibomanager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.internal.bootstrap.geometrypipeline.ibo.IBOInstance;
import com.internal.bootstrap.geometrypipeline.ibo.IBOStruct;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;

class GLSLUtility {

        // Upload \\

        static IBOHandle uploadIndexData(VAOInstance vaoInstance, IBOHandle iboHandle, short[] indices) {
                IBOStruct iboStruct = upload(vaoInstance, indices);
                iboHandle.constructor(iboStruct);
                return iboHandle;
        }

        static IBOInstance uploadIndexData(VAOInstance vaoInstance, IBOInstance iboInstance, short[] indices) {
                IBOStruct iboStruct = upload(vaoInstance, indices);
                iboInstance.constructor(iboStruct);
                return iboInstance;
        }

        private static IBOStruct upload(VAOInstance vaoInstance, short[] indices) {

                GL30 gl30 = Gdx.gl30;
                GL20 gl20 = Gdx.gl20;

                gl30.glBindVertexArray(vaoInstance.getVAOStruct().attributeHandle);

                int ibo = gl20.glGenBuffer();
                gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, ibo);

                ShortBuffer buffer = ByteBuffer
                                .allocateDirect(indices.length * 2)
                                .order(ByteOrder.nativeOrder())
                                .asShortBuffer();

                buffer.put(indices).flip();
                gl20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, indices.length * 2, buffer, GL20.GL_STATIC_DRAW);

                gl30.glBindVertexArray(0);

                return new IBOStruct(ibo, indices.length);
        }

        // Removal \\

        static void removeIndexData(IBOStruct iboStruct) {
                Gdx.gl20.glDeleteBuffer(iboStruct.indexHandle);
        }
}