package application.bootstrap.geometrypipeline.compositebuffermanager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import application.core.engine.EngineContext;
import application.core.util.graphics.gl.GL20;
import application.core.util.graphics.gl.GL30;

class GLSLUtility {

    /*
     * GL creation and disposal operations for CompositeBufferManager.
     * Package-private — only CompositeBufferManager may call these.
     */

    // Instance VBO \\

    static int createDynamicInstanceVBO(int maxInstances, int floatsPerInstance) {

        GL20 gl = EngineContext.gl20;
        int size = maxInstances * floatsPerInstance * Float.BYTES;

        int vbo = gl.glGenBuffer();
        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        gl.glBufferData(GL20.GL_ARRAY_BUFFER, size, null, GL20.GL_DYNAMIC_DRAW);
        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);

        return vbo;
    }

    // Composite VAO \\

    static int createInstancedVAO(
            int meshVBOHandle,
            int[] meshAttrSizes,
            int meshIBOHandle,
            int instanceVBOHandle,
            int[] instanceAttrSizes) {

        GL30 gl30 = EngineContext.gl30;
        GL20 gl20 = EngineContext.gl20;

        IntBuffer idBuf = ByteBuffer.allocateDirect(Integer.BYTES)
                .order(ByteOrder.nativeOrder()).asIntBuffer();
        gl30.glGenVertexArrays(1, idBuf);
        int vao = idBuf.get(0);

        gl30.glBindVertexArray(vao);

        gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, meshVBOHandle);

        int meshStride = 0;
        for (int s : meshAttrSizes)
            meshStride += s;
        int meshStrideBytes = meshStride * Float.BYTES;
        int byteOffset = 0;

        for (int i = 0; i < meshAttrSizes.length; i++) {
            gl20.glEnableVertexAttribArray(i);
            gl20.glVertexAttribPointer(i, meshAttrSizes[i], GL20.GL_FLOAT, false, meshStrideBytes, byteOffset);
            byteOffset += meshAttrSizes[i] * Float.BYTES;
        }

        int instanceStride = 0;
        for (int s : instanceAttrSizes)
            instanceStride += s;
        int instanceStrideBytes = instanceStride * Float.BYTES;

        gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, instanceVBOHandle);

        int instanceByteOffset = 0;

        for (int i = 0; i < instanceAttrSizes.length; i++) {
            int loc = meshAttrSizes.length + i;
            gl20.glEnableVertexAttribArray(loc);
            gl20.glVertexAttribPointer(loc, instanceAttrSizes[i], GL20.GL_FLOAT, false,
                    instanceStrideBytes, instanceByteOffset);
            gl30.glVertexAttribDivisor(loc, 1);
            instanceByteOffset += instanceAttrSizes[i] * Float.BYTES;
        }

        gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, meshIBOHandle);

        gl30.glBindVertexArray(0);
        gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);

        return vao;
    }

    // Cleanup \\

    static void deleteBuffer(int handle) {
        EngineContext.gl20.glDeleteBuffer(handle);
    }

    static void deleteVAO(int handle) {

        IntBuffer buf = ByteBuffer.allocateDirect(Integer.BYTES)
                .order(ByteOrder.nativeOrder()).asIntBuffer();
        buf.put(handle).flip();

        EngineContext.gl30.glDeleteVertexArrays(1, buf);
    }
}
