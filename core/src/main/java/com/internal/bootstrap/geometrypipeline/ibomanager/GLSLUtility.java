package com.internal.bootstrap.geometrypipeline.ibomanager;

import com.internal.core.app.CoreContext;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import com.internal.core.util.graphics.gl.GL20;
import com.internal.core.util.graphics.gl.GL30;
import com.internal.bootstrap.geometrypipeline.ibo.IBOData;
import com.internal.bootstrap.geometrypipeline.ibo.IBOHandle;
import com.internal.bootstrap.geometrypipeline.ibo.IBOInstance;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;

class GLSLUtility {

        /*
         * GL upload and deletion operations for IBOManager.
         * Package-private — only IBOManager may call these.
         */

        // Upload \\

        static IBOHandle uploadIndexData(
                        VAOInstance vaoInstance,
                        IBOHandle iboHandle,
                        short[] indices) {

                IBOData iboData = upload(vaoInstance, indices);
                iboHandle.constructor(iboData);

                return iboHandle;
        }

        static IBOInstance uploadIndexData(
                        VAOInstance vaoInstance,
                        IBOInstance iboInstance,
                        short[] indices) {

                IBOData iboData = upload(vaoInstance, indices);
                iboInstance.constructor(iboData);

                return iboInstance;
        }

        private static IBOData upload(VAOInstance vaoInstance, short[] indices) {

                GL30 gl30 = CoreContext.gl30;
                GL20 gl20 = CoreContext.gl20;
                int size = indices.length * Short.BYTES;

                gl30.glBindVertexArray(vaoInstance.getVAOData().getAttributeHandle());

                int ibo = gl20.glGenBuffer();
                gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, ibo);

                ShortBuffer buffer = ByteBuffer
                                .allocateDirect(size)
                                .order(ByteOrder.nativeOrder())
                                .asShortBuffer();
                buffer.put(indices).flip();

                gl20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, size, buffer, GL20.GL_STATIC_DRAW);
                gl30.glBindVertexArray(0);

                return new IBOData(ibo, indices.length);
        }

        // Removal \\

        static void removeIndexData(IBOData iboData) {
                CoreContext.gl20.glDeleteBuffer(iboData.getIndexHandle());
        }
}
