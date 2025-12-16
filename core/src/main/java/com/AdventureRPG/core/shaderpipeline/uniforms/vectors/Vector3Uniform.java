package com.AdventureRPG.core.shaderpipeline.uniforms.vectors;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector3;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;

public class Vector3Uniform extends UniformAttribute<Vector3> {

    private ByteBuffer buffer;

    public Vector3Uniform() {
        super(new Vector3());
        this.buffer = BufferUtils.newByteBuffer(12); // 3 floats * 4 bytes
    }

    @Override
    protected void push(int handle, Vector3 value) {
        Gdx.gl.glUniform3f(handle, value.x, value.y, value.z);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        buffer.putFloat(value.x);
        buffer.putFloat(value.y);
        buffer.putFloat(value.z);
        buffer.flip();
        return buffer;
    }
}