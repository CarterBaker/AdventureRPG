package com.AdventureRPG.core.shaders.uniforms.scalars;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;

public class BooleanUniform extends UniformAttribute<Boolean> {

    private ByteBuffer buffer;

    public BooleanUniform() {
        super(false);
        this.buffer = BufferUtils.newByteBuffer(4);
    }

    @Override
    protected void push(int handle, Boolean value) {
        Gdx.gl.glUniform1i(handle, value ? 1 : 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        buffer.putInt(value ? 1 : 0);
        buffer.flip();
        return buffer;
    }
}