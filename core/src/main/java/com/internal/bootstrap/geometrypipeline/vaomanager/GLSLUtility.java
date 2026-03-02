package com.internal.bootstrap.geometrypipeline.vaomanager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;
import com.internal.bootstrap.geometrypipeline.vao.VAOStruct;

class GLSLUtility {

        // Instance Creation \\

        static VAOInstance createVAOInstance(VAOInstance vaoInstance, VAOHandle template) {
                VAOStruct vaoStruct = createStruct(template.getVAOStruct().attrSizes);
                vaoInstance.constructor(vaoStruct);
                return vaoInstance;
        }

        private static VAOStruct createStruct(int[] attrSizes) {

                GL30 gl = Gdx.gl30;
                IntBuffer id = ByteBuffer
                                .allocateDirect(4)
                                .order(ByteOrder.nativeOrder())
                                .asIntBuffer();

                gl.glGenVertexArrays(1, id);
                int vao = id.get(0);

                gl.glBindVertexArray(vao);
                for (int i = 0; i < attrSizes.length; i++)
                        gl.glEnableVertexAttribArray(i);
                gl.glBindVertexArray(0);

                return new VAOStruct(vao, attrSizes);
        }

        // Removal \\

        static void removeVAOStruct(VAOStruct vaoStruct) {
                if (vaoStruct.attributeHandle == 0)
                        return;
                IntBuffer buffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
                buffer.put(vaoStruct.attributeHandle).flip();
                Gdx.gl30.glDeleteVertexArrays(1, buffer);
        }

        static void removeVAOInstance(VAOInstance vaoInstance) {

                int vao = vaoInstance.getVAOStruct().attributeHandle;

                if (vao == 0)
                        return;

                IntBuffer buffer = ByteBuffer
                                .allocateDirect(4)
                                .order(ByteOrder.nativeOrder())
                                .asIntBuffer();

                buffer.put(vao).flip();
                Gdx.gl30.glDeleteVertexArrays(1, buffer);
        }
}