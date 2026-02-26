package com.internal.bootstrap.shaderpipeline.uniforms.samplers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;

import java.nio.ByteBuffer;

public final class SampleImage2DArrayUniform extends UniformAttribute<Integer> {

    // Internal
    private final ByteBuffer buffer;
    private int gpuHandle = 0; // permanent — set once from material JSON
    private int textureUnit = 0; // transient — assigned each frame by RenderSystem

    public SampleImage2DArrayUniform() {
        super(0);
        this.buffer = BufferUtils.newByteBuffer(4); // 1 int * 4 bytes
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new SampleImage2DArrayUniform();
    }

    // Sampler \\
    @Override
    public boolean isSampler() {
        return true;
    }

    @Override
    public void bindTexture(int unit) {
        this.textureUnit = unit;
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0 + unit);
        Gdx.gl30.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, gpuHandle);
    }

    // Push \\
    @Override
    protected void push(int handle, Integer value) {
        Gdx.gl.glUniform1i(handle, textureUnit);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        buffer.putInt(gpuHandle);
        buffer.flip();
        return buffer;
    }

    @Override
    protected void applyValue(Integer value) {
        this.gpuHandle = value;
        this.value = value;
    }
}