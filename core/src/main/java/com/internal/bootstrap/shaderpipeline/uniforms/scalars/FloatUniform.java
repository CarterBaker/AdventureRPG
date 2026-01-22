package com.internal.bootstrap.shaderpipeline.uniforms.scalars;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;

import java.nio.ByteBuffer;

public class FloatUniform extends UniformAttribute<Float> {

    // Internal
    private final ByteBuffer buffer;

    public FloatUniform() {

        // Internal
        super(0f);
        this.buffer = BufferUtils.newByteBuffer(4);
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new FloatUniform();
    }

    @Override
    protected void push(int handle, Float value) {
        Gdx.gl.glUniform1f(handle, value);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        buffer.clear();

        buffer.putFloat(value);

        buffer.flip();
        return buffer;
    }

    @Override
    public void set(Float value) {
        this.value = value;
        super.set(value);
    }
}
