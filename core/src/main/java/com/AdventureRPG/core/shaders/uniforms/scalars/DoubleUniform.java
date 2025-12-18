package com.AdventureRPG.core.shaders.uniforms.scalars;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;

public class DoubleUniform extends UniformAttribute<Double> {

    private ByteBuffer buffer;

    public DoubleUniform() {
        super(0.0);
        this.buffer = BufferUtils.newByteBuffer(4); // 4 bytes (float), not 8
    }

    @Override
    protected void push(int handle, Double value) {
        Gdx.gl.glUniform1f(handle, value.floatValue());
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        buffer.putFloat(value.floatValue()); // Convert to float for UBO
        buffer.flip();
        return buffer;
    }
}