package com.AdventureRPG.core.shaders.uniforms.vectors;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Mathematics.Vectors.Vector3Double;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;

public class Vector3DoubleUniform extends UniformAttribute<Vector3Double> {

    private ByteBuffer buffer;

    public Vector3DoubleUniform() {
        super(new Vector3Double());
        this.buffer = BufferUtils.newByteBuffer(12); // 3 floats * 4 bytes
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
        buffer.flip();
        return buffer;
    }
}