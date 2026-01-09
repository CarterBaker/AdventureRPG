package com.AdventureRPG.bootstrap.shaderpipeline.ubomanager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

final class GLSLUtility {

    private GLSLUtility() {
    }

    // UBO Creation \\
    static int createUniformBuffer() {
        IntBuffer buffer = BufferUtils.newIntBuffer(1);
        Gdx.gl30.glGenBuffers(1, buffer);
        return buffer.get(0);
    }

    static void allocateUniformBuffer(int buffer, int sizeBytes) {
        Gdx.gl30.glBindBuffer(GL30.GL_UNIFORM_BUFFER, buffer);
        Gdx.gl30.glBufferData(GL30.GL_UNIFORM_BUFFER, sizeBytes, null, GL20.GL_DYNAMIC_DRAW);
        Gdx.gl30.glBindBuffer(GL30.GL_UNIFORM_BUFFER, 0);
    }

    static void bindUniformBufferBase(int buffer, int bindingPoint) {
        Gdx.gl30.glBindBufferBase(GL30.GL_UNIFORM_BUFFER, bindingPoint, buffer);
    }

    // UBO Updates \\
    static void updateUniformBuffer(int buffer, int offset, ByteBuffer data) {
        Gdx.gl30.glBindBuffer(GL30.GL_UNIFORM_BUFFER, buffer);
        Gdx.gl30.glBufferSubData(GL30.GL_UNIFORM_BUFFER, offset, data.remaining(), data);
        Gdx.gl30.glBindBuffer(GL30.GL_UNIFORM_BUFFER, 0);
    }

    // UBO Deletion \\
    static void deleteUniformBuffer(int buffer) {
        IntBuffer buf = BufferUtils.newIntBuffer(1);
        buf.put(buffer).flip();
        Gdx.gl30.glDeleteBuffers(1, buf);
    }
}
