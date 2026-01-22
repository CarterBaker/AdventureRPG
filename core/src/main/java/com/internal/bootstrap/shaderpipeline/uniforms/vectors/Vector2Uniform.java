package com.internal.bootstrap.shaderpipeline.uniforms.vectors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.vectors.Vector2;

import java.nio.ByteBuffer;

public class Vector2Uniform extends UniformAttribute<Object> {

    // Internal
    private final ByteBuffer buffer;

    public Vector2Uniform() {

        // Internal
        super(new Vector2());
        this.buffer = BufferUtils.newByteBuffer(8); // 2 floats * 4 bytes
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Vector2Uniform();
    }

    @Override
    protected void push(int handle, Object value) {

        // From libGDX vector
        if (value instanceof com.badlogic.gdx.math.Vector2 gdxVector)
            Gdx.gl.glUniform2f(handle, gdxVector.x, gdxVector.y);

        // From internal vector
        else if (value instanceof Vector2 internalVector)
            Gdx.gl.glUniform2f(handle, internalVector.x, internalVector.y);

        else // TODO: Add my own error
            throw new IllegalArgumentException(
                    "push(Int, Vector2): Expected Vector2 or com.badlogic.gdx.math.Vector2, got " + value.getClass());
    }

    @Override
    public ByteBuffer getByteBuffer() {

        buffer.clear();

        buffer.putFloat(((Vector2) value).x);
        buffer.putFloat(((Vector2) value).y);

        buffer.flip();
        return buffer;
    }

    @Override
    public void set(Object value) {

        // From libGDX vector
        if (value instanceof com.badlogic.gdx.math.Vector2 gdxVector)
            ((Vector2) this.value).fromGDX(gdxVector);

        // From internal vector
        else if (value instanceof Vector2 internalVector)
            ((Vector2) this.value).set((Vector2) internalVector);

        else // TODO: Add my own error
            throw new IllegalArgumentException(
                    "set(Vector2): Expected Vector2 or com.badlogic.gdx.math.Vector2, got " + value.getClass());

        super.set(value);
    }
}