package com.internal.bootstrap.shaderpipeline.uniforms.samplers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;

import java.nio.ByteBuffer;

public class SampleImage2DUniform extends UniformAttribute<Integer> {

    // Internal
    private final ByteBuffer buffer;

    public SampleImage2DUniform() {
        super(0);
        this.buffer = BufferUtils.newByteBuffer(4);
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new SampleImage2DUniform();
    }

    // Sampler \\

    @Override
    public boolean isSampler() {
        return true;
    }

    @Override
    public void bindTexture(int unit) {
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0 + unit);
        Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, value);
        this.value = unit;
    }

    // Push \\

    @Override
    protected void push(int handle, Integer value) {
        Gdx.gl.glUniform1i(handle, value);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        buffer.putInt(value);
        buffer.flip();
        return buffer;
    }

    @Override
    public void set(Integer value) {
        this.value = value;
        super.set(value);
    }
}