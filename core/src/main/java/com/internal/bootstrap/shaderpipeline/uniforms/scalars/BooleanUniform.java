package com.internal.bootstrap.shaderpipeline.uniforms.scalars;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;

import java.nio.ByteBuffer;

public class BooleanUniform extends UniformAttribute<Boolean> {

    // Internal
    private final ByteBuffer buffer;

    public BooleanUniform() {

        // Internal
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

    @Override
    public void set(Boolean value) {
        this.value = value;
        super.set(value);
    }
}
