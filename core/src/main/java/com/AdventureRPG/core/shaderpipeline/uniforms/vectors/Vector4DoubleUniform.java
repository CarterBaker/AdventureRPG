package com.AdventureRPG.core.shaderpipeline.uniforms.vectors;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.mathematics.vectors.Vector4Double;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import java.nio.ByteBuffer;

public class Vector4DoubleUniform extends UniformAttribute<Vector4Double> {

    // Internal
    private final ByteBuffer buffer;

    public Vector4DoubleUniform() {

        // Internal
        super(new Vector4Double());
        this.buffer = BufferUtils.newByteBuffer(16); // 4 floats * 4 bytes
    }

    @Override
    protected void push(int handle, Vector4Double value) {
        Gdx.gl.glUniform4f(handle,
                (float) value.x,
                (float) value.y,
                (float) value.z,
                (float) value.w);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        buffer.clear();

        buffer.putFloat((float) value.x);
        buffer.putFloat((float) value.y);
        buffer.putFloat((float) value.z);
        buffer.putFloat((float) value.w);

        buffer.flip();
        return buffer;
    }

    @Override
    public void set(Vector4Double value) {
        this.value.set(value);
    }
}