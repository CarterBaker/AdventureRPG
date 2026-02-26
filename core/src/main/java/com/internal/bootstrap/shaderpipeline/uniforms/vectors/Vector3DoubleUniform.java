package com.internal.bootstrap.shaderpipeline.uniforms.vectors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.vectors.Vector3Double;

import java.nio.ByteBuffer;

public final class Vector3DoubleUniform extends UniformAttribute<Vector3Double> {

    // Internal
    private final ByteBuffer buffer;

    public Vector3DoubleUniform() {
        // Internal
        super(new Vector3Double());
        this.buffer = BufferUtils.newByteBuffer(16); // 3 floats * 4 bytes + 4 bytes padding (std140, doubles downcast
                                                     // to float for GLSL ES)
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Vector3DoubleUniform();
    }

    @Override
    protected void push(int handle, Vector3Double value) {
        Gdx.gl.glUniform3f(handle, (float) value.x, (float) value.y, (float) value.z);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        buffer.putFloat((float) value.x);
        buffer.putFloat((float) value.y);
        buffer.putFloat((float) value.z);
        buffer.putFloat(0f); // padding
        buffer.flip();
        return buffer;
    }

    @Override
    protected void applyValue(Vector3Double value) {
        this.value.set(value);
    }
}