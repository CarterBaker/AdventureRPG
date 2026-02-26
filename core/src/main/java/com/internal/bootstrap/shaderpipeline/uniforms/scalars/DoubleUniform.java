package com.internal.bootstrap.shaderpipeline.uniforms.scalars;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;

import java.nio.ByteBuffer;

public final class DoubleUniform extends UniformAttribute<Double> {

    // Internal
    private final ByteBuffer buffer;

    public DoubleUniform() {
        // Internal
        super(0.0);
        this.buffer = BufferUtils.newByteBuffer(4); // 1 float * 4 bytes (double downcast to float for GLSL ES)
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new DoubleUniform();
    }

    @Override
    protected void push(int handle, Double value) {
        Gdx.gl.glUniform1f(handle, value.floatValue());
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        buffer.putFloat(value.floatValue());
        buffer.flip();
        return buffer;
    }

    @Override
    protected void applyValue(Double value) {
        this.value = value;
    }
}