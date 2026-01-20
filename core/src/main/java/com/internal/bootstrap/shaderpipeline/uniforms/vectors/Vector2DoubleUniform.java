package com.internal.bootstrap.shaderpipeline.uniforms.vectors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.vectors.Vector2Double;

import java.nio.ByteBuffer;

public class Vector2DoubleUniform extends UniformAttribute<Vector2Double> {

    // Internal
    private final ByteBuffer buffer;

    public Vector2DoubleUniform() {

        // Internal
        super(new Vector2Double());
        this.buffer = BufferUtils.newByteBuffer(8); // 2 floats * 4 bytes
    }

    @Override
    protected void push(int handle, Vector2Double value) {
        Gdx.gl.glUniform2f(handle, (float) value.x, (float) value.y);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        buffer.clear();

        buffer.putFloat((float) value.x);
        buffer.putFloat((float) value.y);

        buffer.flip();
        return buffer;
    }

    @Override
    public void set(Vector2Double value) {
        this.value.set(value);
        super.set(value);
    }
}