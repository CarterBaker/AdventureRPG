package com.AdventureRPG.core.shaderpipeline.uniforms.vectors;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.mathematics.vectors.Vector3Double;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import java.nio.ByteBuffer;

public class Vector3DoubleUniform extends UniformAttribute<Vector3Double> {

    // Internal
    private final ByteBuffer buffer;

    public Vector3DoubleUniform() {

        // Internal
        super(new Vector3Double());
        this.buffer = BufferUtils.newByteBuffer(16);
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
    public void set(Vector3Double value) {
        this.value.set(value);
    }
}