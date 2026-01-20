package com.internal.bootstrap.shaderpipeline.uniforms.vectors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.vectors.Vector3Int;

import java.nio.ByteBuffer;

public class Vector3IntUniform extends UniformAttribute<Vector3Int> {

    // Internal
    private final ByteBuffer buffer;

    public Vector3IntUniform() {

        // Internal
        super(new Vector3Int());
        this.buffer = BufferUtils.newByteBuffer(16);
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
        buffer.putInt(0); // padding

        buffer.flip();
        return buffer;
    }

    @Override
    public void set(Vector3Int value) {
        this.value.set(value);
        super.set(value);
    }
}