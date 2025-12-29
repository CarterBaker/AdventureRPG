package com.AdventureRPG.core.shaderpipeline.uniforms.vectors;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Mathematics.Vectors.Vector4;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import java.nio.ByteBuffer;

public class Vector4Uniform extends UniformAttribute<Object> {

    // Internal
    private final ByteBuffer buffer;

    public Vector4Uniform() {

        // Internal
        super(new Vector4());
        this.buffer = BufferUtils.newByteBuffer(16); // 4 floats * 4 bytes
    }

    @Override
    protected void push(int handle, Object value) {

        // From libGDX vector
        if (value instanceof com.badlogic.gdx.math.Vector4 gdxVector)
            Gdx.gl.glUniform4f(handle, gdxVector.x, gdxVector.y, gdxVector.z, gdxVector.w);

        // From internal vector
        else if (value instanceof Vector4 internalVector)
            Gdx.gl.glUniform4f(handle, internalVector.x, internalVector.y, internalVector.z, internalVector.w);

        else // TODO: Add my own error
            throw new IllegalArgumentException(
                    "push(Int, Vector4): Expected Vector4 or com.badlogic.gdx.math.Vector4, got " + value.getClass());
    }

    @Override
    public ByteBuffer getByteBuffer() {

        buffer.clear();

        buffer.putFloat(((Vector4) value).x);
        buffer.putFloat(((Vector4) value).y);
        buffer.putFloat(((Vector4) value).z);
        buffer.putFloat(((Vector4) value).w);

        buffer.flip();
        return buffer;
    }

    @Override
    public void set(Object value) {

        // From libGDX vector
        if (value instanceof com.badlogic.gdx.math.Vector4 gdxVector)
            ((Vector4) this.value).fromGDX(gdxVector);

        // From internal vector
        else if (value instanceof Vector4 internalVector)
            ((Vector4) this.value).set(internalVector);

        else // TODO: Add my own error
            throw new IllegalArgumentException(
                    "set(Vector3): Expected Vector4 or com.badlogic.gdx.math.Vector4, got " + value.getClass());
    }
}