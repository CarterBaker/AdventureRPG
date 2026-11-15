package application.bootstrap.geometrypipeline.ibomanager;

import java.nio.Buffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import application.bootstrap.geometrypipeline.ibo.IBOData;
import application.bootstrap.geometrypipeline.ibo.IBOHandle;
import application.bootstrap.geometrypipeline.ibo.IBOInstance;
import application.bootstrap.geometrypipeline.vao.VAOInstance;
import engine.graphics.gl.GL20;
import engine.graphics.gl.GL30;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.memory.BufferUtility;

class IBOGLSLUtility extends EngineUtility {

    /*
     * GL upload, in-place update and deletion for IBOManager. Updates respecify
     * the same buffer handle so VAOs referencing it stay valid, and every
     * upload goes through one reusable direct scratch buffer, read straight
     * from the caller's backing array up to its live count so no upload ever
     * copies onto the heap. Indices are unsigned shorts, or unsigned ints for
     * a mesh with more vertices than a short reaches. Package-private.
     */

    // Scratch
    private static ShortBuffer shortScratch;
    private static IntBuffer intScratch;

    private static ShortBuffer fillScratch(short[] indices, int indexCount) {

        shortScratch = BufferUtility.ensureCapacity(shortScratch, indexCount);
        shortScratch.put(indices, 0, indexCount).flip();

        return shortScratch;
    }

    private static IntBuffer fillScratch(int[] indices, int indexCount) {

        intScratch = BufferUtility.ensureCapacity(intScratch, indexCount);
        intScratch.put(indices, 0, indexCount).flip();

        return intScratch;
    }

    // Upload \\

    static IBOHandle uploadIndexData(
            VAOInstance vaoInstance,
            IBOHandle iboHandle,
            short[] indices) {

        IBOData iboData = upload(
                vaoInstance, fillScratch(indices, indices.length), indices.length, EngineSetting.GL_UNSIGNED_SHORT);
        iboHandle.constructor(iboData);

        return iboHandle;
    }

    static IBOHandle uploadIndexData(
            VAOInstance vaoInstance,
            IBOHandle iboHandle,
            int[] indices) {

        IBOData iboData = upload(
                vaoInstance, fillScratch(indices, indices.length), indices.length, EngineSetting.GL_UNSIGNED_INT);
        iboHandle.constructor(iboData);

        return iboHandle;
    }

    static IBOInstance uploadIndexData(
            VAOInstance vaoInstance,
            IBOInstance iboInstance,
            short[] indices,
            int indexCount) {

        IBOData iboData = upload(
                vaoInstance, fillScratch(indices, indexCount), indexCount, EngineSetting.GL_UNSIGNED_SHORT);
        iboInstance.constructor(iboData);

        return iboInstance;
    }

    private static IBOData upload(VAOInstance vaoInstance, Buffer buffer, int indexCount, int indexType) {

        GL30 gl30 = EngineContext.gl30;
        GL20 gl20 = EngineContext.gl20;
        int size = indexCount * resolveIndexBytes(indexType);

        gl30.glBindVertexArray(vaoInstance.getVAOData().getAttributeHandle());

        int ibo = gl20.glGenBuffer();
        gl20.glBindBuffer(EngineSetting.GL_ELEMENT_ARRAY_BUFFER, ibo);

        gl20.glBufferData(EngineSetting.GL_ELEMENT_ARRAY_BUFFER, size, buffer, EngineSetting.GL_STATIC_DRAW);
        gl30.glBindVertexArray(0);

        return new IBOData(ibo, indexCount, indexType);
    }

    private static int resolveIndexBytes(int indexType) {
        return indexType == EngineSetting.GL_UNSIGNED_INT ? Integer.BYTES : Short.BYTES;
    }

    // Update \\

    static IBOInstance updateIndexData(IBOInstance iboInstance, short[] indices, int indexCount) {

        IBOData oldData = iboInstance.getIBOData();
        IBOData newData = reupload(
                oldData.getIndexHandle(), fillScratch(indices, indexCount), indexCount,
                EngineSetting.GL_UNSIGNED_SHORT);
        iboInstance.constructor(newData);

        return iboInstance;
    }

    static IBOHandle updateIndexData(IBOHandle iboHandle, short[] indices, int indexCount) {

        IBOData oldData = iboHandle.getIBOData();
        IBOData newData = reupload(
                oldData.getIndexHandle(), fillScratch(indices, indexCount), indexCount,
                EngineSetting.GL_UNSIGNED_SHORT);
        iboHandle.constructor(newData);

        return iboHandle;
    }

    static IBOHandle updateIndexData(IBOHandle iboHandle, int[] indices, int indexCount) {

        IBOData oldData = iboHandle.getIBOData();
        IBOData newData = reupload(
                oldData.getIndexHandle(), fillScratch(indices, indexCount), indexCount,
                EngineSetting.GL_UNSIGNED_INT);
        iboHandle.constructor(newData);

        return iboHandle;
    }

    private static IBOData reupload(int ibo, Buffer buffer, int indexCount, int indexType) {

        GL20 gl20 = EngineContext.gl20;
        int size = indexCount * resolveIndexBytes(indexType);

        gl20.glBindBuffer(EngineSetting.GL_ELEMENT_ARRAY_BUFFER, ibo);
        gl20.glBufferData(EngineSetting.GL_ELEMENT_ARRAY_BUFFER, size, buffer, EngineSetting.GL_DYNAMIC_DRAW);
        gl20.glBindBuffer(EngineSetting.GL_ELEMENT_ARRAY_BUFFER, 0);

        return new IBOData(ibo, indexCount, indexType);
    }

    // Removal \\

    static void removeIndexData(IBOData iboData) {
        EngineContext.gl20.glDeleteBuffer(iboData.getIndexHandle());
    }
}