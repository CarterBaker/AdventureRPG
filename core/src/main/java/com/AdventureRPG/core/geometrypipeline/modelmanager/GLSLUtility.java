package com.AdventureRPG.core.geometrypipeline.modelmanager;

import com.AdventureRPG.core.geometrypipeline.vaomanager.VAOHandle;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

class GLSLUtility {

    static MeshHandle uploadMeshData(VAOHandle vaoHandle, float[] vertices, short[] indices) {

        GL20 gl = Gdx.gl20;

        // Create and upload vertex buffer
        int vbo = gl.glGenBuffer();
        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);

        FloatBuffer vertexBuffer = ByteBuffer
                .allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(vertices).flip();

        gl.glBufferData(GL20.GL_ARRAY_BUFFER, vertices.length * 4, vertexBuffer, GL20.GL_STATIC_DRAW);

        // Create and upload index buffer
        int ibo = gl.glGenBuffer();
        gl.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, ibo);

        ShortBuffer indexBuffer = ByteBuffer
                .allocateDirect(indices.length * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        indexBuffer.put(indices).flip();

        gl.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, indices.length * 2, indexBuffer, GL20.GL_STATIC_DRAW);

        // Unbind buffers
        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);

        return new MeshHandle(vaoHandle.gpuHandle, vbo, ibo, indices.length);
    }

    static void freeMeshData(MeshHandle handle) {

        GL20 gl = Gdx.gl20;

        gl.glDeleteBuffer(handle.vbo);
        gl.glDeleteBuffer(handle.ibo);
    }
}