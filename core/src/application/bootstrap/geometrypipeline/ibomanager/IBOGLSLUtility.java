package application.bootstrap.geometrypipeline.ibomanager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import application.bootstrap.geometrypipeline.ibo.IBOData;
import application.bootstrap.geometrypipeline.ibo.IBOHandle;
import application.bootstrap.geometrypipeline.ibo.IBOInstance;
import application.bootstrap.geometrypipeline.vao.VAOInstance;
import engine.graphics.gl.GL20;
import engine.graphics.gl.GL30;
import engine.root.EngineContext;
import engine.root.EngineSetting;

class IBOGLSLUtility {

        /*
         * GL upload, in-place update, and deletion operations for IBOManager.
         * updateIndexData respecifies an EXISTING index buffer's data store via
         * the same GL handle rather than allocating a new one, so any VAO that
         * already references this handle stays valid with no extra work.
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

                GL30 gl30 = EngineContext.gl30;
                GL20 gl20 = EngineContext.gl20;
                int size = indices.length * Short.BYTES;

                gl30.glBindVertexArray(vaoInstance.getVAOData().getAttributeHandle());

                int ibo = gl20.glGenBuffer();
                gl20.glBindBuffer(EngineSetting.GL_ELEMENT_ARRAY_BUFFER, ibo);

                ShortBuffer buffer = ByteBuffer
                                .allocateDirect(size)
                                .order(ByteOrder.nativeOrder())
                                .asShortBuffer();
                buffer.put(indices).flip();

                gl20.glBufferData(EngineSetting.GL_ELEMENT_ARRAY_BUFFER, size, buffer, EngineSetting.GL_STATIC_DRAW);
                gl30.glBindVertexArray(0);

                return new IBOData(ibo, indices.length);
        }

        // Update \\

        static IBOInstance updateIndexData(IBOInstance iboInstance, short[] indices) {

                IBOData oldData = iboInstance.getIBOData();
                IBOData newData = reupload(oldData.getIndexHandle(), indices);
                iboInstance.constructor(newData);

                return iboInstance;
        }

        private static IBOData reupload(int ibo, short[] indices) {

                GL20 gl20 = EngineContext.gl20;
                int size = indices.length * Short.BYTES;

                gl20.glBindBuffer(EngineSetting.GL_ELEMENT_ARRAY_BUFFER, ibo);

                ShortBuffer buffer = ByteBuffer
                                .allocateDirect(size)
                                .order(ByteOrder.nativeOrder())
                                .asShortBuffer();
                buffer.put(indices).flip();

                gl20.glBufferData(EngineSetting.GL_ELEMENT_ARRAY_BUFFER, size, buffer, EngineSetting.GL_DYNAMIC_DRAW);
                gl20.glBindBuffer(EngineSetting.GL_ELEMENT_ARRAY_BUFFER, 0);

                return new IBOData(ibo, indices.length);
        }

        // Removal \\

        static void removeIndexData(IBOData iboData) {
                EngineContext.gl20.glDeleteBuffer(iboData.getIndexHandle());
        }
}