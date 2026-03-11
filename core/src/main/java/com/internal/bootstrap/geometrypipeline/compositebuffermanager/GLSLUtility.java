package com.internal.bootstrap.geometrypipeline.compositebuffermanager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/*
 * GL operations for CompositeBufferManager — creation and disposal only.
 * Package-private — only CompositeBufferManager may call these.
 */
class GLSLUtility {

    // Instance VBO \\

    static int createDynamicInstanceVBO(int maxInstances, int floatsPerInstance) {
        GL20 gl = Gdx.gl20;
        int vbo = gl.glGenBuffer();
        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        gl.glBufferData(GL20.GL_ARRAY_BUFFER, maxInstances * floatsPerInstance * 4, null, GL20.GL_DYNAMIC_DRAW);
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

        GL30 gl30 = Gdx.gl30;
        GL20 gl20 = Gdx.gl20;

        IntBuffer idBuf = ByteBuffer.allocateDirect(4)
                .order(ByteOrder.nativeOrder()).asIntBuffer();
        gl30.glGenVertexArrays(1, idBuf);
        int vao = idBuf.get(0);
        gl30.glBindVertexArray(vao);

        gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, meshVBOHandle);
        int meshStride = 0;
        for (int s : meshAttrSizes)
            meshStride += s;
        int meshStrideBytes = meshStride * 4;
        int byteOffset = 0;
        for (int i = 0; i < meshAttrSizes.length; i++) {
            gl20.glEnableVertexAttribArray(i);
            gl20.glVertexAttribPointer(i, meshAttrSizes[i], GL20.GL_FLOAT, false, meshStrideBytes, byteOffset);
            byteOffset += meshAttrSizes[i] * 4;
        }

        int instanceStride = 0;
        for (int s : instanceAttrSizes)
            instanceStride += s;
        int instanceStrideBytes = instanceStride * 4;

        gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, instanceVBOHandle);
        int instanceByteOffset = 0;
        for (int i = 0; i < instanceAttrSizes.length; i++) {
            int loc = meshAttrSizes.length + i;
            gl20.glEnableVertexAttribArray(loc);
            gl20.glVertexAttribPointer(loc, instanceAttrSizes[i], GL20.GL_FLOAT, false,
                    instanceStrideBytes, instanceByteOffset);
            gl30.glVertexAttribDivisor(loc, 1);
            instanceByteOffset += instanceAttrSizes[i] * 4;
        }

        gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, meshIBOHandle);
        gl30.glBindVertexArray(0);
        gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);

        return vao;
    }

    // Cleanup \\

    static void deleteBuffer(int handle) {
        Gdx.gl20.glDeleteBuffer(handle);
    }

    static void deleteVAO(int handle) {
        IntBuffer buf = ByteBuffer.allocateDirect(4)
                .order(ByteOrder.nativeOrder()).asIntBuffer();
        buf.put(handle).flip();
        Gdx.gl30.glDeleteVertexArrays(1, buf);
    }
}