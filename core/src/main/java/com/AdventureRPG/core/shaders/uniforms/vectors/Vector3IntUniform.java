package com.AdventureRPG.core.shaders.uniforms.vectors;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector3Int;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;

public class Vector3IntUniform extends UniformAttribute<Vector3Int> {

    private ByteBuffer buffer;

    public Vector3IntUniform() {
        super(new Vector3Int());
        this.buffer = BufferUtils.newByteBuffer(12); // 3 ints * 4 bytes
    }

    @Override
    protected void push(int handle, Vector3Int value) {
        Gdx.gl.glUniform3i(handle, value.x, value.y, value.z);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        buffer.putInt(value.x);
        buffer.putInt(value.y);
        buffer.putInt(value.z);
        buffer.flip();
        return buffer;
    }
}