package com.AdventureRPG.core.geometrypipeline.util;

import com.AdventureRPG.core.geometrypipeline.vaomanager.VAOHandle;
import com.AdventureRPG.core.geometrypipeline.vbomanager.VBOHandle;
import com.AdventureRPG.core.geometrypipeline.ibomanager.IBOHandle;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class GLSLUtility {

   public  static VAOHandle createVAO(int floatsPerVert) {

        GL30 gl = Gdx.gl30;

        IntBuffer id = ByteBuffer
                .allocateDirect(4)
                .order(ByteOrder.nativeOrder())
                .asIntBuffer();

        gl.glGenVertexArrays(1, id);
        int vao = id.get(0);

        return new VAOHandle(vao, floatsPerVert);
    }

   public  static VBOHandle uploadVertexData(VAOHandle vaoHandle, float[] vertices) {

        GL20 gl = Gdx.gl20;

        int vbo = gl.glGenBuffer();
        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);

        FloatBuffer vertexBuffer = ByteBuffer
                .allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(vertices).flip();

        gl.glBufferData(GL20.GL_ARRAY_BUFFER, vertices.length * 4, vertexBuffer, GL20.GL_STATIC_DRAW);
        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);

        int vertexCount = vertices.length / vaoHandle.vertStride;
        return new VBOHandle(vbo, vertexCount);
    }

   public  static IBOHandle uploadIndexData(short[] indices) {

        GL20 gl = Gdx.gl20;

        int ibo = gl.glGenBuffer();
        gl.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, ibo);

        ShortBuffer indexBuffer = ByteBuffer
                .allocateDirect(indices.length * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        indexBuffer.put(indices).flip();

        gl.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, indices.length * 2, indexBuffer, GL20.GL_STATIC_DRAW);
        gl.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);

        int indexCount = indices.length;
        return new IBOHandle(ibo, indexCount);
    }

   public  static void freeVAO(VAOHandle handle) {

        GL30 gl = Gdx.gl30;

        IntBuffer id = ByteBuffer
                .allocateDirect(4)
                .order(ByteOrder.nativeOrder())
                .asIntBuffer();
        id.put(0, handle.attributeHandle);

        gl.glDeleteVertexArrays(1, id);
    }

   public  static void freeVBO(VBOHandle handle) {
        Gdx.gl20.glDeleteBuffer(handle.vertexHandle);
    }

   public  static void freeIBO(IBOHandle handle) {
        Gdx.gl20.glDeleteBuffer(handle.indexHandle);
    }
}
