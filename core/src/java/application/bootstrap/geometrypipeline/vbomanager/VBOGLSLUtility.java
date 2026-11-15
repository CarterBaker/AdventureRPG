package application.bootstrap.geometrypipeline.vbomanager;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import application.bootstrap.geometrypipeline.vao.VAOData;
import application.bootstrap.geometrypipeline.vao.VAOInstance;
import application.bootstrap.geometrypipeline.vaomanager.VAOAttributeUtility;
import application.bootstrap.geometrypipeline.vbo.VBOData;
import application.bootstrap.geometrypipeline.vbo.VBOHandle;
import application.bootstrap.geometrypipeline.vbo.VBOInstance;
import engine.graphics.gl.GL20;
import engine.graphics.gl.GL30;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.memory.BufferUtility;

class VBOGLSLUtility extends EngineUtility {

    /*
     * GL upload, in-place update and deletion for VBOManager. Updates respecify
     * the same buffer handle so VAOs referencing it stay valid, and every
     * upload goes through one reusable direct scratch buffer, read straight
     * from the caller's backing array up to its live count so no upload ever
     * copies onto the heap. Vertex data arrives as floats, or as the whole
     * 4-byte words of a packed layout. Package-private.
     */

    // Scratch
    private static FloatBuffer floatScratch;
    private static IntBuffer intScratch;

    private static FloatBuffer fillScratch(float[] vertices, int floatCount) {

        floatScratch = BufferUtility.ensureCapacity(floatScratch, floatCount);
        floatScratch.put(vertices, 0, floatCount).flip();

        return floatScratch;
    }

    private static IntBuffer fillScratch(int[] vertices, int intCount) {

        intScratch = BufferUtility.ensureCapacity(intScratch, intCount);
        intScratch.put(vertices, 0, intCount).flip();

        return intScratch;
    }

    // Upload \\

    static VBOHandle uploadVertexData(
            VAOInstance vaoInstance,
            VBOHandle vboHandle,
            float[] vertices) {

        VBOData vboData = upload(vaoInstance, fillScratch(vertices, vertices.length), vertices.length);
        vboHandle.constructor(vboData);

        return vboHandle;
    }

    // Packed vertex data: whole 4-byte words, as many per vertex as the layout's stride
    static VBOHandle uploadVertexData(
            VAOInstance vaoInstance,
            VBOHandle vboHandle,
            int[] vertices) {

        VBOData vboData = upload(vaoInstance, fillScratch(vertices, vertices.length), vertices.length);
        vboHandle.constructor(vboData);

        return vboHandle;
    }

    static VBOInstance uploadVertexData(
            VAOInstance vaoInstance,
            VBOInstance vboInstance,
            float[] vertices,
            int floatCount) {

        VBOData vboData = upload(vaoInstance, fillScratch(vertices, floatCount), floatCount);
        vboInstance.constructor(vboData);

        return vboInstance;
    }

    private static VBOData upload(VAOInstance vaoInstance, Buffer buffer, int wordCount) {

        GL30 gl30 = EngineContext.gl30;
        GL20 gl20 = EngineContext.gl20;
        VAOData vaoData = vaoInstance.getVAOData();
        int size = wordCount * Float.BYTES;

        gl30.glBindVertexArray(vaoData.getAttributeHandle());

        int vbo = gl20.glGenBuffer();
        gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, vbo);

        gl20.glBufferData(EngineSetting.GL_ARRAY_BUFFER, size, buffer, EngineSetting.GL_STATIC_DRAW);
        VAOAttributeUtility.pointAttributes(vaoData);

        gl30.glBindVertexArray(0);
        gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, 0);

        return new VBOData(vbo, wordCount / vaoData.getVertStride());
    }

    // Update \\

    static VBOInstance updateVertexData(
            VAOInstance vaoInstance,
            VBOInstance vboInstance,
            float[] vertices,
            int floatCount) {

        VBOData oldData = vboInstance.getVBOData();
        int vertStride = vaoInstance.getVAOData().getVertStride();
        VBOData newData = reupload(
                oldData.getVertexHandle(), fillScratch(vertices, floatCount), floatCount, vertStride);
        vboInstance.constructor(newData);

        return vboInstance;
    }

    static VBOHandle updateVertexData(
            VAOInstance vaoInstance,
            VBOHandle vboHandle,
            float[] vertices,
            int floatCount) {

        VBOData oldData = vboHandle.getVBOData();
        int vertStride = vaoInstance.getVAOData().getVertStride();
        VBOData newData = reupload(
                oldData.getVertexHandle(), fillScratch(vertices, floatCount), floatCount, vertStride);
        vboHandle.constructor(newData);

        return vboHandle;
    }

    static VBOHandle updateVertexData(
            VAOInstance vaoInstance,
            VBOHandle vboHandle,
            int[] vertices,
            int intCount) {

        VBOData oldData = vboHandle.getVBOData();
        int vertStride = vaoInstance.getVAOData().getVertStride();
        VBOData newData = reupload(oldData.getVertexHandle(), fillScratch(vertices, intCount), intCount, vertStride);
        vboHandle.constructor(newData);

        return vboHandle;
    }

    private static VBOData reupload(int vbo, Buffer buffer, int wordCount, int vertStride) {

        GL20 gl20 = EngineContext.gl20;
        int size = wordCount * Float.BYTES;

        gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, vbo);
        gl20.glBufferData(EngineSetting.GL_ARRAY_BUFFER, size, buffer, EngineSetting.GL_DYNAMIC_DRAW);
        gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, 0);

        return new VBOData(vbo, wordCount / vertStride);
    }

    // Removal \\

    static void removeVertexData(VBOData vboData) {
        EngineContext.gl20.glDeleteBuffer(vboData.getVertexHandle());
    }
}