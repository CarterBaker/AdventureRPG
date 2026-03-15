package com.internal.bootstrap.geometrypipeline.vaomanager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
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
}