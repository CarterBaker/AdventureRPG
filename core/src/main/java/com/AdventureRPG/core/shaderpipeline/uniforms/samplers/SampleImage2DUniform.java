package com.AdventureRPG.core.shaderpipeline.uniforms.samplers;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;

public class SampleImage2DUniform extends UniformAttribute<Integer> {

    private ByteBuffer buffer;

    public SampleImage2DUniform() {
        super(0);
        this.buffer = BufferUtils.newByteBuffer(4); // 1 int * 4 bytes
    }

    @Override
    protected void push(int handle, Integer value) {
        // Upload texture unit index (not texture handle)
        Gdx.gl.glUniform1i(handle, value);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        buffer.putInt(value);
        buffer.flip();
        return buffer;
    }
}