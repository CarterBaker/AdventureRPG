package com.AdventureRPG.core.shaderpipeline.uniforms.scalars;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;

public class IntegerUniform extends UniformAttribute<Integer> {

    private ByteBuffer buffer;

    public IntegerUniform() {
        super(0);
        this.buffer = BufferUtils.newByteBuffer(4);
    }

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
}