package com.internal.bootstrap.shaderpipeline.uniforms.vectors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.vectors.Vector3;

import java.nio.ByteBuffer;

public final class Vector3Uniform extends UniformAttribute<Object> {

    // Internal
    private final ByteBuffer buffer;

    public Vector3Uniform() {
        // Internal
        super(new Vector3());
        this.buffer = BufferUtils.newByteBuffer(16); // 3 floats * 4 bytes + 4 bytes padding (std140)
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Vector3Uniform();
    }

    @Override
    protected void push(int handle, Object value) {
        // From libGDX vector
        if (value instanceof com.badlogic.gdx.math.Vector3 gdxVector)
            Gdx.gl.glUniform3f(handle, gdxVector.x, gdxVector.y, gdxVector.z);
        // From internal vector
        else if (value instanceof Vector3 internalVector)
            Gdx.gl.glUniform3f(handle, internalVector.x, internalVector.y, internalVector.z);
        else
            throw new IllegalArgumentException(
                    "push(int, Vector3): Expected Vector3 or com.badlogic.gdx.math.Vector3, got " + value.getClass());
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        buffer.putFloat(((Vector3) value).x);
        buffer.putFloat(((Vector3) value).y);
        buffer.putFloat(((Vector3) value).z);
        buffer.putFloat(0f); // padding
        buffer.flip();
        return buffer;
    }

    @Override
    protected void applyValue(Object value) {
        // From libGDX vector
        if (value instanceof com.badlogic.gdx.math.Vector3 gdxVector)
            ((Vector3) this.value).fromGDX(gdxVector);
        // From internal vector
        else if (value instanceof Vector3 internalVector)
            ((Vector3) this.value).set(internalVector);
        else
            throw new IllegalArgumentException(
                    "applyValue(Vector3): Expected Vector3 or com.badlogic.gdx.math.Vector3, got " + value.getClass());
    }
}