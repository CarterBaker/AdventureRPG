package com.internal.bootstrap.shaderpipeline.uniforms.vectors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.vectors.Vector4Double;

import java.nio.ByteBuffer;

public class Vector4DoubleUniform extends UniformAttribute<Vector4Double> {

    // Internal
    private final ByteBuffer buffer;

    public Vector4DoubleUniform() {

        // Internal
        super(new Vector4Double());
        this.buffer = BufferUtils.newByteBuffer(16); // 4 floats * 4 bytes
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Vector4DoubleUniform();
    }

    @Override
    protected void push(int handle, Vector4Double value) {
        Gdx.gl.glUniform4f(handle,
                (float) value.x,
                (float) value.y,
                (float) value.z,
                (float) value.w);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        buffer.clear();

        buffer.putFloat((float) value.x);
        buffer.putFloat((float) value.y);
        buffer.putFloat((float) value.z);
        buffer.putFloat((float) value.w);

        buffer.flip();
        return buffer;
    }

    @Override
    public void set(Vector4Double value) {
        this.value.set(value);
        super.set(value);
    }
}