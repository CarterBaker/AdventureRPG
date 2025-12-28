package com.AdventureRPG.core.shaders.uniforms.scalars;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;

public class IntegerUniform extends UniformAttribute<Integer> {

    // Internal
    private final ByteBuffer buffer;

    public IntegerUniform() {

        // Internal
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

    @Override
    public void set(Integer value) {
        this.value = value;
    }
}
