package com.internal.bootstrap.shaderpipeline.uniforms.vectors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.vectors.Vector3Boolean;

import java.nio.ByteBuffer;

public class Vector3BooleanUniform extends UniformAttribute<Vector3Boolean> {

    // Internal
    private final ByteBuffer buffer;

    public Vector3BooleanUniform() {

        // Internal
        super(new Vector3Boolean());
        this.buffer = BufferUtils.newByteBuffer(16);
    }

    @Override
    protected void push(int handle, Vector3Boolean value) {
        Gdx.gl.glUniform3i(handle, value.x ? 1 : 0, value.y ? 1 : 0, value.z ? 1 : 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        buffer.clear();

        buffer.putInt(value.x ? 1 : 0);
        buffer.putInt(value.y ? 1 : 0);
        buffer.putInt(value.z ? 1 : 0);
        buffer.putInt(0); // padding

        buffer.flip();
        return buffer;
    }

    @Override
    public void set(Vector3Boolean value) {
        this.value.set(value);
        super.set(value);
    }
}