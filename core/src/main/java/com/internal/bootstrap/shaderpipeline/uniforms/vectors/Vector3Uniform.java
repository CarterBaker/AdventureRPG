package com.internal.bootstrap.shaderpipeline.uniforms.vectors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.vectors.Vector3;

import java.nio.ByteBuffer;

public class Vector3Uniform extends UniformAttribute<Object> {

    // Internal
    private final ByteBuffer buffer;

    public Vector3Uniform() {

        // Internal
        super(new Vector3());
        this.buffer = BufferUtils.newByteBuffer(16);
    }

    @Override
    protected void push(int handle, Object value) {

        // From libGDX vector
        if (value instanceof com.badlogic.gdx.math.Vector3 gdxVector)
            Gdx.gl.glUniform3f(handle, gdxVector.x, gdxVector.y, gdxVector.z);

        // From internal vector
        else if (value instanceof Vector3 internalVector)
            Gdx.gl.glUniform3f(handle, internalVector.x, internalVector.y, internalVector.z);

        else // TODO: Add my own error
            throw new IllegalArgumentException(
                    "push(Int, Vector3): Expected Vector3 or com.badlogic.gdx.math.Vector3, got " + value.getClass());
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
    public void set(Object value) {

        // From libGDX vector
        if (value instanceof com.badlogic.gdx.math.Vector3 gdxVector)
            ((Vector3) this.value).fromGDX(gdxVector);

        // From internal vector
        else if (value instanceof Vector3 internalVector)
            ((Vector3) this.value).set(internalVector);

        else // TODO: Add my own error
            throw new IllegalArgumentException(
                    "set(Vector3): Expected Vector3 or com.badlogic.gdx.math.Vector3, got " + value.getClass());
    }
}