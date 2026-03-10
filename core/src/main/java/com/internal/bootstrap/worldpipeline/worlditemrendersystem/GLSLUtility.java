package com.internal.bootstrap.worldpipeline.worlditemrendersystem;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

class GLSLUtility {

    // Instance VBO \\

    static int createDynamicInstanceVBO(int maxInstances) {
        GL20 gl = Gdx.gl20;
        int vbo = gl.glGenBuffer();
        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        // 6 floats per instance * 4 bytes per float = 24 bytes per instance
        gl.glBufferData(GL20.GL_ARRAY_BUFFER, maxInstances * 6 * 4, null, GL20.GL_DYNAMIC_DRAW);
        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
        return vbo;
    }

    static void updateInstanceVBO(int vbo, FloatBuffer data, int floatCount) {
        GL20 gl = Gdx.gl20;
        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        gl.glBufferSubData(GL20.GL_ARRAY_BUFFER, 0, floatCount * 4, data);
        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
    }

    // Composite VAO \\

    /*
     * Creates a VAO that wires:
     * - mesh VBO attributes 0..N-1 at their original layout locations
     * - instance VBO attribute N as vec4 [chunkX, chunkZ, localX, localZ] divisor=1
     * - instance VBO attribute N+1 as vec2 [localY, orientation] divisor=1
     * - IBO bound into VAO state
     * Stride = 24 bytes (6 floats), vec4 at offset 0, vec2 at offset 16.
     */
    static int createInstancedVAO(
            int meshVBOHandle,
            int[] meshAttrSizes,
            int meshIBOHandle,
            int instanceVBOHandle) {

        GL30 gl30 = Gdx.gl30;
        GL20 gl20 = Gdx.gl20;

        IntBuffer idBuf = ByteBuffer.allocateDirect(4)
                .order(ByteOrder.nativeOrder()).asIntBuffer();
        gl30.glGenVertexArrays(1, idBuf);
        int vao = idBuf.get(0);
        gl30.glBindVertexArray(vao);

        // Mesh vertex attributes
        gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, meshVBOHandle);
        int vertStride = 0;
        for (int size : meshAttrSizes)
            vertStride += size;
        int strideBytes = vertStride * 4;
        int byteOffset = 0;
        for (int i = 0; i < meshAttrSizes.length; i++) {
            gl20.glEnableVertexAttribArray(i);
            gl20.glVertexAttribPointer(
                    i, meshAttrSizes[i], GL20.GL_FLOAT, false, strideBytes, byteOffset);
            byteOffset += meshAttrSizes[i] * 4;
        }

        // Instance attribute 0 — vec4(chunkX, chunkZ, localX, localZ) at location N
        int instanceLoc0 = meshAttrSizes.length;
        gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, instanceVBOHandle);
        gl20.glEnableVertexAttribArray(instanceLoc0);
        gl20.glVertexAttribPointer(instanceLoc0, 4, GL20.GL_FLOAT, false, 24, 0);
        gl30.glVertexAttribDivisor(instanceLoc0, 1);

        // Instance attribute 1 — vec2(localY, orientation) at location N+1
        int instanceLoc1 = instanceLoc0 + 1;
        gl20.glEnableVertexAttribArray(instanceLoc1);
        gl20.glVertexAttribPointer(instanceLoc1, 2, GL20.GL_FLOAT, false, 24, 16);
        gl30.glVertexAttribDivisor(instanceLoc1, 1);

        // IBO wired into VAO state
        gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, meshIBOHandle);

        gl30.glBindVertexArray(0);
        gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);

        return vao;
    }

    // Draw \\

    static void drawElementsInstanced(int vao, int indexCount, int instanceCount) {
        Gdx.gl30.glBindVertexArray(vao);
        Gdx.gl30.glDrawElementsInstanced(
                GL20.GL_TRIANGLES, indexCount, GL20.GL_UNSIGNED_SHORT, 0, instanceCount);
        Gdx.gl30.glBindVertexArray(0);
    }

    // Shader \\

    static void useShader(int shaderHandle) {
        Gdx.gl.glUseProgram(shaderHandle);
    }

    // UBO \\

    static void bindUniformBlock(int shaderProgram, String blockName, int bindingPoint) {
        int blockIndex = Gdx.gl30.glGetUniformBlockIndex(shaderProgram, blockName);
        if (blockIndex != GL30.GL_INVALID_INDEX)
            Gdx.gl30.glUniformBlockBinding(shaderProgram, blockIndex, bindingPoint);
    }

    static void bindUniformBuffer(int bindingPoint, int gpuHandle) {
        Gdx.gl30.glBindBufferBase(GL30.GL_UNIFORM_BUFFER, bindingPoint, gpuHandle);
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