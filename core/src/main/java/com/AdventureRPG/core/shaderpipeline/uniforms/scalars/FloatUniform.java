package com.AdventureRPG.core.shaderpipeline.uniforms.scalars;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;

public class FloatUniform extends UniformAttribute<Float> {

    private ByteBuffer buffer;

    public FloatUniform() {
        super(0f);
        this.buffer = BufferUtils.newByteBuffer(4);
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
}