package com.AdventureRPG.core.shaders.uniforms.scalars;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;

public class DoubleUniform extends UniformAttribute<Double> {

    // Internal
    private final ByteBuffer buffer;

    public DoubleUniform() {

        // Internal
        super(0.0);
        this.buffer = BufferUtils.newByteBuffer(4); // store as float for GPU
    }

    @Override
    protected void push(int handle, Double value) {
        Gdx.gl.glUniform1f(handle, value.floatValue());
    }

    @Override
    public ByteBuffer getByteBuffer() {

        buffer.clear();

        buffer.putFloat(value.floatValue()); // convert double → float

        buffer.flip();
        return buffer;
    }

    @Override
    public void set(Double value) {
        this.value = value;
    }
}
